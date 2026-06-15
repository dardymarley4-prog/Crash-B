package com.example.game

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sin
import kotlin.random.Random

// Represents a 3D flying debris particle from crash impact!
data class DebrisParticle(
    var position: Vector3D,
    var velocity: Vector3D,
    var rotation: Vector3D,
    val rotSpeed: Vector3D,
    val color: Color,
    val size: Float,
    var lift: Float = 1f
)

// Vehicle specs in garage
data class Vehicle(
    val id: String,
    val name: String,
    val type: String, // "sports", "cyber", "monster"
    val baseColor: Color,
    val accentColor: Color,
    val baseSpeed: Float,        // Max potential speed
    val baseAcceleration: Float, // Accel rate
    val baseHandling: Float,     // Lateral responsiveness
    val baseArmor: Float,        // Health resilience (higher = less damage taken)
    val price: Int,
    var isUnlocked: Boolean = false,
    var speedUpgrades: Int = 0,
    var accelUpgrades: Int = 0,
    var handlingUpgrades: Int = 0,
    var armorUpgrades: Int = 0
) {
    fun getMaxSpeed() = baseSpeed + (speedUpgrades * 15f)
    fun getAcceleration() = baseAcceleration + (accelUpgrades * 0.08f)
    fun getHandling() = baseHandling + (handlingUpgrades * 0.05f)
    fun getArmor() = baseArmor + (armorUpgrades * 0.08f)
}

// Track segment for retro pseudo-3D feeling
data class TrackSegment(
    val index: Int,
    var curve: Float,  // Horizontal bend (-1 = left, 1 = right)
    var hill: Float,   // Vertical grade (-1 = down, 1 = up)
    var obstacleType: ObstacleType = ObstacleType.NONE,
    var obstacleX: Float = 0f, // Lateral spacing (-1f left, 1f right)
    var obstacleSmashed: Boolean = false,
    var opponentSpeed: Float = 0f
)

enum class ObstacleType {
    NONE,
    BRICK_WALL,
    OPPONENT_CAR,
    COIN_GATE
}

enum class GameMode {
    CHRONO,    // Time trial
    DEMOLITION, // Smash bricks for max score!
    SURVIVAL   // Endless dodging
}

