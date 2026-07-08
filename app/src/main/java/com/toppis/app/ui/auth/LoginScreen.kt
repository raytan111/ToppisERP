package com.toppis.app.ui.auth

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

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

/** Construye el path de una estrella de 5 puntas (con radios elípticos para el foreshortening). */
private fun estrellaPath(
    cx: Float, cy: Float,
    rxOut: Float, ryOut: Float,
    rxIn: Float, ryIn: Float,
    puntas: Int = 5
): Path = Path().apply {
    val paso = PI.toFloat() / puntas
    var a = -PI.toFloat() / 2f // arranca en la punta de arriba
    for (i in 0 until puntas * 2) {
        val rx = if (i % 2 == 0) rxOut else rxIn
        val ry = if (i % 2 == 0) ryOut else ryIn
        val x = cx + rx * cos(a)
        val y = cy + ry * sin(a)
        if (i == 0) moveTo(x, y) else lineTo(x, y)
        a += paso
    }
    close()
}

/** Paleta dorada de la moneda (oro viejo, más oscuro; combina con rojo/crema). */
private val OroBrillo = Color(0xFFE8CC7A)
private val OroClaro = Color(0xFFC9A24A)
private val OroMedio = Color(0xFFA67C2E)
private val OroProfundo = Color(0xFF6E4E14)
private val OroSombra = Color(0xFF3E2B0A)

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
    val logo = ImageBitmap.imageResource(id = com.toppis.erp.R.drawable.toppis_logo)

    Box(
        modifier = Modifier.size(156.dp),
        contentAlignment = Alignment.Center
    ) {
        // ── Sombra proyectada (estática, no gira) ────────────────────────────
        Canvas(
            modifier = Modifier
                .size(124.dp)
                .offset(y = 16.dp)
        ) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.Black.copy(alpha = 0.40f), Color.Transparent),
                    center = center,
                    radius = size.minDimension / 2f
                )
            )
        }

        // ── Moneda 3D: proyección manual con rebanadas en profundidad ────────
        Canvas(modifier = Modifier.size(132.dp)) {
            val cx0 = size.width / 2f
            val cy = size.height / 2f
            val theta = angulo * PI.toFloat() / 180f
            val c = cos(theta)              // foreshortening horizontal de la cara
            val s = sin(theta)              // desplazamiento del canto al girar
            val cAbs = abs(c)
            val r = size.minDimension * 0.40f
            val grosor = size.minDimension * 0.10f   // espesor real de la moneda
            val rxFace = r * cAbs                     // radio horizontal (elipse)

            // Cuerpo/canto: apilo rebanadas desde el fondo (-grosor/2) al frente.
            val n = 22
            val edgeBrush = Brush.verticalGradient(
                colors = listOf(OroBrillo, OroClaro, OroMedio, OroSombra),
                startY = cy - r,
                endY = cy + r
            )
            for (i in 0..n) {
                val t = i / n.toFloat()
                val z = (t - 0.5f) * grosor          // profundidad de la rebanada
                val cx = cx0 + z * s                 // posición en pantalla al rotar
                drawOval(
                    brush = edgeBrush,
                    topLeft = Offset(cx - rxFace, cy - r),
                    size = Size(2f * rxFace, 2f * r)
                )
            }

            // Cara frontal (la rebanada más cercana al observador).
            val zFront = 0.5f * grosor
            val cxFront = cx0 + zFront * s
            val faceCenter = Offset(cxFront, cy)

            // Degradado dorado de la cara.
            drawOval(
                brush = Brush.radialGradient(
                    colors = listOf(OroBrillo, OroClaro, OroMedio, OroProfundo),
                    center = Offset(faceCenter.x - rxFace * 0.3f, cy - r * 0.3f),
                    radius = r * 1.15f
                ),
                topLeft = Offset(faceCenter.x - rxFace, cy - r),
                size = Size(2f * rxFace, 2f * r)
            )
            // Bisel interior brillante.
            drawOval(
                color = OroBrillo.copy(alpha = 0.55f),
                topLeft = Offset(faceCenter.x - rxFace, cy - r),
                size = Size(2f * rxFace, 2f * r),
                style = Stroke(width = size.minDimension * 0.02f)
            )

            val deFrente = c >= 0f
            // Solo dibujar contenido de la cara si no está casi de canto.
            if (cAbs > 0.12f) {
                if (deFrente) {
                    // Logo recortado en la cara, con foreshortening horizontal.
                    val fw = 2f * rxFace * 0.66f
                    val fh = 2f * r * 0.66f
                    val clip = Path().apply {
                        addOval(
                            Rect(
                                Offset(faceCenter.x - rxFace, cy - r),
                                Size(2f * rxFace, 2f * r)
                            )
                        )
                    }
                    clipPath(clip) {
                        drawImage(
                            image = logo,
                            srcOffset = IntOffset.Zero,
                            srcSize = IntSize(logo.width, logo.height),
                            dstOffset = IntOffset(
                                (faceCenter.x - fw / 2f).roundToInt(),
                                (cy - fh / 2f).roundToInt()
                            ),
                            dstSize = IntSize(fw.roundToInt().coerceAtLeast(1), fh.roundToInt().coerceAtLeast(1))
                        )
                    }
                } else {
                    // Cara trasera terminada: estrella de 5 puntas acuñada + aro de perlas.
                    // Aro interior (borde grabado).
                    drawOval(
                        color = OroSombra.copy(alpha = 0.45f),
                        topLeft = Offset(faceCenter.x - rxFace * 0.82f, cy - r * 0.82f),
                        size = Size(2f * rxFace * 0.82f, 2f * r * 0.82f),
                        style = Stroke(width = size.minDimension * 0.015f)
                    )
                    // Perlas alrededor (beading), foreshortened con la cara.
                    val perlas = 24
                    for (p in 0 until perlas) {
                        val a = p * (2f * PI.toFloat() / perlas)
                        val px = faceCenter.x + rxFace * 0.9f * cos(a)
                        val py = cy + r * 0.9f * sin(a)
                        drawCircle(
                            color = OroSombra.copy(alpha = 0.35f),
                            radius = size.minDimension * 0.008f,
                            center = Offset(px, py)
                        )
                    }
                    // Estrella de 5 puntas (relieve): sombra + cara + brillo.
                    val estrella = estrellaPath(
                        cx = faceCenter.x, cy = cy,
                        rxOut = rxFace * 0.5f, ryOut = r * 0.5f,
                        rxIn = rxFace * 0.22f, ryIn = r * 0.22f
                    )
                    // Sombra (desplazada abajo-derecha) para dar volumen.
                    drawPath(
                        estrellaPath(
                            cx = faceCenter.x + rxFace * 0.02f, cy = cy + r * 0.02f,
                            rxOut = rxFace * 0.5f, ryOut = r * 0.5f,
                            rxIn = rxFace * 0.22f, ryIn = r * 0.22f
                        ),
                        color = OroSombra.copy(alpha = 0.5f)
                    )
                    drawPath(estrella, color = OroProfundo)
                    drawPath(estrella, color = OroBrillo.copy(alpha = 0.6f), style = Stroke(width = size.minDimension * 0.012f))
                }
            }
        }
    }
}
