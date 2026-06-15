package com.example.game

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun CrashBGameApp(context: Context) {
    val gameState = remember { GameState(context) }
    val effectsHelper = remember { EffectsHelper(context) }

    var currentScreen by remember { mutableStateOf("menu") } // "menu", "garage", "play"
    var selectedMode by remember { mutableStateOf(GameMode.DEMOLITION) }

    DisposableEffect(Unit) {
        onDispose {
            effectsHelper.release()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F0E17))
    ) {
        when (currentScreen) {
            "menu" -> {
                MenuScreen(
                    gameState = gameState,
                    onStartGame = { mode ->
                        selectedMode = mode
                        gameState.resetGameplay(mode)
                        currentScreen = "play"
                    },
                    onNavigateGarage = {
                        currentScreen = "garage"
                    }
                )
            }
            "garage" -> {
                GarageScreen(
                    gameState = gameState,
                    effectsHelper = effectsHelper,
                    onBack = {
                        currentScreen = "menu"
                    }
                )
            }
            "play" -> {
                PlayScreen(
                    gameState = gameState,
                    mode = selectedMode,
                    effectsHelper = effectsHelper,
                    onExit = {
                        gameState.endRoundCheck(selectedMode)
                        currentScreen = "menu"
                    }
                )
            }
        }
    }
}

