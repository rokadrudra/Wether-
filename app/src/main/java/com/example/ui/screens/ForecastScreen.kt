package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.api.DailyForecast
import com.example.data.api.HourlyForecast
import com.example.ui.WeatherUiState
import com.example.ui.WeatherViewModel
import com.example.ui.theme.WeatherThemeHelper
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ForecastScreen(
    viewModel: WeatherViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        when (val state = uiState) {
            is WeatherUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Loading full forecasts...", style = MaterialTheme.typography.bodyLarge, color = Color.White)
                }
            }
            is WeatherUiState.Success -> {
                val data = state.data
                val weather = data.weather
                val hourly = weather.hourly
                val daily = weather.daily

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Extended Forecast",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Forecast indices for ${state.cityName.split(",")[0]}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.7f)
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    if (hourly != null) {
                        HourlyForecastCard(hourly = hourly, viewModel = viewModel)
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    if (daily != null) {
                        DailyForecastListCard(daily = daily, viewModel = viewModel)
                    }

                    Spacer(modifier = Modifier.height(64.dp))
                }
            }
            is WeatherUiState.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "Could not fetch forecast details.", color = Color.White)
                }
            }
        }
    }
}

@Composable
fun HourlyForecastCard(hourly: HourlyForecast, viewModel: WeatherViewModel) {
    val tempUnitPref by viewModel.tempUnit.collectAsState()
    val isCelsius = tempUnitPref == "C"
    val scrollState = rememberScrollState()

    fun formatHour(timeStr: String): String {
        return try {
            val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.US)
            val formatter = SimpleDateFormat("h a", Locale.US)
            val date = parser.parse(timeStr)
            if (date != null) formatter.format(date) else timeStr.split("T").lastOrNull() ?: timeStr
        } catch (e: Exception) {
            timeStr.split("T").lastOrNull() ?: timeStr
        }
    }

    GlassmorphicCard(modifier = Modifier.fillMaxWidth().testTag("hourly_forecast_card")) {
        Text(
            text = "Hourly Conditions (Next 24h)",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState)
                .padding(bottom = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Take the next 24 data indices
            val times = hourly.time.take(24)
            val temps = hourly.temperature2m.take(24)
            val codes = hourly.weatherCode.take(24)

            times.forEachIndexed { index, time ->
                val hourLabel = if (index == 0) "Now" else formatHour(time)
                val tempVal = temps[index]
                val convertedTemp = if (isCelsius) tempVal else (tempVal * 9 / 5 + 32)
                val roundedTemp = Math.round(convertedTemp)
                val spec = WeatherThemeHelper.getWeatherSpec(codes[index])

                Column(
                    modifier = Modifier
                        .width(60.dp)
                        .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                        .padding(vertical = 12.dp, horizontal = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = hourLabel,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = spec.iconEmoji,
                        fontSize = 24.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "$roundedTemp°",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun DailyForecastListCard(daily: DailyForecast, viewModel: WeatherViewModel) {
    val tempUnitPref by viewModel.tempUnit.collectAsState()
    val isCelsius = tempUnitPref == "C"

    // Helper to extract day name
    fun formatDayName(timeStr: String, index: Int): String {
        if (index == 0) return "Today"
        return try {
            val parser = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val formatter = SimpleDateFormat("EEEE", Locale.US)
            val date = parser.parse(timeStr)
            if (date != null) formatter.format(date) else timeStr
        } catch (e: Exception) {
            timeStr
        }
    }

    // Determine aggregate global min & max to draw standard heat bars correctly
    val globalMin = daily.temperature2mMin.minOrNull() ?: 0.0
    val globalMax = daily.temperature2mMax.maxOrNull() ?: 40.0
    val globalSpan = (globalMax - globalMin).coerceAtLeast(1.0)

    GlassmorphicCard(modifier = Modifier.fillMaxWidth().testTag("daily_forecast_card")) {
        Text(
            text = "7-Day Forecast",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(16.dp))

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            daily.time.forEachIndexed { index, time ->
                val dayLabel = formatDayName(time, index)
                val code = daily.weatherCode[index]
                val spec = WeatherThemeHelper.getWeatherSpec(code)

                val minT = daily.temperature2mMin[index]
                val maxT = daily.temperature2mMax[index]

                val displayMin = if (isCelsius) minT else (minT * 9 / 5 + 32)
                val displayMax = if (isCelsius) maxT else (maxT * 9 / 5 + 32)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Day name (scaled width)
                    Text(
                        text = dayLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        modifier = Modifier.width(90.dp)
                    )

                    // Emoji indicator
                    Text(
                        text = spec.iconEmoji,
                        fontSize = 22.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.width(36.dp)
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    // Raw min value
                    Text(
                        text = "${Math.round(displayMin)}°",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.6f),
                        textAlign = TextAlign.End,
                        modifier = Modifier.width(32.dp)
                    )

                    // Premium Canvas-Based Temperature range bar
                    Canvas(
                        modifier = Modifier
                            .weight(1f)
                            .height(8.dp)
                            .padding(horizontal = 12.dp)
                    ) {
                        val widthPx = size.width
                        val heightPx = size.height

                        // Draw background track line
                        drawRoundRect(
                            color = Color.White.copy(alpha = 0.15f),
                            size = Size(widthPx, heightPx),
                            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                        )

                        // Calculate visual start and length of the active span
                        val startPct = ((minT - globalMin) / globalSpan).coerceIn(0.0, 1.0)
                        val endPct = ((maxT - globalMin) / globalSpan).coerceIn(0.0, 1.0)

                        val barStart = startPct.toFloat() * widthPx
                        val barWidth = ((endPct - startPct).toFloat() * widthPx).coerceAtLeast(4.dp.toPx())

                        // Draw premium thermal gradient representing the temperature band
                        drawRoundRect(
                            color = Color(0xFF38BDF8), // cool blue accent
                            topLeft = Offset(barStart, 0f),
                            size = Size(barWidth, heightPx),
                            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                        )
                    }

                    // Raw max value
                    Text(
                        text = "${Math.round(displayMax)}°",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.End,
                        modifier = Modifier.width(32.dp)
                    )
                }
            }
        }
    }
}
