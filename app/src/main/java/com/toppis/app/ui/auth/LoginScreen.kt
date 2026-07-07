package com.toppis.app.ui.auth

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos

@Composable
fun LoginScreen(
    viewModel: AuthViewModel,
    onLoginSuccess: () -> Unit
) {
    val authState by viewModel.authState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    // Reaccionar a cambios de estado
    LaunchedEffect(authState) {
        when (authState) {
            is AuthState.Success -> {
                viewModel.resetState()
                onLoginSuccess()
            }
            is AuthState.Error -> {
                snackbarHostState.showSnackbar((authState as AuthState.Error).message)
                viewModel.resetState()
            }
            else -> {}
        }
    }

    val cs = MaterialTheme.colorScheme
    val isLoading = authState is AuthState.Loading
    val habilitado = email.isNotBlank() && password.isNotBlank() && !isLoading

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = cs.surface,
        // Sin insets: el header rojo se dibuja detrás de la barra de estado.
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { _ ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── Cabecera de marca con gradiente (edge-to-edge hasta arriba) ──
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .clip(RoundedCornerShape(bottomStart = 44.dp, bottomEnd = 44.dp))
                    .background(
                        Brush.linearGradient(colors = listOf(cs.primary, cs.tertiary))
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.statusBarsPadding()
                ) {
                    LogoGiratorio()
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = "ToppisERP",
                        style = MaterialTheme.typography.displaySmall,
                        color = cs.onPrimary
                    )
                    Text(
                        text = "Sistema de gestión",
                        style = MaterialTheme.typography.bodyMedium,
                        color = cs.onPrimary.copy(alpha = 0.85f)
                    )
                }
            }

            Spacer(Modifier.height(28.dp))

            // ── Formulario en tarjeta elevada ────────────────────────────────
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    Text(
                        "Iniciar sesión",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(20.dp))

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Usuario") },
                        leadingIcon = { Icon(Icons.Filled.Person, contentDescription = "Usuario") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(16.dp))

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Contraseña") },
                        leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = "Contraseña") },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Filled.VisibilityOff
                                                  else Icons.Filled.Visibility,
                                    contentDescription = if (passwordVisible) "Ocultar" else "Mostrar"
                                )
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None
                                               else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(24.dp))

                    Button(
                        onClick = { viewModel.login(email, password) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        shape = RoundedCornerShape(16.dp),
                        enabled = habilitado,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = cs.primary,
                            contentColor = cs.onPrimary,
                            disabledContainerColor = cs.primary.copy(alpha = 0.35f),
                            disabledContentColor = cs.onPrimary.copy(alpha = 0.7f)
                        )
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                strokeWidth = 2.dp,
                                color = cs.onPrimary
                            )
                        } else {
                            Text("Ingresar", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

/** Paleta dorada de la moneda (combina con el rojo/crema del logo). */
private val OroBrillo = Color(0xFFFFF3C4)
private val OroClaro = Color(0xFFF4D160)
private val OroMedio = Color(0xFFD4AF37)
private val OroProfundo = Color(0xFFB8860B)
private val OroSombra = Color(0xFF7A5210)

/**
 * Moneda de oro que gira sobre su eje vertical: lento de frente (para verla más
 * tiempo) y rápido al pasar por detrás. Tiene canto grueso con iluminación,
 * sombra proyectada y, en la cara trasera, un emblema dorado (no el logo
 * espejado). Se logra con un easing simétrico.
 */
@Composable
private fun LogoGiratorio() {
    val transition = rememberInfiniteTransition(label = "logo")
    val angulo by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            // Ease-in-out simétrico: lento en 0°/360° (frente), rápido en 180° (detrás).
            animation = tween(3600, easing = CubicBezierEasing(0.45f, 0f, 0.55f, 1f)),
            repeatMode = RepeatMode.Restart
        ),
        label = "angulo"
    )
    // Cara frontal cuando el coseno del ángulo es positivo (0–90° y 270–360°).
    val deFrente = cos(angulo * PI.toFloat() / 180f) >= 0f

    Box(
        modifier = Modifier.size(150.dp),
        contentAlignment = Alignment.Center
    ) {
        // ── Sombra proyectada (estática, no gira) ────────────────────────────
        Canvas(
            modifier = Modifier
                .size(122.dp)
                .offset(y = 14.dp)
        ) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.Black.copy(alpha = 0.38f), Color.Transparent),
                    center = center,
                    radius = size.minDimension / 2f
                )
            )
        }

        // ── Moneda (gira en 3D) ──────────────────────────────────────────────
        Box(
            modifier = Modifier
                .size(116.dp)
                .graphicsLayer {
                    rotationY = angulo
                    cameraDistance = 16f * density
                },
            contentAlignment = Alignment.Center
        ) {
            // Cuerpo de la moneda: canto + cara con degradado dorado.
            Canvas(modifier = Modifier.fillMaxSize()) {
                val d = size.minDimension
                val r = d / 2f
                val rim = d * 0.11f // grosor del canto

                // Canto (borde grueso) con iluminación vertical: da sensación de espesor.
                drawCircle(color = OroProfundo, radius = r, center = center)
                drawCircle(
                    brush = Brush.verticalGradient(
                        colors = listOf(OroClaro, OroSombra)
                    ),
                    radius = r - rim / 2f,
                    center = center,
                    style = Stroke(width = rim)
                )

                // Cara: degradado radial con brillo arriba-izquierda.
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(OroBrillo, OroClaro, OroMedio, OroProfundo),
                        center = Offset(center.x - r * 0.28f, center.y - r * 0.28f),
                        radius = r * 1.15f
                    ),
                    radius = r - rim,
                    center = center
                )

                // Bisel interior brillante (borde de la cara).
                drawCircle(
                    color = OroBrillo.copy(alpha = 0.55f),
                    radius = r - rim,
                    center = center,
                    style = Stroke(width = d * 0.02f)
                )
            }

            if (deFrente) {
                // Cara frontal: el logo.
                Image(
                    painter = painterResource(id = com.toppis.erp.R.drawable.toppis_logo),
                    contentDescription = "Logo Toppis",
                    modifier = Modifier
                        .size(74.dp)
                        .clip(CircleShape)
                )
            } else {
                // Cara trasera: emblema dorado simétrico (rings + rombo).
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val d = size.minDimension
                    val r = d / 2f
                    drawCircle(
                        color = OroSombra.copy(alpha = 0.5f),
                        radius = r * 0.55f,
                        center = center,
                        style = Stroke(width = d * 0.03f)
                    )
                    drawCircle(
                        color = OroSombra.copy(alpha = 0.5f),
                        radius = r * 0.40f,
                        center = center,
                        style = Stroke(width = d * 0.02f)
                    )
                    // Rombo central
                    val k = r * 0.20f
                    val path = androidx.compose.ui.graphics.Path().apply {
                        moveTo(center.x, center.y - k)
                        lineTo(center.x + k, center.y)
                        lineTo(center.x, center.y + k)
                        lineTo(center.x - k, center.y)
                        close()
                    }
                    drawPath(path, color = OroSombra.copy(alpha = 0.55f))
                }
            }
        }
    }
}