@Composable
fun MenuScreen(
    gameState: GameState,
    onStartGame: (GameMode) -> Unit,
    onNavigateGarage: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "menuGrids")
    val gridOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 100f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "gridScroll"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .drawBehind {
                // Retro sci-fi background grid rendering
                val w = size.width
                val h = size.height
                val gridColor = Color(0x1F00E676)
                // Draw horizontal lines with perspective scaling
                var y = h * 0.4f
                while (y < h) {
                    val progress = (y - h * 0.4f) / (h * 0.6f)
                    val densityScroll = y + (gridOffset * progress)
                    if (densityScroll < h) {
                        drawLine(
                            color = gridColor,
                            start = Offset(0f, densityScroll),
                            end = Offset(w, densityScroll),
                            strokeWidth = 1f + progress * 2.5f
                        )
                    }
                    y += 40f * (1.1f + progress * 2f)
                }
                // Vertical lines with perspective
                val numGridLines = 14
                for (i in 0..numGridLines) {
                    val ratio = i.toFloat() / numGridLines
                    val startX = w * ratio
                    val endX = w * 0.5f + (ratio - 0.5f) * w * 4.5f
                    drawLine(
                        color = gridColor,
                        start = Offset(startX, h * 0.4f),
                        end = Offset(endX, h),
                        strokeWidth = 2f
                    )
                }
            }
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
            .windowInsetsPadding(WindowInsets.safeDrawing),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(28.dp))

        // Title Header
        Text(
            text = "CRASH B",
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 58.sp,
            color = Color(0xFFFF5722),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .testTag("app_title")
                .drawBehind {
                    drawCircle(
                        color = Color(0x2FFF5722),
                        radius = 120f,
                        center = center
                    )
                }
        )
        Text(
            text = "3D DESTY CAR SIMULATOR",
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Light,
            fontSize = 13.sp,
            color = Color(0xFF00E676),
            letterSpacing = 4.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(35.dp))

        // Coin Indicator Bar
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF2A2438))
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = "Monnaie",
                tint = Color(0xFFFFDF00)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "${gameState.totalCoins} B-Coins",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Game Modes choice
        Text(
            text = "SÉLECTIONNER UN MODE DE JEU",
            color = Color.LightGray,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp,
            modifier = Modifier.align(Alignment.Start)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Demolition Mode Card
        ModeCard(
            title = "Mode Démo / Crasheur",
            description = "Foncez à haute vitesse pour détruire les barricades de briques ! Plus l'impact est violent, plus vous gagnez de points.",
            icon = Icons.Default.Clear,
            badgeColor = Color(0xFFEF5350),
            highScoreLabel = "Record: ${gameState.highScoreCrash} smashes",
            onClick = { onStartGame(GameMode.DEMOLITION) }
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Survival Mode Card
        ModeCard(
            title = "Mode Survie",
            description = "Une course infinie d'évitement. Esquivez les blocs et véhicules roulant en sens inverse. Jusqu'où irez-vous ?",
            icon = Icons.Default.Warning,
            badgeColor = Color(0xFFFFB300),
            highScoreLabel = "Distance max: ${gameState.highScoreSurvival.toInt()} m",
            onClick = { onStartGame(GameMode.SURVIVAL) }
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Chrono Speedrun Mode Card
        ModeCard(
            title = "Chrono Tour de Piste",
            description = "Battez le chronomètre en évitant de vous crasher pour réaliser le tour parfait sur notre piste rétro alpine !",
            icon = Icons.Default.Refresh,
            badgeColor = Color(0xFF29B6F6),
            highScoreLabel = "Meilleur temps: ${if (gameState.highScoreChrono > 900f) "--" else "%.2f".format(gameState.highScoreChrono)}s",
            onClick = { onStartGame(GameMode.CHRONO) }
        )

        Spacer(modifier = Modifier.height(35.dp))

        // Garage CTA
        Button(
            onClick = onNavigateGarage,
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .testTag("garage_button"),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF00E676),
                contentColor = Color.Black
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Build, contentDescription = null)
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "ENTRER AU GARAGE",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Crashes analytics
        Text(
            text = "Total de carambolages épiques: ${gameState.totalCrashesCount}",
            color = Color.Gray,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
fun ModeCard(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    badgeColor: Color,
    highScoreLabel: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color(0x601F1D2B)),
        border = BorderStroke(1.dp, Color(0xFF2D2A44)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(badgeColor.copy(alpha = 0.25f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = icon, contentDescription = null, tint = badgeColor)
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(text = title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(text = highScoreLabel, color = Color(0xFF00E676), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(text = description, color = Color.LightGray, fontSize = 13.sp, lineHeight = 18.sp)
        }
    }
}

@Composable
fun GarageScreen(
    gameState: GameState,
    effectsHelper: EffectsHelper,
    onBack: () -> Unit
) {
    var viewRotationY by remember { mutableStateOf(0f) }
    var rotationInterval by remember { mutableStateOf(true) }

    // Selected vehicle state
    var selectedVehicleIndex by remember { mutableStateOf(0) }
    val currentVehicle = gameState.vehiclesList[selectedVehicleIndex]

    // Rotate garage 3D car continuously code
    LaunchedEffect(rotationInterval) {
        if (rotationInterval) {
            while (true) {
                viewRotationY += 0.024f
                delay(16)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        // Top navbar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color(0xFF242235))
            ) {
                Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Retour", tint = Color.White)
            }
            Text(
                text = "LE GARAGE",
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = Color.White
            )
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF00E676))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(imageVector = Icons.Default.Star, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = "${gameState.totalCoins}", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 3D Visual Studio viewport inside a stylish container
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1.2f)
                .clip(RoundedCornerShape(16.dp))
                .border(2.dp, Color(0xFF2A2438))
                .background(Color(0xFF08070F))
                .pointerInput(Unit) {
                    detectHorizontalDragGestures { change, dragAmount ->
                        change.consume()
                        rotationInterval = false
                        viewRotationY += dragAmount * 0.015f
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            // Standard Compose Canvas draws 3D rotating model
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cx = size.width / 2f
                val cy = size.height / 2f + 40f
                val fov = size.width * 0.52f

                // Define 3D model
                val model = when (currentVehicle.type) {
                    "cyber" -> Model3D.createCyberTruck(currentVehicle.baseColor)
                    "monster" -> Model3D.createMonsterTruck(currentVehicle.baseColor)
                    else -> Model3D.createSportsCar(currentVehicle.baseColor, currentVehicle.accentColor)
                }

                // Render platform under model
                val platformModel = Model3D.createBrickWall(Color(0xFF1E1E1E)).transform(
                    scale = Vector3D(2.5f, 0.15f, 2.5f),
                    rotationRad = Vector3D(0f, 0f, 0f),
                    translation = Vector3D(0f, -0.6f, 0f)
                )

                platformModel.render(
                    drawScope = this,
                    cx = cx,
                    cy = cy,
                    fov = fov,
                    cameraPos = Vector3D(0f, 0.6f, -3.2f),
                    cameraRot = Vector3D(0.25f, 0f, 0f)
                )

                // Render current car
                val animatedCar = model.transform(
                    scale = Vector3D(0.85f, 0.85f, 0.85f),
                    rotationRad = Vector3D(0f, viewRotationY, 0f),
                    translation = Vector3D(0f, -0.2f, 0f)
                )

                animatedCar.render(
                    drawScope = this,
                    cx = cx,
                    cy = cy,
                    fov = fov,
                    cameraPos = Vector3D(0f, 0.6f, -3.2f),
                    cameraRot = Vector3D(0.25f, 0f, 0f)
                )
            }

            // Carousel arrow tags
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .align(Alignment.Center),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = {
                        rotationInterval = true
                        selectedVehicleIndex = if (selectedVehicleIndex == 0) gameState.vehiclesList.size -1 else selectedVehicleIndex - 1
                    },
                    modifier = Modifier.background(Color(0x35FFFFFF), CircleShape)
                ) {
                    Icon(imageVector = Icons.Default.KeyboardArrowLeft, contentDescription = "Prev", tint = Color.White)
                }
                IconButton(
                    onClick = {
                        rotationInterval = true
                        selectedVehicleIndex = (selectedVehicleIndex + 1) % gameState.vehiclesList.size
                    },
                    modifier = Modifier.background(Color(0x35FFFFFF), CircleShape)
                ) {
                    Icon(imageVector = Icons.Default.KeyboardArrowRight, contentDescription = "Next", tint = Color.White)
                }
            }

            // Visual hints
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(imageVector = Icons.Default.Refresh, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(12.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = "Glisser pour faire tourner dans l'atelier", color = Color.Gray, fontSize = 10.sp)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Vehicle info and unlocking/selecting CTAs
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1F1D2B)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = currentVehicle.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(text = "Aptitudes de type ${currentVehicle.type.uppercase()}", color = Color(0xFF00E676), fontSize = 11.sp)
                    }

                    if (!currentVehicle.isUnlocked) {
                        Button(
                            onClick = {
                                if (gameState.purchaseVehicle(currentVehicle)) {
                                    effectsHelper.playUnlockSound()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB300)),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Menu, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "Acheter ${currentVehicle.price}", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    } else {
                        val isActive = gameState.activeVehicleId == currentVehicle.id
                        Button(
                            onClick = {
                                gameState.activeVehicleId = currentVehicle.id
                                gameState.saveAllState()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isActive) Color(0xFF00E676) else Color(0x35FFFFFF)
                            ),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = if (isActive) "ÉQUIPÉ" else "ÉQUIPER",
                                color = if (isActive) Color.Black else Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Vehicle upgrading section
        Text(
            text = "DÉVELOPPEMENT STATS DE PERFORMANCE (NIVEAUX 0-5)",
            color = Color.LightGray,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1.5f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            UpgradeRow(
                title = "Vitesse de pointe (${currentVehicle.getMaxSpeed().toInt()} km/h)",
                level = currentVehicle.speedUpgrades,
                upgradeCost = 150 + (currentVehicle.speedUpgrades * 100),
                enabled = currentVehicle.isUnlocked && currentVehicle.speedUpgrades < 5,
                onClick = {
                    gameState.upgradeStat(currentVehicle, "speed")
                    effectsHelper.vibrate(80, 150)
                }
            )
            UpgradeRow(
                title = "Puissance d'accélération (${(currentVehicle.getAcceleration() * 10).toInt()} units)",
                level = currentVehicle.accelUpgrades,
                upgradeCost = 150 + (currentVehicle.accelUpgrades * 100),
                enabled = currentVehicle.isUnlocked && currentVehicle.accelUpgrades < 5,
                onClick = {
                    gameState.upgradeStat(currentVehicle, "accel")
                    effectsHelper.vibrate(80, 150)
                }
            )
            UpgradeRow(
                title = "Maniabilité / Drifts (${(currentVehicle.getHandling() * 10).toInt()} pts)",
                level = currentVehicle.handlingUpgrades,
                upgradeCost = 150 + (currentVehicle.handlingUpgrades * 100),
                enabled = currentVehicle.isUnlocked && currentVehicle.handlingUpgrades < 5,
                onClick = {
                    gameState.upgradeStat(currentVehicle, "handling")
                    effectsHelper.vibrate(80, 150)
                }
            )
            UpgradeRow(
                title = "Coque Blindée / Armor (${(currentVehicle.getArmor() * 100).toInt()}% Absorption)",
                level = currentVehicle.armorUpgrades,
                upgradeCost = 150 + (currentVehicle.armorUpgrades * 100),
                enabled = currentVehicle.isUnlocked && currentVehicle.armorUpgrades < 5,
                onClick = {
                    gameState.upgradeStat(currentVehicle, "armor")
                    effectsHelper.vibrate(80, 150)
                }
            )
        }
    }
}

@Composable
fun UpgradeRow(
    title: String,
    level: Int,
    upgradeCost: Int,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0x351F1D2B)),
        border = BorderStroke(1.dp, Color(0xFF242231))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1.2f)) {
                Text(text = title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(4.dp))
                // Draw 5 upgrade blocks
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    for (i in 1..5) {
                        val active = i <= level
                        Box(
                            modifier = Modifier
                                .size(width = 24.dp, height = 6.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(if (active) Color(0xFF00E676) else Color(0x35FFFFFF))
                        )
                    }
                }
            }

            if (enabled) {
                Button(
                    onClick = onClick,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676)),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = Color.Black, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(text = "$upgradeCost", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                }
            } else {
                Text(
                    text = if (level >= 5) "MAX" else "NON DISPO",
                    color = Color.LightGray,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }
        }
    }
}

