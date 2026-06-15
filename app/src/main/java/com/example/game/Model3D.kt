package com.example.game

import android.graphics.Path
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import kotlin.math.cos
import kotlin.math.sin

data class Vector3D(var x: Float, var y: Float, var z: Float) {
    fun rotateX(rad: Float): Vector3D {
        val cosA = cos(rad)
        val sinA = sin(rad)
        return Vector3D(x, y * cosA - z * sinA, y * sinA + z * cosA)
    }

    fun rotateY(rad: Float): Vector3D {
        val cosA = cos(rad)
        val sinA = sin(rad)
        return Vector3D(x * cosA + z * sinA, y, -x * sinA + z * cosA)
    }

    fun rotateZ(rad: Float): Vector3D {
        val cosA = cos(rad)
        val sinA = sin(rad)
        return Vector3D(x * cosA - y * sinA, x * sinA + y * cosA, z)
    }

    fun plus(v: Vector3D) = Vector3D(x + v.x, y + v.y, z + v.z)
    fun minus(v: Vector3D) = Vector3D(x - v.x, y - v.y, z - v.z)
}

data class Face3D(
    val vertexIndices: List<Int>,
    val color: Color,
    val lineOnly: Boolean = false,
    val outlineColor: Color? = null
)

class Model3D(
    val vertices: List<Vector3D>,
    val faces: List<Face3D>
) {
    fun transform(
        scale: Vector3D,
        rotationRad: Vector3D,
        translation: Vector3D
    ): Model3D {
        val transformedVertices = vertices.map { v ->
            var temp = Vector3D(v.x * scale.x, v.y * scale.y, v.z * scale.z)
            if (rotationRad.x != 0f) temp = temp.rotateX(rotationRad.x)
            if (rotationRad.y != 0f) temp = temp.rotateY(rotationRad.y)
            if (rotationRad.z != 0f) temp = temp.rotateZ(rotationRad.z)
            temp.plus(translation)
        }
        return Model3D(transformedVertices, faces)
    }

    fun render(
        drawScope: DrawScope,
        cx: Float,
        cy: Float,
        fov: Float,
        cameraPos: Vector3D = Vector3D(0f, 0f, 0f),
        cameraRot: Vector3D = Vector3D(0f, 0f, 0f),
        screenShakeX: Float = 0f,
        screenShakeY: Float = 0f
    ) {
        val paint = android.graphics.Paint().apply {
            isAntiAlias = true
            style = android.graphics.Paint.Style.FILL
        }
        val edgePaint = android.graphics.Paint().apply {
            isAntiAlias = true
            style = android.graphics.Paint.Style.STROKE
            strokeWidth = 2f
        }

        // Camera transformations
        val cameraTranslatedVertices = vertices.map { v ->
            // Apply translation relative to camera
            var temp = v.minus(cameraPos)
            // Apply reverse camera rotations
            if (cameraRot.y != 0f) temp = temp.rotateY(-cameraRot.y)
            if (cameraRot.x != 0f) temp = temp.rotateX(-cameraRot.x)
            if (cameraRot.z != 0f) temp = temp.rotateZ(-cameraRot.z)
            temp
        }

        // Project and sort faces back-to-front
        val faceDepths = faces.mapIndexed { index, face ->
            val avgZ = face.vertexIndices.map { cameraTranslatedVertices[it].z }.average()
            Pair(index, avgZ)
        }.sortedByDescending { it.second }

        val canvas = drawScope.drawContext.canvas.nativeCanvas

        for ((faceIndex, avgZ) in faceDepths) {
            // Do not render objects behind the camera
            if (avgZ <= 0.1f) continue

            val face = faces[faceIndex]
            val path = Path()
            var valid = true

            for (i in face.vertexIndices.indices) {
                val vert = cameraTranslatedVertices[face.vertexIndices[i]]
                if (vert.z <= 0.05f) {
                    valid = false
                    break
                }
                // Perspective project
                val sx = cx + (vert.x * fov) / vert.z + screenShakeX
                val sy = cy - (vert.y * fov) / vert.z + screenShakeY // screen goes down

                if (i == 0) {
                    path.moveTo(sx, sy)
                } else {
                    path.lineTo(sx, sy)
                }
            }

            if (valid) {
                path.close()
                if (!face.lineOnly) {
                    paint.color = face.color.hashCode()
                    canvas.drawPath(path, paint)
                }
                if (face.outlineColor != null) {
                    edgePaint.color = face.outlineColor.hashCode()
                    canvas.drawPath(path, edgePaint)
                }
            }
        }
    }

    companion object {
        // Generates an interactive sports car shape
        fun createSportsCar(primaryColor: Color, secondaryColor: Color): Model3D {
            val verts = listOf(
                // HOOD/FRONT (0 to 3)
                Vector3D(-0.6f, -0.2f, 1.5f),  // 0 Front bottom left
                Vector3D(0.6f, -0.2f, 1.5f),   // 1 Front bottom right
                Vector3D(0.5f, 0.0f, 1.4f),    // 2 Front top right
                Vector3D(-0.5f, 0.0f, 1.4f),   // 3 Front top left

                // CABIN/BODY (4 to 7)
                Vector3D(-0.65f, -0.2f, -1.5f), // 4 Rear bottom left
                Vector3D(0.65f, -0.2f, -1.5f),  // 5 Rear bottom right
                Vector3D(0.5f, 0.2f, -0.6f),    // 6 Mid cabin roof right
                Vector3D(-0.5f, 0.2f, -0.6f),   // 7 Mid cabin roof left

                // COCKPIT WINDSHIELD (8, 9, 10, 11)
                Vector3D(-0.45f, 0.45f, -0.4f),  // 8 Cockpit roof back left
                Vector3D(0.45f, 0.45f, -0.4f),   // 9 Cockpit roof back right
                Vector3D(0.4f, 0.45f, -0.1f),    // 10 Cockpit roof front right
                Vector3D(-0.4f, 0.45f, -0.1f),   // 11 Cockpit roof front left

                // REAR SPOILER (12 to 15)
                Vector3D(-0.6f, 0.3f, -1.45f),  // 12 Spoiler top left
                Vector3D(0.6f, 0.3f, -1.45f),   // 13 Spoiler top right
                Vector3D(0.6f, -0.1f, -1.45f),  // 14 Spoiler bottom right
                Vector3D(-0.6f, -0.1f, -1.45f)   // 15 Spoiler bottom left
            )

            val faces = listOf(
                // Hood front face
                Face3D(listOf(0, 1, 2, 3), primaryColor, outlineColor = Color.White.copy(alpha=0.3f)),
                // Hood top
                Face3D(listOf(3, 2, 6, 7), secondaryColor, outlineColor = Color.White.copy(alpha=0.2f)),
                // Windshield
                Face3D(listOf(7, 6, 10, 11), Color.Cyan.copy(alpha = 0.6f), outlineColor = Color.White),
                // Roof
                Face3D(listOf(11, 10, 9, 8), ClassyRoofColor, outlineColor = Color.White.copy(alpha=0.3f)),
                // Rear Windshield
                Face3D(listOf(8, 9, 5, 4), Color.Cyan.copy(alpha=0.4f)),
                // Left Side Panels
                Face3D(listOf(0, 3, 7, 4), primaryColor, outlineColor = Color.Black.copy(alpha=0.4f)),
                // Right Side Panels
                Face3D(listOf(1, 5, 6, 2), primaryColor, outlineColor = Color.Black.copy(alpha=0.4f)),
                // Spoiler
                Face3D(listOf(12, 13, 14, 15), Color.Red, outlineColor = Color.White)
            )

            return Model3D(verts, faces)
        }

        // Generates a Cyber truck / Cyber style futuristic steel vehicle
        fun createCyberTruck(primaryColor: Color): Model3D {
            val verts = listOf(
                // Base structure (bottom)
                Vector3D(-0.7f, -0.2f, 1.8f), // 0 Front Left
                Vector3D(0.7f, -0.2f, 1.8f),  // 1 Front Right
                Vector3D(0.7f, -0.2f, -1.8f), // 2 Rear Right
                Vector3D(-0.7f, -0.2f, -1.8f),// 3 Rear Left

                // Angular Roof peak
                Vector3D(-0.6f, 0.55f, -0.1f), // 4 Left Peak
                Vector3D(0.6f, 0.55f, -0.1f),  // 5 Right Peak

                // Front nose (mid line height)
                Vector3D(-0.68f, 0.1f, 1.7f),  // 6 Front Top Left
                Vector3D(0.68f, 0.1f, 1.7f),   // 7 Front Top Right

                // Rear deck (flat tail)
                Vector3D(-0.65f, 0.15f, -1.7f), // 8 Rear Top Left
                Vector3D(0.65f, 0.15f, -1.7f)   // 9 Rear Top Right
            )

            val faces = listOf(
                // Front LED bar nose
                Face3D(listOf(0, 1, 7, 6), Color(0xFFE0E0E0), outlineColor = Color.Yellow),
                // Front windshield ramp
                Face3D(listOf(6, 7, 5, 4), primaryColor, outlineColor = Color.Cyan),
                // Rear bed slide
                Face3D(listOf(4, 5, 9, 8), Color(0xFF2B2B2B), outlineColor = Color.LightGray),
                // Side Left Panel
                Face3D(listOf(0, 6, 4, 8, 3), primaryColor.copy(alpha = 0.9f), outlineColor = Color.DarkGray),
                // Side Right Panel
                Face3D(listOf(1, 2, 9, 5, 7), primaryColor.copy(alpha = 0.9f), outlineColor = Color.DarkGray),
                // Rear Tail Gate
                Face3D(listOf(3, 8, 9, 2), Color.DarkGray, outlineColor = Color.Red)
            )

            return Model3D(verts, faces)
        }

        // Generates a Monster Truck (Very armor-looking, big)
        fun createMonsterTruck(primaryColor: Color): Model3D {
            val verts = listOf(
                // Big cabin chassis base
                Vector3D(-0.8f, 0.1f, 1.3f), // 0
                Vector3D(0.8f, 0.1f, 1.3f),  // 1
                Vector3D(0.8f, 0.1f, -1.3f), // 2
                Vector3D(-0.8f, 0.1f, -1.3f),// 3

                // Monster cabin roof
                Vector3D(-0.6f, 0.8f, 0.3f),  // 4
                Vector3D(0.6f, 0.8f, 0.3f),   // 5
                Vector3D(0.6f, 0.8f, -0.6f),  // 6
                Vector3D(-0.6f, 0.8f, -0.6f), // 7

                // Front radiator grille
                Vector3D(-0.75f, 0.4f, 1.25f), // 8
                Vector3D(0.75f, 0.4f, 1.25f),  // 9

                // Heavy exhaust pipes
                Vector3D(-0.3f, 1.1f, -0.8f),  // 10
                Vector3D(0.3f, 1.1f, -0.8f)    // 11
            )

            val faces = listOf(
                // Grille
                Face3D(listOf(0, 1, 9, 8), Color.Gray, outlineColor = Color.White),
                // Windshield
                Face3D(listOf(8, 9, 5, 4), Color.Black, outlineColor = Color.Blue),
                // Roof
                Face3D(listOf(4, 5, 6, 7), primaryColor, outlineColor = Color.White),
                // Sides
                Face3D(listOf(0, 8, 4, 7, 3), primaryColor.copy(alpha=0.95f), outlineColor = Color.Yellow),
                Face3D(listOf(1, 2, 6, 5, 9), primaryColor.copy(alpha=0.95f), outlineColor = Color.Yellow),
                // Exhaust stacks represented
                Face3D(listOf(6, 11, 7, 10), Color(0xFFFF5722), outlineColor = Color.White)
            )

            return Model3D(verts, faces)
        }

        // Generic obstacle: Brick Barrier
        fun createBrickWall(color: Color = Color(0xFFB03A2E)): Model3D {
            val verts = listOf(
                Vector3D(-0.8f, -0.3f, 0.3f), // 0
                Vector3D(0.8f, -0.3f, 0.3f),  // 1
                Vector3D(0.8f, 0.7f, 0.3f),   // 2
                Vector3D(-0.8f, 0.7f, 0.3f),  // 3

                Vector3D(-0.8f, -0.3f, -0.3f), // 4
                Vector3D(0.8f, -0.3f, -0.3f),  // 5
                Vector3D(0.8f, 0.7f, -0.3f),   // 6
                Vector3D(-0.8f, 0.7f, -0.3f)   // 7
            )

            val faces = listOf(
                // Front
                Face3D(listOf(0, 1, 2, 3), color, outlineColor = Color.White),
                // Back
                Face3D(listOf(5, 4, 7, 6), color.copy(alpha = 0.8f), outlineColor = Color.White),
                // Top
                Face3D(listOf(3, 2, 6, 7), color.copy(alpha = 0.9f), outlineColor = Color.White),
                // Left
                Face3D(listOf(4, 0, 3, 7), color.copy(alpha = 0.75f)),
                // Right
                Face3D(listOf(1, 5, 6, 2), color.copy(alpha = 0.75f))
            )
            return Model3D(verts, faces)
        }

        // Generates a realistic rotating tire
        fun createTire(): Model3D {
            val verts = mutableListOf<Vector3D>()
            val numSegments = 8
            val rOuter = 0.45f
            val rInner = 0.2f
            val width = 0.35f

            // Front ring
            for (i in 0 until numSegments) {
                val angle = (i * 2.0 * Math.PI / numSegments).toFloat()
                verts.add(Vector3D(rOuter * cos(angle), rOuter * sin(angle), width / 2f))
            }
            // Back ring
            for (i in 0 until numSegments) {
                val angle = (i * 2.0 * Math.PI / numSegments).toFloat()
                verts.add(Vector3D(rOuter * cos(angle), rOuter * sin(angle), -width / 2f))
            }

            val faces = mutableListOf<Face3D>()
            val darkGrey = Color(0xFF1E1E1E)
            val grey = Color(0xFF3E3E3E)

            // Front face (wheel spokes/rim indicator)
            faces.add(Face3D((0 until numSegments).toList(), grey, outlineColor = Color.LightGray))

            // Back face
            faces.add(Face3D(((numSegments * 2 - 1) downTo numSegments).toList(), darkGrey))

            // Outer cylinder wall
            for (i in 0 until numSegments) {
                val next = (i + 1) % numSegments
                faces.add(
                    Face3D(
                        listOf(i, next, next + numSegments, i + numSegments),
                        darkGrey,
                        outlineColor = Color.Black
                    )
                )
            }

            return Model3D(verts, faces)
        }

        val ClassyRoofColor = Color(0xAA112233)
    }
}
