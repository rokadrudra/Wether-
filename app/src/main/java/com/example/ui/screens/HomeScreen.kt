package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.CombinedWeatherData
import com.example.ui.WeatherUiState
import com.example.ui.WeatherViewModel
import com.example.ui.theme.WeatherStatusSpec
import com.example.ui.theme.WeatherThemeHelper
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HomeScreen(
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
                    modifier = Modifier
                        .fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 4.dp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Fetching weather coordinates...",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
            is WeatherUiState.Success -> {
                val data = state.data
                val weather = data.weather
                val current = weather.current
                val spec = WeatherThemeHelper.getWeatherSpec(current?.weatherCode ?: 0)

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    // Top Bar Actions (Manual Location Detect Button and Favorite toggle)
                    HeaderActions(
                        viewModel = viewModel,
                        state = state,
                        spec = spec
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Hero Current Weather card
                    HeroWeatherCard(
                        data = data,
                        cityName = state.cityName,
                        viewModel = viewModel,
                        spec = spec
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Smart Rain Alerts Banner
                    RainAlertBanner(data = data, viewModel = viewModel)

                    Spacer(modifier = Modifier.height(16.dp))

                    // Core atmospheric details (Humidity, Wind, Pressure, Cloud Cover)
                    WeatherDetailsGrid(data = data, viewModel = viewModel)

                    Spacer(modifier = Modifier.height(16.dp))

                    // Sun Dial Sunrise/Sunset tracker
                    SunProgressCard(data = data)

                    Spacer(modifier = Modifier.height(16.dp))

                    // Gauges Row (UV Index and AQI)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        UvGaugeCard(data = data, modifier = Modifier.weight(1f))
                        AqiGaugeCard(data = data, modifier = Modifier.weight(1f))
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Weather Radar
                    RadarMapCard()

                    Spacer(modifier = Modifier.height(64.dp)) // Extra scroll spacing
                }
            }
            is WeatherUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    GlassmorphicCard(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudOff,
                                contentDescription = "Error",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Something went wrong",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = state.message,
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(
                                onClick = { viewModel.refreshWeather() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Text("Retry connection", color = MaterialTheme.colorScheme.onPrimary)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HeaderActions(
    viewModel: WeatherViewModel,
    state: WeatherUiState.Success,
    spec: WeatherStatusSpec
) {
    val favorites by viewModel.favoriteCities.collectAsState()
    val lat = state.data.weather.latitude
    val lon = state.data.weather.longitude
    // We try to approximate matching ids or names to toggle favorite
    val isFav = favorites.any {
        kotlin.math.abs(it.latitude - lat) < 0.05 && kotlin.math.abs(it.longitude - lon) < 0.05
    }

    // Centered pulsing dot animation
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = EaseInOutCirc),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Transparent glass block square button
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White.copy(alpha = 0.18f))
                .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                .clickable { viewModel.detectLocation() }
                .testTag("detect_location_button"),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.MyLocation,
                contentDescription = "Detect My Location",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }

        // Center location column with pulsing active dot
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = state.cityName.split(",")[0],
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF4ADE80).copy(alpha = pulseAlpha))
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = "CURRENT LOCATION",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White.copy(alpha = 0.8f),
                    letterSpacing = 1.sp
                )
            }
        }

        // Right favorite toggler in matching Glass Square
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White.copy(alpha = 0.18f))
                .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                .clickable {
                    val id = (lat * 1000 + lon * 1000).toLong()
                    viewModel.toggleFavorite(
                        cityId = id,
                        name = state.cityName.split(",")[0],
                        country = if (state.cityName.split(",").size > 1) state.cityName.split(",")[1].trim() else "",
                        state = "",
                        lat = lat,
                        lon = lon,
                        isFav = isFav
                    )
                }
                .testTag("favorite_toggle_button"),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                contentDescription = "Add to Favs",
                tint = if (isFav) Color.Red else Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun HeroWeatherCard(
    data: CombinedWeatherData,
    cityName: String,
    viewModel: WeatherViewModel,
    spec: WeatherStatusSpec
) {
    val tempUnitPref by viewModel.tempUnit.collectAsState()
    val isCelsius = tempUnitPref == "C"

    val current = data.weather.current
    if (current == null) return

    val rawTemp = current.temperature2m
    val displayTemp = if (isCelsius) rawTemp else (rawTemp * 9 / 5 + 32)
    val roundedTemp = Math.round(displayTemp)

    val rawMin = data.weather.daily?.temperature2mMin?.firstOrNull() ?: rawTemp
    val rawMax = data.weather.daily?.temperature2mMax?.firstOrNull() ?: rawTemp
    val minT = if (isCelsius) rawMin else (rawMin * 9 / 5 + 32)
    val maxT = if (isCelsius) rawMax else (rawMax * 9 / 5 + 32)

    GlassmorphicCard(
        modifier = Modifier.fillMaxWidth().testTag("hero_weather_card")
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Last updated: ${current.time.replace("T", " ")}",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.55f),
                fontSize = 11.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Temperature with Floating Emoji Layout
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.wrapContentSize()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "$roundedTemp°",
                        fontSize = 96.sp,
                        fontWeight = FontWeight.ExtraLight,
                        color = Color.White,
                        letterSpacing = (-4).sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = spec.iconEmoji,
                        fontSize = 52.sp,
                        modifier = Modifier.offset(y = (-24).dp)
                    )
                }
            }

            Text(
                text = spec.description,
                style = MaterialTheme.typography.titleLarge,
                fontSize = 22.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White,
                modifier = Modifier.padding(top = 2.dp)
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Subtitle High / Low line from Sleek Interface
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "H: ${Math.round(maxT)}°",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Normal,
                    color = Color.White.copy(alpha = 0.9f)
                )
                Text(
                    text = "L: ${Math.round(minT)}°",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Normal,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }
        }
    }
}

