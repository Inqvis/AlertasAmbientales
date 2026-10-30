package com.example.alertatemperaturas

import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Paper = Color(0xFFF4F1EA)
private val Ink = Color(0xFF1F2A22)
private val Muted = Color(0xFF6B6F66)
private val Hairline = Color(0xFFDAD5C8)

enum class SensorStatus(val label: String, val color: Color) {
    OPTIMAL("En rango", Color(0xFF3F6B4A)),
    WARNING("Atención", Color(0xFFB7791F)),
    CRITICAL("Crítico", Color(0xFFB4442F))
}

data class SensorReading(
    val title: String,
    val value: String,
    val unit: String,
    val numeric: Float,
    val status: SensorStatus,
    val detail: String,
    val scale: ClosedFloatingPointRange<Float>,
    val optimal: ClosedFloatingPointRange<Float>,
    val critLow: Float,
    val critHigh: Float,
    val rangeNote: String
)

fun evaluateSensors(
    tempC: Double,
    soilHumidityPct: Int,
    airHumidityPct: Int,
    lightPct: Int
): List<SensorReading> = listOf(
    SensorReading(
        title = "Temperatura",
        value = "%.1f".format(tempC),
        unit = "°C",
        numeric = tempC.toFloat(),
        status = when {
            tempC < 10.0 || tempC > 30.0 -> SensorStatus.CRITICAL
            tempC in 18.0..25.0 -> SensorStatus.OPTIMAL
            else -> SensorStatus.WARNING
        },
        detail = when {
            tempC > 30.0 -> "sobre 30 °C"
            tempC < 10.0 -> "bajo 10 °C"
            tempC > 25.0 -> "sobre el óptimo"
            tempC < 18.0 -> "bajo el óptimo"
            else -> ""
        },
        scale = 0f..40f, optimal = 18f..25f, critLow = 10f, critHigh = 30f,
        rangeNote = "Óptimo 18–25 °C · crítico bajo 10 o sobre 30"
    ),
    SensorReading(
        title = "Humedad del suelo",
        value = "$soilHumidityPct",
        unit = "%",
        numeric = soilHumidityPct.toFloat(),
        status = when {
            soilHumidityPct < 30 || soilHumidityPct > 90 -> SensorStatus.CRITICAL
            soilHumidityPct in 60..80 -> SensorStatus.OPTIMAL
            else -> SensorStatus.WARNING
        },
        detail = when {
            soilHumidityPct < 30 -> "sequía"
            soilHumidityPct > 90 -> "saturación"
            soilHumidityPct > 80 -> "sobre el óptimo"
            soilHumidityPct < 60 -> "bajo el óptimo"
            else -> ""
        },
        scale = 0f..100f, optimal = 60f..80f, critLow = 30f, critHigh = 90f,
        rangeNote = "Óptimo 60–80 % · sequía bajo 30 · saturación sobre 90"
    ),
    SensorReading(
        title = "Humedad ambiental",
        value = "$airHumidityPct",
        unit = "%",
        numeric = airHumidityPct.toFloat(),
        status = when {
            airHumidityPct < 40 || airHumidityPct > 85 -> SensorStatus.CRITICAL
            airHumidityPct in 60..70 -> SensorStatus.OPTIMAL
            else -> SensorStatus.WARNING
        },
        detail = when {
            airHumidityPct < 40 -> "ambiente seco"
            airHumidityPct > 85 -> "riesgo de hongos"
            airHumidityPct > 70 -> "sobre el óptimo"
            airHumidityPct < 60 -> "bajo el óptimo"
            else -> ""
        },
        scale = 0f..100f, optimal = 60f..70f, critLow = 40f, critHigh = 85f,
        rangeNote = "Óptimo 60–70 % · seco bajo 40 · hongos sobre 85"
    ),
    SensorReading(
        title = "Luz",
        value = "$lightPct",
        unit = "%",
        numeric = lightPct.toFloat(),
        status = when {
            lightPct < 40 -> SensorStatus.CRITICAL
            lightPct >= 70 -> SensorStatus.OPTIMAL
            else -> SensorStatus.WARNING
        },
        detail = when {
            lightPct < 20 -> "luz muy baja"
            lightPct < 40 -> "déficit de luz"
            lightPct < 70 -> "luz moderada"
            else -> ""
        },
        scale = 0f..100f, optimal = 70f..100f, critLow = 40f, critHigh = 100f,
        rangeNote = "Óptimo desde 70 % · déficit bajo 40 · crítico bajo 20"
    )
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Light background is forced, so keep dark status bar icons even in system dark mode.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
        )
        setContent { AgroVerdeDashboard() }
    }
}

@Composable
fun AgroVerdeDashboard() {
    var readings by remember { mutableStateOf<List<SensorReading>>(emptyList()) }
    var statusLine by remember { mutableStateOf("Conectando con Firebase…") }

    DisposableEffect(Unit) {
        val registration = FirebaseFirestore.getInstance()
            .collection("monitoreo_agricola")
            .document("estado_actual")
            .addSnapshotListener { doc, error ->
                val temp = doc?.getDouble("temperatura")
                val soil = doc?.getLong("humedad_suelo")
                val air = doc?.getLong("humedad_relativa")
                val light = doc?.getLong("nivel_luz")
                when {
                    error != null -> statusLine = "Error de conexión: ${error.message}"
                    doc == null || !doc.exists() -> statusLine = "Sin datos en estado_actual"
                    temp == null || soil == null || air == null || light == null ->
                        statusLine = "Documento incompleto en estado_actual"
                    else -> {
                        readings = evaluateSensors(temp, soil.toInt(), air.toInt(), light.toInt())
                        statusLine = "En vivo · actualizado " +
                            SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
                    }
                }
            }
        onDispose { registration.remove() }
    }

    DashboardContent(readings, statusLine)
}