@Composable
fun PlayScreen(
    gameState: GameState,
    mode: GameMode,
    effectsHelper: EffectsHelper,
    onExit: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var isStartedCountDown by remember { mutableStateOf(3) }
    var isPlayPaused by remember { mutableStateOf(false) }

    // Steer angle touch calculation state
    var physicalSteeringWheelAngle by remember { mutableStateOf(0f) }

    // Loop game runner trigger ticks
    LaunchedEffect(isPlayPaused, isStartedCountDown) {
        if (isStartedCountDown > 0) {
            while (isStartedCountDown > 0) {
                delay(1000)
                isStartedCountDown--
            }
        }

        if (!isPlayPaused && isStartedCountDown == 0) {
            var lastTime = System.nanoTime()
            while (gameState.carDamagePct < 100f && !isPlayPaused) {
                val currTime = System.nanoTime()
                val dt = ((currTime - lastTime) / 1_000_000_000f).coerceIn(0.005f, 0.05f)
                lastTime = currTime

                gameState.updatePhysics(dt, effectsHelper, mode)
                delay(12) // roughly 60 fps ticks
            }
        }
    }

    // Top Level Box Screen
    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("play_view")
    ) {
        // Continuous Render Canvas for full pseudo-3D graphics
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    // Let user steer by sliding finger on lower screen sides
                    detectDragGestures(
                        onDragStart = { },
                        onDragEnd = {
                            gameState.isSteeringLeft = false
                            gameState.isSteeringRight = false
                            physicalSteeringWheelAngle = 0f
                        },
                        onDragCancel = {
                            gameState.isSteeringLeft = false
                            gameState.isSteeringRight = false
                            physicalSteeringWheelAngle = 0f
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            if (dragAmount.x < -4) {
                                gameState.isSteeringLeft = true
                                gameState.isSteeringRight = false
                                physicalSteeringWheelAngle = -35f
                            } else if (dragAmount.x > 4) {
                                gameState.isSteeringRight = true
                                gameState.isSteeringLeft = false
                                physicalSteeringWheelAngle = 35f
                            }
                        }
                    )
                }
        ) {
            val cx = size.width / 2f
            val cy = size.height * 0.45f
            val fov = size.width * 0.48f

            // Dynamic camera shake offsets computed
            val shakeX = if (gameState.cameraShakeIntensity > 0f) {
                (Math.random().toFloat() * 2f - 1f) * gameState.cameraShakeIntensity
            } else 0f
            val shakeY = if (gameState.cameraShakeIntensity > 0f) {
                (Math.random().toFloat() * 2f - 1f) * gameState.cameraShakeIntensity
            } else 0f

            // Clean sky dome
            val skyGradient = Brush.verticalGradient(
                colors = if (mode == GameMode.SURVIVAL) {
                    listOf(Color(0xFF0F0C20), Color(0xFF4A154B))
                } else {
                    listOf(Color(0xFF020024), Color(0xFF090979), Color(0xFF00D4FF))
                }
            )
            drawRect(brush = skyGradient, size = Size(size.width, cy))

            // Ground Trapezoid space
            drawRect(
                color = Color(0xFF388A3F),
                topLeft = Offset(0f, cy),
                size = Size(size.width, size.height - cy)
            )

            // Draw procedural road segments
            val totalSegToDraw = 48
            var previousSegX = cx
            var previousSegY = size.height
            var previousSegW = size.width * 0.85f

            var roadOffsetAccum = 0f

            // Start iteration in back-to-front order of visual segments
            for (offset in totalSegToDraw downTo 1) {
                val index = (gameState.currentSegmentIndex + offset) % gameState.trackSegments.size
                val segment = gameState.trackSegments[index]

                // Accumulate curves as we move forward along track
                roadOffsetAccum += segment.curve * (offset * 0.08f)

                // Depth scaling
                val scale = 0.95f / (offset + 1.2f)
                val segmentX = cx + (roadOffsetAccum - gameState.lateralCarOffset * 100f) * scale + shakeX
                val segmentY = cy + (segment.hill * 40f + 160f) * scale + shakeY
                val segmentW = size.width * 0.65f * scale

                // Draw grass sides stripes alternating
                val alternateBg = (index % 3 == 0)
                val stripeColor = if (alternateBg) Color(0xFF2E6F22) else Color(0xFF357F2B)

                if (offset < totalSegToDraw) {
                    // Left grass
                    val leftPol = Path().apply {
                        moveTo(0f, previousSegY)
                        lineTo(previousSegX - previousSegW, previousSegY)
                        lineTo(segmentX - segmentW, segmentY)
                        lineTo(0f, segmentY)
                        close()
                    }
                    drawPath(leftPol, stripeColor)

                    // Right grass
                    val rightPol = Path().apply {
                        moveTo(size.width, previousSegY)
                        lineTo(previousSegX + previousSegW, previousSegY)
                        lineTo(segmentX + segmentW, segmentY)
                        lineTo(size.width, segmentY)
                        close()
                    }
                    drawPath(rightPol, rightPolColors(alternateBg, mode))

                    // Main asphalt road polygon
                    val roadPol = Path().apply {
                        moveTo(previousSegX - previousSegW, previousSegY)
                        moveTo(previousSegX - previousSegW * 0.88f, previousSegY)
                        lineTo(previousSegX + previousSegW * 0.88f, previousSegY)
                        lineTo(segmentX + segmentW * 0.88f, segmentY)
                        lineTo(segmentX - segmentW * 0.88f, segmentY)
                        close()
                    }
                    drawPath(roadPol, Color(0xFF1B1A24))

                    // White sideline guides
                    val linePathLeft = Path().apply {
                        moveTo(previousSegX - previousSegW * 0.88f, previousSegY)
                        lineTo(segmentX - segmentW * 0.88f, segmentY)
                        lineTo(segmentX - segmentW * 0.85f, segmentY)
                        lineTo(previousSegX - previousSegW * 0.85f, previousSegY)
                        close()
                    }
                    drawPath(linePathLeft, Color.White)

                    val linePathRight = Path().apply {
                        moveTo(previousSegX + previousSegW * 0.85f, previousSegY)
                        lineTo(segmentX + segmentW * 0.85f, segmentY)
                        lineTo(segmentX + segmentW * 0.88f, segmentY)
                        lineTo(previousSegX + previousSegW * 0.88f, previousSegY)
                        close()
                    }
                    drawPath(linePathRight, Color.White)

                    // Alternating road center dashes dotted lines
                    if (index % 4 == 0) {
                        val centerLine = Path().apply {
                            moveTo(previousSegX, previousSegY)
                            lineTo(segmentX, segmentY)
                            lineTo(segmentX + segmentW * 0.04f, segmentY)
                            lineTo(previousSegX + previousSegW * 0.04f, previousSegY)
                            close()
                        }
                        drawPath(centerLine, Color.White)
                    }

                    // Render dynamic obstacle blocks/opponents
                    if (segment.obstacleType != ObstacleType.NONE) {
                        val obsScaleX = segmentX + (segment.obstacleX * segmentW * 0.8f)
                        val obsY = segmentY
                        val sizeMultiplier = segmentW * 0.12f

                        if (!segment.obstacleSmashed) {
                            when (segment.obstacleType) {
                                ObstacleType.BRICK_WALL -> {
                                    val wall = Model3D.createBrickWall().transform(
                                        scale = Vector3D(sizeMultiplier * 0.12f, sizeMultiplier * 0.07f, sizeMultiplier * 0.1f),
                                        rotationRad = Vector3D(0f, 0f, 0f),
                                        translation = Vector3D(0f, 0f, 0f)
                                    )
                                    // Project onto Canvas coords
                                    wall.render(
                                        drawScope = this,
                                        cx = obsScaleX,
                                        cy = obsY,
                                        fov = fov,
                                        cameraPos = Vector3D(0f, 0.4f, 5f)
                                    )
                                }
                                ObstacleType.OPPONENT_CAR -> {
                                    val carModel = Model3D.createSportsCar(Color(0xFFE040FB), Color.Yellow).transform(
                                        scale = Vector3D(sizeMultiplier * 0.1f, sizeMultiplier * 0.1f, sizeMultiplier * 0.12f),
                                        rotationRad = Vector3D(0f, 3.14f, 0f), // Facing away
                                        translation = Vector3D(0f, 0.1f, 0f)
                                    )
                                    carModel.render(
                                        drawScope = this,
                                        cx = obsScaleX,
                                        cy = obsY,
                                        fov = fov,
                                        cameraPos = Vector3D(0f, 0.4f, 5f)
                                    )
                                }
                                ObstacleType.COIN_GATE -> {
                                    // Draw glowing floating star gem
                                    val gemY = obsY - segmentW * 0.22f
                                    drawCircle(
                                        color = Color(0xFFFFEB3B),
                                        radius = segmentW * 0.088f,
                                        center = Offset(obsScaleX, gemY)
                                    )
                                    drawCircle(
                                        color = Color.Black,
                                        radius = segmentW * 0.041f,
                                        center = Offset(obsScaleX, gemY)
                                    )
                                }
                                else -> {}
                            }
                        }
                    }
                }

                previousSegX = segmentX
                previousSegY = segmentY
                previousSegW = segmentW
            }

            // Draw dynamic flying crash debris particles !
            for (p in gameState.debrisList) {
                val debrisModel = Model3D.createBrickWall(p.color).transform(
                    scale = Vector3D(p.size, p.size, p.size),
                    rotationRad = p.rotation,
                    translation = p.position
                )
                debrisModel.render(
                    drawScope = this,
                    cx = cx,
                    cy = cy + 120f,
                    fov = fov,
                    cameraPos = Vector3D(0f, 0f, 0f)
                )
            }

            // Draw user-equipped player sports car at lower foreground center
            val userCarModel = when (gameState.getActiveVehicle().type) {
                "cyber" -> Model3D.createCyberTruck(gameState.getActiveVehicle().baseColor)
                "monster" -> Model3D.createMonsterTruck(gameState.getActiveVehicle().baseColor)
                else -> Model3D.createSportsCar(gameState.getActiveVehicle().baseColor, gameState.getActiveVehicle().accentColor)
            }.transform(
                scale = Vector3D(1.15f, 1.15f, 1.15f),
                rotationRad = Vector3D(0f, physicalSteeringWheelAngle * 0.005f, 0f), // tilt based on steer
                translation = Vector3D(0f, -0.6f + (Math.random().toFloat() * 0.012f * (gameState.currentSpeed / 100f)), 2.2f) // bumpy rattle action
            )

            userCarModel.render(
                drawScope = this,
                cx = cx,
                cy = size.height - 180f,
                fov = fov,
                cameraPos = Vector3D(0f, 0f, 0f)
            )

            // Draw Nitro booster exhaust flames visually!
            if (gameState.isNitroActive) {
                drawFlameExhaust(this, cx, size.height - 110f)
                // Draw high warp lines
                for (k in 0..15) {
                    val side = if (k % 2 == 0) 1 else -1
                    val sy = Math.random().toFloat() * size.height
                    val swipeW = Math.random().toFloat() * 110f + 25f
                    val sx = if (side == 1) Math.random().toFloat() * 80f else size.width - Math.random().toFloat() * 80f
                    drawLine(
                        color = Color.Cyan.copy(alpha = 0.45f),
                        start = Offset(sx.toFloat(), sy),
                        end = Offset((sx + swipeW * side).toFloat(), sy),
                        strokeWidth = 3f
                    )
                }
            }
        }

        // Overlay Interactive Dashboard & Speedometer Gauges
        PlayHUDOverlay(
            gameState = gameState,
            mode = mode,
            isPaused = isPlayPaused,
            onPauseToggle = { isPlayPaused = !isPlayPaused },
            onExit = onExit,
            effectsHelper = effectsHelper
        )

        // Count Down Text Overlay UI
        if (isStartedCountDown > 0) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.65f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "$isStartedCountDown",
                    fontSize = 110.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF00E676),
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.animateContentSize()
                )
            }
        }

        // Wrecked Game Over Modal Overlay
        if (gameState.carDamagePct >= 100f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.85f)),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .padding(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF241E33)),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(2.dp, Color.Red)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color.Red,
                            modifier = Modifier.size(54.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "VÉHICULE DÉTRUIT !",
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp,
                            color = Color.White
                        )
                        Text(
                            text = "Votre châssis n'a pas résisté aux impacts violents.",
                            color = Color.LightGray,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        // Score details
                        SummaryStatisticRow("Distance parcourue", "${gameState.distanceTravelled.toInt()} mètres")
                        if (mode == GameMode.DEMOLITION) {
                            SummaryStatisticRow("Barrières détruites", "${gameState.numUnfinishedCrashesCount} barricades")
                        } else {
                            SummaryStatisticRow("Score de Crash", "${gameState.numUnfinishedCrashesCount} collisions (KO)")
                        }
                        SummaryStatisticRow("Butin accumulé", "+${(gameState.distanceTravelled / 20).toInt() + gameState.numUnfinishedCrashesCount * 15} B-Coins")

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = onExit,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF5350)),
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) {
                            Text(text = "RETOURNER AU MÉNU", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// Side green stripes vs dark purple side borders
private fun rightPolColors(alternateBg: Boolean, mode: GameMode): Color {
    return if (mode == GameMode.SURVIVAL) {
         if (alternateBg) Color(0xFFE91E63) else Color(0xBB9C27B0)
    } else {
         if (alternateBg) Color(0xFF2E6F22) else Color(0xFF357F2B)
    }
}

private fun drawFlameExhaust(drawScope: DrawScope, cx: Float, cy: Float) {
    val canvas = drawScope.drawContext.canvas.nativeCanvas
    val paint = android.graphics.Paint().apply {
        color = Color(0xFFFF5722).hashCode()
        style = android.graphics.Paint.Style.FILL
        isAntiAlias = true
    }
    // High cyan core flame path
    val path = Path().apply {
        moveTo(cx - 35f, cy)
        lineTo(cx, cy + 85f)
        lineTo(cx + 35f, cy)
        close()
    }
    canvas.drawPath(path.asAndroidPath(), paint)

    paint.color = Color.Cyan.hashCode()
    val corePath = Path().apply {
        moveTo(cx - 15f, cy)
        lineTo(cx, cy + 45f)
        lineTo(cx + 15f, cy)
        close()
    }
    canvas.drawPath(corePath.asAndroidPath(), paint)
}

@Composable
fun SummaryStatisticRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = Color.Gray, fontSize = 14.sp)
        Text(text = value, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
}

@Composable
fun PlayHUDOverlay(
    gameState: GameState,
    mode: GameMode,
    isPaused: Boolean,
    onPauseToggle: () -> Unit,
    onExit: () -> Unit,
    effectsHelper: EffectsHelper
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        // TOP HUD: Pause, Lap stats, Damage slider
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onPauseToggle,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color(0x951A1829))
            ) {
                Icon(
                    imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Menu,
                    contentDescription = "Menu",
                    tint = Color.White
                )
            }

            // Stat capsules
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Damage Badge HUD
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xBF1F1D2B)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = Color.Red, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "DÉGÂTS: ${gameState.carDamagePct.toInt()}%",
                            color = if (gameState.carDamagePct > 70f) Color.Red else Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }

                // Score capsule depending on mode
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xBF1F1D2B)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = when (mode) {
                            GameMode.CHRONO -> "CHRONO: %.2fs".format(gameState.currentLapTime)
                            GameMode.DEMOLITION -> "CRASHB: ${gameState.numUnfinishedCrashesCount} SMASHES"
                            GameMode.SURVIVAL -> "DST: ${gameState.distanceTravelled.toInt()} m"
                        },
                        color = Color(0xFF00E676),
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // BOTTOM CONTROLS: Speedometer, Gas/Brake Pedals, Nitro action
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Speed indicator overlay
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Large digital speedometer
                Column {
                    Text(
                        text = "${gameState.currentSpeed.toInt()}",
                        fontFamily = FontFamily.Monospace,
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 44.sp,
                        modifier = Modifier.height(48.dp)
                    )
                    Text(text = "KM/H", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                // Nitro booster button tag
                Button(
                    onClick = {
                        if (gameState.nitrosLeftCount > 0 && !gameState.isNitroActive) {
                            effectsHelper.playNitroSound()
                            effectsHelper.vibrate(180, 255)
                            gameState.isNitroActive = true
                            gameState.nitrosLeftCount--
                            // Nitro expires after 3 seconds
                            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                                gameState.isNitroActive = false
                            }, 3200)
                        }
                    },
                    modifier = Modifier
                        .size(68.dp)
                        .testTag("nitro_button"),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (gameState.isNitroActive) Color.Cyan else Color(0xFFFF5722)
                    ),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(imageVector = Icons.Default.Star, contentDescription = null, tint = Color.Black)
                        Text(
                            text = "NITRO [${gameState.nitrosLeftCount}]",
                            color = Color.Black,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }

            // Real physical gas & brakes touch areas (Hold zones)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Brake pedal
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(75.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFE53935).copy(alpha = 0.85f))
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onPress = {
                                    gameState.isBraking = true
                                    try {
                                        awaitRelease()
                                    } finally {
                                        gameState.isBraking = false
                                    }
                                }
                            )
                        }
                        .testTag("brake_pedal"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "FREIN / RECUL", color = Color.White, fontWeight = FontWeight.Black, fontSize = 14.sp)
                }

                // Giant steering wheel overlay guide
                Box(
                    modifier = Modifier
                        .size(75.dp)
                        .clip(CircleShape)
                        .background(Color(0x35FFFFFF))
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onPress = {
                                    effectsHelper.playHorn()
                                    effectsHelper.vibrate(50, 100)
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = Icons.Default.Share, contentDescription = "Klaxon", tint = Color.White, modifier = Modifier.size(28.dp))
                }

                // Gas pedal
                Box(
                    modifier = Modifier
                        .weight(1.3f)
                        .height(75.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF00E676).copy(alpha = 0.85f))
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onPress = {
                                    gameState.isBraking = false
                                    // User accelerates by holding down
                                    effectsHelper.vibrate(20, 50)
                                }
                            )
                        }
                        .testTag("gas_pedal"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "ACCÉLÉRER", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 14.sp)
                }
            }

            // Steering instructions assist
            Text(
                text = "Glissez votre doigt à gauche/droite pour diriger le bolide !",
                color = Color.LightGray,
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Pause Menu Dialog
        if (isPaused) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.8F)),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(0.8F),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1C2C))
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = "JEU EN PAUSE", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        Spacer(modifier = Modifier.height(18.dp))

                        Button(
                            onClick = onPauseToggle,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(text = "REPRENDRE", color = Color.Black, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedButton(
                            onClick = onExit,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(text = "ABANDONNER")
                        }
                    }
                }
            }
        }
    }
}