@Composable
fun RainAlertBanner(data: CombinedWeatherData, viewModel: WeatherViewModel) {
    val rainAlertEnabled by viewModel.rainAlertsEnabled.collectAsState()
    if (!rainAlertEnabled) return

    // Scan future daily precipitation probability
    val rainOdds = data.weather.daily?.precipitationProbabilityMax
        ?: emptyList()

    val maxPrecipitationChance = rainOdds.take(12).maxOrNull() ?: 0

    if (maxPrecipitationChance >= 30) {
        val infiniteTransition = rememberInfiniteTransition()
        val pulseIntensity by infiniteTransition.animateFloat(
            initialValue = 0.4f,
            targetValue = 0.95f,
            animationSpec = infiniteRepeatable(
                animation = tween(1500, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            )
        )

        Card(
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF1E293B).copy(alpha = 0.8f)
            ),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = pulseIntensity)),
            modifier = Modifier.fillMaxWidth().testTag("rain_alert_banner")
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "⛈️",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(end = 12.dp)
                )
                Column {
                    Text(
                        text = "Rain Alert Active",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF38BDF8)
                    )
                    Text(
                        text = "There is up to $maxPrecipitationChance% chance of showers in the next 12 hours. Carry an umbrella!",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }
            }
        }
    }
}