class GameState(private val context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("crash_b_prefs", Context.MODE_PRIVATE)

    // Persistent values
    var totalCoins by mutableStateOf(prefs.getInt("total_coins", 1500))
    var highScoreChrono by mutableStateOf(prefs.getFloat("high_score_chrono", 999.9F)) // Lap time (lower is better)
    var highScoreCrash by mutableStateOf(prefs.getInt("high_score_crash", 0))    // Demo points
    var highScoreSurvival by mutableStateOf(prefs.getFloat("high_score_survival", 0f)) // Distance (meters)
    var totalCrashesCount by mutableStateOf(prefs.getInt("total_crashes", 0))

    // Garage vehicles State
    val vehiclesList = mutableStateListOf<Vehicle>()

    // Current selected car
    var activeVehicleId by mutableStateOf(prefs.getString("active_vehicle", "rusty_clipper") ?: "rusty_clipper")

    init {
        loadVehiclesState()
    }

    private fun loadVehiclesState() {
        vehiclesList.clear()
        vehiclesList.addAll(
            listOf(
                Vehicle(
                    id = "rusty_clipper",
                    name = "Rusty Clipper GT",
                    type = "sports",
                    baseColor = Color(0xFFA1887F),
                    accentColor = Color(0xFFE0F2F1),
                    baseSpeed = 135f,
                    baseAcceleration = 0.5f,
                    baseHandling = 0.7f,
                    baseArmor = 0.4f,
                    price = 0,
                    isUnlocked = true
                ),
                Vehicle(
                    id = "apex_monster",
                    name = "Apex Monster 4x4",
                    type = "monster",
                    baseColor = Color(0xFFEF5350),
                    accentColor = Color(0xFFFFEB3B),
                    baseSpeed = 175f,
                    baseAcceleration = 0.7f,
                    baseHandling = 0.6f,
                    baseArmor = 0.95f,
                    price = 450
                ),
                Vehicle(
                    id = "cyber_charger",
                    name = "Cyber Charger B",
                    type = "cyber",
                    baseColor = Color(0xFF00E676),
                    accentColor = Color(0xFF00B0FF),
                    baseSpeed = 230f,
                    baseAcceleration = 1.1f,
                    baseHandling = 0.85f,
                    baseArmor = 0.7f,
                    price = 1200
                ),
                Vehicle(
                    id = "formula_apex",
                    name = "Formula Apex X",
                    type = "sports",
                    baseColor = Color(0xFFFF5722),
                    accentColor = Color(0xFFFFFFFF),
                    baseSpeed = 310f,
                    baseAcceleration = 1.6f,
                    baseHandling = 1.3f,
                    baseArmor = 0.35f,
                    price = 2800
                )
            )
        )

        // Restore upgrades from preferences
        for (i in vehiclesList.indices) {
            val v = vehiclesList[i]
            val suffix = "_${v.id}"
            v.isUnlocked = prefs.getBoolean("unlocked$suffix", v.isUnlocked)
            v.speedUpgrades = prefs.getInt("speed_up$suffix", 0)
            v.accelUpgrades = prefs.getInt("accel_up$suffix", 0)
            v.handlingUpgrades = prefs.getInt("handling_up$suffix", 0)
            v.armorUpgrades = prefs.getInt("armor_up$suffix", 0)
        }
    }

    fun saveAllState() {
        val editor = prefs.edit()
        editor.putInt("total_coins", totalCoins)
        editor.putFloat("high_score_chrono", highScoreChrono)
        editor.putInt("high_score_crash", highScoreCrash)
        editor.putFloat("high_score_survival", highScoreSurvival)
        editor.putInt("total_crashes", totalCrashesCount)
        editor.putString("active_vehicle", activeVehicleId)

        for (v in vehiclesList) {
            val suffix = "_${v.id}"
            editor.putBoolean("unlocked$suffix", v.isUnlocked)
            editor.putInt("speed_up$suffix", v.speedUpgrades)
            editor.putInt("accel_up$suffix", v.accelUpgrades)
            editor.putInt("handling_up$suffix", v.handlingUpgrades)
            editor.putInt("armor_up$suffix", v.armorUpgrades)
        }
        editor.apply()
    }

    fun getActiveVehicle(): Vehicle {
        return vehiclesList.firstOrNull { it.id == activeVehicleId } ?: vehiclesList[0]
    }

    fun purchaseVehicle(vehicle: Vehicle): Boolean {
        if (totalCoins >= vehicle.price && !vehicle.isUnlocked) {
            totalCoins -= vehicle.price
            vehicle.isUnlocked = true
            activeVehicleId = vehicle.id
            saveAllState()
            return true
        }
        return false
    }

    fun upgradeStat(vehicle: Vehicle, statType: String): Boolean {
        val cost = 150 + (getUpgradeLevel(vehicle, statType) * 100)
        if (totalCoins >= cost && getUpgradeLevel(vehicle, statType) < 5) {
            totalCoins -= cost
            when (statType) {
                "speed" -> vehicle.speedUpgrades++
                "accel" -> vehicle.accelUpgrades++
                "handling" -> vehicle.handlingUpgrades++
                "armor" -> vehicle.armorUpgrades++
            }
            saveAllState()
            return true
        }
        return false
    }

    private fun getUpgradeLevel(vehicle: Vehicle, statType: String): Int {
        return when (statType) {
            "speed" -> vehicle.speedUpgrades
            "accel" -> vehicle.accelUpgrades
            "handling" -> vehicle.handlingUpgrades
            "armor" -> vehicle.armorUpgrades
            else -> 0
        }
    }

    // Dynamic gameplay properties
    var currentSpeed by mutableStateOf(0f) // km/h
    var distanceTravelled by mutableStateOf(0f) // meters
    var carDamagePct by mutableStateOf(0f) // 0 to 100% (Destroyed)
    var currentLapTime by mutableStateOf(0f) // seconds
    var numUnfinishedCrashesCount by mutableStateOf(0)
    var nitrosLeftCount by mutableStateOf(3)
    var isNitroActive by mutableStateOf(false)
    var isSteeringLeft by mutableStateOf(false)
    var isSteeringRight by mutableStateOf(false)
    var isBraking by mutableStateOf(false)
    var driftAngle by mutableStateOf(0f) // visually tilt camera and car!

    // Track state
    val trackSegments = mutableListOf<TrackSegment>()
    val SEGMENT_LENGTH = 150f // Z distance units
    var currentSegmentIndex = 0
    var lateralCarOffset = 0f // -2.5f (left dirt) to 2.5f (right dirt) with 0f being road center

    // Opponent positions / Obstacles positions
    val debrisList = mutableStateListOf<DebrisParticle>()

    // Screen Shake state triggers
    var cameraShakeIntensity by mutableStateOf(0f)

    init {
        generateTrack()
    }

    fun generateTrack() {
        trackSegments.clear()
        // Generate procedural infinite loop of track segments
        var currentCurve = 0f
        var currentHill = 0f

        val random = Random(42)

        for (i in 0..400) {
            // Gradually transition curve directions
            if (i % 40 == 0) {
                currentCurve = (random.nextFloat() * 1.8f - 0.9f) // curved left/right
                currentHill = (random.nextFloat() * 1.2f - 0.6f)  // hills & slopes
            }
            // First 15 segments are completely flat & empty for clean starting
            val isStart = i < 15
            var obstacle = ObstacleType.NONE
            var obsX = 0f

            if (!isStart && random.nextFloat() < 0.16f) {
                val roll = random.nextFloat()
                obstacle = when {
                    roll < 0.35f -> ObstacleType.BRICK_WALL
                    roll < 0.85f -> ObstacleType.OPPONENT_CAR
                    else -> ObstacleType.COIN_GATE
                }
                obsX = (random.nextFloat() * 1.4f - 0.7f) // placed randomly on track
            }

            trackSegments.add(
                TrackSegment(
                    index = i,
                    curve = if (isStart) 0f else currentCurve,
                    hill = if (isStart) 0f else currentHill,
                    obstacleType = obstacle,
                    obstacleX = obsX,
                    opponentSpeed = if (obstacle == ObstacleType.OPPONENT_CAR) 45f + random.nextFloat() * 35f else 0f
                )
            )
        }
    }

    fun spawnCrashDebris(worldPos: Vector3D, color: Color) {
        val random = Random.Default
        for (k in 0..12) {
            val velX = (random.nextFloat() * 14f - 7f)
            val velY = (random.nextFloat() * 15f + 5f)
            val velZ = (random.nextFloat() * -18f - 5f) // flying backwards relative to racer

            debrisList.add(
                DebrisParticle(
                    position = Vector3D(worldPos.x, worldPos.y + 0.2f, worldPos.z),
                    velocity = Vector3D(velX, velY, velZ),
                    rotation = Vector3D(random.nextFloat() * 3.14f, random.nextFloat() * 3.14f, 0f),
                    rotSpeed = Vector3D(random.nextFloat() * 2f - 1f, random.nextFloat() * 2f - 1f, random.nextFloat() * 2f - 1f),
                    color = color,
                    size = 0.15f + random.nextFloat() * 0.15f
                )
            )
        }
    }

    fun resetGameplay(mode: GameMode) {
        currentSpeed = 0f
        distanceTravelled = 0f
        carDamagePct = 0f
        currentLapTime = 0f
        numUnfinishedCrashesCount = 0
        nitrosLeftCount = if (mode == GameMode.SURVIVAL) 5 else 3
        isNitroActive = false
        lateralCarOffset = 0f
        currentSegmentIndex = 0
        isSteeringLeft = false
        isSteeringRight = false
        isBraking = false
        driftAngle = 0f
        cameraShakeIntensity = 0f
        debrisList.clear()

        // Regrow obstacles states
        for (seg in trackSegments) {
            seg.obstacleSmashed = false
        }
    }

    // Mechanics loops
    fun updatePhysics(dt: Float, effectsHelper: EffectsHelper, mode: GameMode) {
        val activeCar = getActiveVehicle()

        // Decay screen shakes
        if (cameraShakeIntensity > 0f) {
            cameraShakeIntensity = max(0f, cameraShakeIntensity - dt * 3.5f)
        }

        // Debris Physics updates
        val iterator = debrisList.iterator()
        while (iterator.hasNext()) {
            val p = iterator.next()
            p.position = p.position.plus(p.velocity.rotateY(driftAngle * 0.05f).plus(Vector3D(0f, 0f, -currentSpeed * 0.035f * dt)))
            // Add gravity simulation to debris
            p.velocity.y -= 25.0f * dt
            p.rotation = p.rotation.plus(p.rotSpeed)
            p.lift -= dt
            if (p.position.y < -1f || p.lift <= 0f) {
                iterator.remove()
            }
        }

        if (carDamagePct >= 100f) {
            // Car is wrecked, deceleration apply
            currentSpeed = max(0f, currentSpeed - 120f * dt)
            return
        }

        // Lateral input and drifts
        val maxSteerSpeed = activeCar.getHandling() * 2.8f
        if (isSteeringLeft) {
            lateralCarOffset -= maxSteerSpeed * dt * (0.3f + (currentSpeed / 130f))
            driftAngle = (driftAngle - 0.22f).coerceIn(-1.0f, 1.0f)
        } else if (isSteeringRight) {
            lateralCarOffset += maxSteerSpeed * dt * (0.3f + (currentSpeed / 130f))
            driftAngle = (driftAngle + 0.22f).coerceIn(-1.0f, 1.0f)
        } else {
            driftAngle = (driftAngle * 0.85f)
        }

        // Friction / Road margins penalty
        val isOutOfRoad = abs(lateralCarOffset) > 1.25f
        val currentMax = if (isNitroActive) {
            activeCar.getMaxSpeed() * 1.35f
        } else {
            if (isOutOfRoad) activeCar.getMaxSpeed() * 0.45f else activeCar.getMaxSpeed()
        }

        // Nitro action
        if (isNitroActive) {
            currentSpeed += activeCar.getAcceleration() * 155f * dt
            if (currentSpeed >= currentMax) {
                currentSpeed = currentMax
            }
        } else if (isBraking) {
            currentSpeed = max(0f, currentSpeed - 220f * dt)
        } else {
            // Natural acceleration forces
            if (currentSpeed < currentMax) {
                currentSpeed += activeCar.getAcceleration() * 45f * dt
            } else {
                currentSpeed -= 35f * dt // natural decay down to limits
            }
        }

        // Apply friction when out of road bounds
        if (isOutOfRoad) {
            // Continuous mild vibrate upon dirt off-road
            effectsHelper.vibrate(15, 35)
            if (currentSpeed > currentMax) {
                currentSpeed = max(currentMax, currentSpeed - 130f * dt)
            }
        }

        // Advance progress
        val forwardProgress = currentSpeed * 0.277f * dt // convert km/h to m/s
        distanceTravelled += forwardProgress
        currentSegmentIndex = (distanceTravelled / 8.5f).toInt() % trackSegments.size

        // Lap time
        currentLapTime += dt

        // Sound engine frequency tick
        effectsHelper.playEngineSound(currentSpeed / activeCar.getMaxSpeed())

        // Spot collisions with Obstacles/Opponents
        checkObstaclesCollisions(effectsHelper, mode)
    }

    private fun checkObstaclesCollisions(effectsHelper: EffectsHelper, mode: GameMode) {
        val segmentRange = (currentSegmentIndex + 1)..(currentSegmentIndex + 3)
        for (idx in segmentRange) {
            val actualIdx = idx % trackSegments.size
            val seg = trackSegments[actualIdx]
            if (seg.obstacleType == ObstacleType.NONE || seg.obstacleSmashed) continue

            // Distance triggers collision check when nearing segment
            // Z distance check: Player has reached the obstacle.
            val lateralDist = abs(lateralCarOffset - seg.obstacleX)

            if (lateralDist < 0.62f) {
                // Slam collision!
                seg.obstacleSmashed = true

                val activeCar = getActiveVehicle()
                val isCoin = seg.obstacleType == ObstacleType.COIN_GATE

                if (isCoin) {
                    totalCoins += 45
                    effectsHelper.playCheckpointSound()
                    effectsHelper.vibrate(50, 200)
                    saveAllState()
                } else {
                    // It's a wall or other car to CRASH into!
                    totalCrashesCount++
                    numUnfinishedCrashesCount++

                    // Shake intensity depends on speed ratio
                    val speedScalar = (currentSpeed / activeCar.getMaxSpeed()).coerceIn(0.1f, 1.4f)
                    cameraShakeIntensity = 18f * speedScalar

                    // Audio crunch haptics
                    effectsHelper.playCrashSound(speedScalar)
                    effectsHelper.vibrate(280, (230f * speedScalar).toInt())

                    // Spawning Debris
                    val obstacleColor = if (seg.obstacleType == ObstacleType.BRICK_WALL) Color(0xFFF44336) else Color.DarkGray
                    val pointOfImpact = Vector3D(seg.obstacleX, 0f, 12f)
                    spawnCrashDebris(pointOfImpact, obstacleColor)

                    // Physics feedback: Slow down the car immediately upon collision resistance
                    val impactRatio = if (seg.obstacleType == ObstacleType.BRICK_WALL) 0.65f else 0.45f
                    currentSpeed *= (1f - impactRatio)

                    // Compute Damage with Armor reduction
                    val rawDamage = if (seg.obstacleType == ObstacleType.BRICK_WALL) 35f else 22f
                    val armorMitigation = (1f - activeCar.getArmor() * 0.45f).coerceIn(0.3f, 1.0f)
                    val damageReceived = rawDamage * armorMitigation * speedScalar

                    carDamagePct = (carDamagePct + damageReceived).coerceIn(0f, 100f)

                    // Accumulate Demolition Mode score
                    if (mode == GameMode.DEMOLITION) {
                        val pointsMultiplier = if (isNitroActive) 3 else 1
                        val pointsAwarded = ((rawDamage * speedScalar).toInt() * 10) * pointsMultiplier
                        totalCoins += pointsAwarded / 5
                        saveAllState()
                    }
                }
            }
        }
    }

    fun endRoundCheck(mode: GameMode) {
        // High scores updates
        when (mode) {
            GameMode.CHRONO -> {
                if (currentLapTime < highScoreChrono && carDamagePct < 100f) {
                    highScoreChrono = currentLapTime
                }
                totalCoins += (200 - currentLapTime.toInt()).coerceAtLeast(30)
            }
            GameMode.DEMOLITION -> {
                if (numUnfinishedCrashesCount > highScoreCrash) {
                    highScoreCrash = numUnfinishedCrashesCount
                }
                totalCoins += numUnfinishedCrashesCount * 15
            }
            GameMode.SURVIVAL -> {
                if (distanceTravelled > highScoreSurvival) {
                    highScoreSurvival = distanceTravelled
                }
                totalCoins += (distanceTravelled / 20f).toInt()
            }
        }
        saveAllState()
    }
}