@Composable
private fun DashboardContent(readings: List<SensorReading>, statusLine: String) {
    val alerts = readings.filter { it.status != SensorStatus.OPTIMAL }
    val hasCritical = alerts.any { it.status == SensorStatus.CRITICAL }

    LaunchedEffect(alerts.size, hasCritical) {
        if (alerts.isEmpty()) return@LaunchedEffect
        runCatching {
            val tone = if (hasCritical) ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD else ToneGenerator.TONE_PROP_BEEP
            val generator = ToneGenerator(AudioManager.STREAM_ALARM, 85)
            generator.startTone(tone, 400)
            delay(500)
            generator.release()
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Paper)
    ) {
        Column(
            Modifier
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 20.dp)
        ) {
            Header(alertCount = alerts.size, total = readings.size, statusLine = statusLine)

            if (alerts.isNotEmpty()) {
                Spacer(Modifier.height(32.dp))
                SectionLabel("Requiere atención")
                alerts.forEach { AlertRow(it) }
            }

            if (readings.isNotEmpty()) {
                Spacer(Modifier.height(32.dp))
                SectionLabel("Lecturas")
            }
            readings.forEachIndexed { i, sensor ->
                if (i > 0) HorizontalDivider(color = Hairline, thickness = 1.dp)
                SensorRow(sensor)
            }
        }
    }
}

@Composable
private fun Header(alertCount: Int, total: Int, statusLine: String) {
    Text(
        "AGROVERDE GLOBAL",
        color = Muted,
        fontSize = 11.sp,
        letterSpacing = 2.sp,
        fontWeight = FontWeight.Medium
    )
    Text("Invernadero 1", color = Ink, fontSize = 15.sp)
    Spacer(Modifier.height(28.dp))
    Text(
        text = when {
            total == 0 -> "Esperando\nlecturas."
            alertCount == 0 -> "Todo en rango."
            else -> "$alertCount de $total lecturas\nfuera de rango."
        },
        color = Ink,
        fontFamily = FontFamily.Serif,
        fontSize = 34.sp,
        lineHeight = 40.sp
    )
    Spacer(Modifier.height(8.dp))
    Text(statusLine, color = Muted, fontSize = 13.sp)
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text.uppercase(),
        color = Muted,
        fontSize = 11.sp,
        letterSpacing = 2.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
private fun AlertRow(sensor: SensorReading) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .width(3.dp)
                .height(36.dp)
                .background(sensor.status.color)
        )
        Column(
            Modifier
                .weight(1f)
                .padding(start = 12.dp)
        ) {
            Text(sensor.title, color = Ink, fontSize = 16.sp)
            Text(
                "${sensor.status.label} · ${sensor.detail}",
                color = sensor.status.color,
                fontSize = 13.sp
            )
        }
        Text("${sensor.value} ${sensor.unit}", color = Ink, fontSize = 16.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun SensorRow(sensor: SensorReading) {
    Column(Modifier.padding(vertical = 18.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
            Column(Modifier.weight(1f)) {
                Text(sensor.title, color = Ink, fontSize = 16.sp)
                Text(
                    if (sensor.status == SensorStatus.OPTIMAL) sensor.status.label
                    else "${sensor.status.label} · ${sensor.detail}",
                    color = sensor.status.color,
                    fontSize = 13.sp
                )
            }
            Row(verticalAlignment = Alignment.Bottom) {
                Text(sensor.value, color = Ink, fontSize = 40.sp, fontWeight = FontWeight.Light, lineHeight = 40.sp)
                Text(
                    sensor.unit,
                    color = Muted,
                    fontSize = 15.sp,
                    modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        RangeBar(sensor)
        Spacer(Modifier.height(6.dp))
        Text(sensor.rangeNote, color = Muted, fontSize = 12.sp)
    }
}

/** Horizontal scale split into critical / warning / optimal bands, with a marker at the current value. */
@Composable
private fun RangeBar(sensor: SensorReading) {
    Canvas(
        Modifier
            .fillMaxWidth()
            .height(16.dp)
    ) {
        val min = sensor.scale.start
        val span = sensor.scale.endInclusive - min
        fun x(v: Float) = ((v - min) / span).coerceIn(0f, 1f) * size.width

        val barH = 5.dp.toPx()
        val top = (size.height - barH) / 2
        val bands = listOf(
            Triple(min, sensor.critLow, SensorStatus.CRITICAL.color.copy(alpha = 0.30f)),
            Triple(sensor.critLow, sensor.optimal.start, SensorStatus.WARNING.color.copy(alpha = 0.30f)),
            Triple(sensor.optimal.start, sensor.optimal.endInclusive, SensorStatus.OPTIMAL.color.copy(alpha = 0.45f)),
            Triple(sensor.optimal.endInclusive, sensor.critHigh, SensorStatus.WARNING.color.copy(alpha = 0.30f)),
            Triple(sensor.critHigh, sensor.scale.endInclusive, SensorStatus.CRITICAL.color.copy(alpha = 0.30f))
        )
        bands.forEach { (from, to, color) ->
            if (to > from) drawRect(color, Offset(x(from), top), Size(x(to) - x(from), barH))
        }

        val cx = x(sensor.numeric)
        val center = Offset(cx, size.height / 2)
        drawCircle(Paper, radius = 7.dp.toPx(), center = center)
        drawCircle(sensor.status.color, radius = 5.dp.toPx(), center = center)
    }
}

@Preview(showBackground = true, heightDp = 1100)
@Composable
fun AgroVerdeDashboardPreview() {
    DashboardContent(evaluateSensors(32.4, 68, 52, 35), "Vista previa")
}