@Composable
fun WeatherDetailsGrid(data: CombinedWeatherData, viewModel: WeatherViewModel) {
    val current = data.weather.current ?: return
    val windUnitPref by viewModel.windUnit.collectAsState()

    // Conversions
    val rawWindSpeed = current.windSpeed10m
    val displayWindSpeed = when (windUnitPref) {
        "mph" -> rawWindSpeed * 0.621371
        "m/s" -> rawWindSpeed * 0.277778
        else -> rawWindSpeed // km/h
    }
    val roundedWind = String.format(Locale.US, "%.1f", displayWindSpeed)

    GlassmorphicCard(modifier = Modifier.fillMaxWidth().testTag("weather_details_card")) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Atmospheric Metrics",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                DetailItem(
                    icon = Icons.Outlined.WaterDrop,
                    label = "Humidity",
                    value = "${current.relativeHumidity2m}%",
                    modifier = Modifier.weight(1f)
                )
                DetailItem(
                    icon = Icons.Outlined.Air,
                    label = "Wind Speed",
                    value = "$roundedWind $windUnitPref",
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                DetailItem(
                    icon = Icons.Outlined.Speed,
                    label = "Pressure",
                    value = "${Math.round(current.pressureMsl ?: 1013.2)} hPa",
                    modifier = Modifier.weight(1f)
                )
                DetailItem(
                    icon = Icons.Outlined.Cloud,
                    label = "Cloud Cover",
                    value = "${Math.round(current.cloudCover ?: 0.0)}%",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun DetailItem(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = modifier.padding(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.65f)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

@Composable
fun SunProgressCard(data: CombinedWeatherData) {
    val daily = data.weather.daily ?: return
    val sunriseStr = daily.sunrise.firstOrNull() ?: ""
    val sunsetStr = daily.sunset.firstOrNull() ?: ""

    // Format strings (e.g., "2026-05-22T04:47")
    fun formatSunTime(timeStr: String): String {
        return try {
            val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.US)
            val formatter = SimpleDateFormat("h:mm a", Locale.US)
            val date = parser.parse(timeStr)
            if (date != null) formatter.format(date) else timeStr.split("T").lastOrNull() ?: timeStr
        } catch (e: Exception) {
            timeStr.split("T").lastOrNull() ?: timeStr
        }
    }

    val displaySunrise = formatSunTime(sunriseStr)
    val displaySunset = formatSunTime(sunsetStr)

    GlassmorphicCard(modifier = Modifier.fillMaxWidth().testTag("sun_progress_card")) {
        Text(
            text = "Sunrise & Sunset",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp),
            contentAlignment = Alignment.Center
        ) {
            // Draw a sunset trajectory arc
            Canvas(modifier = Modifier.fillMaxSize()) {
                val pathStroke = Stroke(
                    width = 3.dp.toPx(),
                    cap = StrokeCap.Round
                )
                // Draw dotted arc
                drawArc(
                    color = Color.White.copy(alpha = 0.3f),
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = false,
                    style = pathStroke
                )

                // Draw a dynamic sun traveling along the path based on local time
                val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                val fraction = ((hour - 6).toFloat() / 12f).coerceIn(0f, 1f)
                val currentAngleRad = Math.toRadians((180f + fraction * 180f).toDouble())

                val cx = size.width / 2f
                val cy = size.height
                val rx = size.width / 2f
                val ry = size.height

                val sunX = cx + rx * kotlin.math.cos(currentAngleRad).toFloat()
                val sunY = cy + ry * kotlin.math.sin(currentAngleRad).toFloat()

                drawCircle(
                    color = Color(0xFFFBBF24),
                    radius = 8.dp.toPx(),
                    center = Offset(sunX, sunY)
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(horizontalAlignment = Alignment.Start) {
                Text(
                    text = "Sunrise",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.65f)
                )
                Text(
                    text = displaySunrise,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "Sunset",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.65f)
                )
                Text(
                    text = displaySunset,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
fun UvGaugeCard(data: CombinedWeatherData, modifier: Modifier = Modifier) {
    val uvMax = data.weather.daily?.uvIndexMax?.firstOrNull() ?: 0.0
    val uvCategory = when {
        uvMax <= 2.0 -> "Low"
        uvMax <= 5.0 -> "Moderate"
        uvMax <= 7.0 -> "High"
        uvMax <= 10.0 -> "Very High"
        else -> "Extreme"
    }
    val uvColor = when {
        uvMax <= 2.0 -> Color(0xFF34D399) // green
        uvMax <= 5.0 -> Color(0xFFFBBF24) // yellow
        uvMax <= 7.0 -> Color(0xFFF97316) // orange
        uvMax <= 10.0 -> Color(0xFFEF4444) // red
        else -> Color(0xFFA855F7) // violet
    }

    GlassmorphicCard(modifier = modifier.height(160.dp).testTag("uv_gauge_card")) {
        Text(
            text = "UV Index",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = String.format(Locale.US, "%.1f", uvMax),
            fontSize = 32.sp,
            fontWeight = FontWeight.Black,
            color = Color.White
        )
        Text(
            text = uvCategory,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = uvColor
        )
        Spacer(modifier = Modifier.height(8.dp))

        // Linear Progress
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(3.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction = (uvMax / 12f).toFloat().coerceIn(0f, 1f))
                    .background(uvColor, RoundedCornerShape(3.dp))
            )
        }
    }
}

@Composable
fun AqiGaugeCard(data: CombinedWeatherData, modifier: Modifier = Modifier) {
    val aqiVal = data.aqi?.current?.usAqi ?: 35 // default if null or empty
    val (aqiDesc, aqiColor) = when {
        aqiVal <= 50 -> "Good" to Color(0xFF34D399)
        aqiVal <= 100 -> "Moderate" to Color(0xFFFBBF24)
        aqiVal <= 150 -> "Sensitive" to Color(0xFFF97316)
        else -> "Poor" to Color(0xFFEF4444)
    }

    GlassmorphicCard(modifier = modifier.height(160.dp).testTag("aqi_gauge_card")) {
        Text(
            text = "Air Quality",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = "$aqiVal AQI",
            fontSize = 32.sp,
            fontWeight = FontWeight.Black,
            color = Color.White
        )
        Text(
            text = aqiDesc,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = aqiColor
        )
        Spacer(modifier = Modifier.height(8.dp))

        // Circular sweep progress
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(3.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction = (aqiVal / 300f).toFloat().coerceIn(0f, 1f))
                    .background(aqiColor, RoundedCornerShape(3.dp))
            )
        }
    }
}

@Composable
fun RadarMapCard() {
    val infiniteTransition = rememberInfiniteTransition()
    val sweepAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )
    val radarPulse by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutLinearInEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    GlassmorphicCard(modifier = Modifier.fillMaxWidth().height(260.dp).testTag("radar_map_card")) {
        Text(
            text = "Interactive Radar (Animated)",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color(0xFF0F172A).copy(alpha = 0.7f), RoundedCornerShape(16.dp))
                .clip(RoundedCornerShape(16.dp))
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val fullRadius = size.minDimension / 2.2f

                // Draw radar grids
                drawCircle(
                    color = Color(0xFF10B981).copy(alpha = 0.15f),
                    radius = fullRadius,
                    style = Stroke(width = 1.dp.toPx())
                )
                drawCircle(
                    color = Color(0xFF10B981).copy(alpha = 0.15f),
                    radius = fullRadius * 0.66f,
                    style = Stroke(width = 1.dp.toPx())
                )
                drawCircle(
                    color = Color(0xFF10B981).copy(alpha = 0.15f),
                    radius = fullRadius * 0.33f,
                    style = Stroke(width = 1.dp.toPx())
                )

                // Grid lines (vertical and horizontal cross hairs)
                drawLine(
                    color = Color(0xFF10B981).copy(alpha = 0.12f),
                    start = Offset(center.x - fullRadius, center.y),
                    end = Offset(center.x + fullRadius, center.y),
                    strokeWidth = 1.dp.toPx()
                )
                drawLine(
                    color = Color(0xFF10B981).copy(alpha = 0.12f),
                    start = Offset(center.x, center.y - fullRadius),
                    end = Offset(center.x, center.y + fullRadius),
                    strokeWidth = 1.dp.toPx()
                )

                // Pulse propagation circle
                drawCircle(
                    color = Color(0xFF10B981).copy(alpha = 0.08f * (1f - radarPulse)),
                    radius = fullRadius * radarPulse
                )

                // Interactive-looking radar storms
                drawCircle(
                    color = Color(0xFF10B981).copy(alpha = 0.45f),
                    radius = 18.dp.toPx(),
                    center = Offset(center.x + fullRadius * 0.45f, center.y - fullRadius * 0.25f)
                )
                drawCircle(
                    color = Color(0xFF0EA5E9).copy(alpha = 0.4f),
                    radius = 24.dp.toPx(),
                    center = Offset(center.x - fullRadius * 0.4f, center.y + fullRadius * 0.1f)
                )
                drawCircle(
                    color = Color(0xFFEF4444).copy(alpha = 0.35f),
                    radius = 12.dp.toPx(),
                    center = Offset(center.x + fullRadius * 0.38f, center.y - fullRadius * 0.33f)
                )

                // Radar scanning sweep vector
                val rad = Math.toRadians(sweepAngle.toDouble())
                val sweepX = center.x + fullRadius * kotlin.math.cos(rad).toFloat()
                val sweepY = center.y + fullRadius * kotlin.math.sin(rad).toFloat()

                drawLine(
                    color = Color(0xFF10B981).copy(alpha = 0.7f),
                    start = center,
                    end = Offset(sweepX, sweepY),
                    strokeWidth = 2.dp.toPx()
                )
            }

            // Small glowing neon badge
            Box(
                modifier = Modifier
                    .padding(12.dp)
                    .background(Color(0xFF10B981).copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                    .border(1.dp, Color(0xFF10B981), RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
                    .align(Alignment.TopEnd)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(Color(0xFF10B981), CircleShape)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "LIVE RADAR",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF10B981)
                    )
                }
            }
        }
    }
}

@Composable
fun GlassmorphicCard(
    modifier: Modifier = Modifier,
    borderWidth: Dp = 1.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.2f)
        ),
        shape = RoundedCornerShape(28.dp),
        border = BorderStroke(
            borderWidth,
            Brush.verticalGradient(
                listOf(
                    Color.White.copy(alpha = 0.3f),
                    Color.White.copy(alpha = 0.1f)
                )
            )
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            content = content
        )
    }
}
