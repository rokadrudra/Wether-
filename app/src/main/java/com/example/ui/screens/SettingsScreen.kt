package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.WeatherViewModel

@Composable
fun SettingsScreen(
    viewModel: WeatherViewModel,
    modifier: Modifier = Modifier
) {
    val tempUnit by viewModel.tempUnit.collectAsState()
    val windUnit by viewModel.windUnit.collectAsState()
    val notificationsEnabled by viewModel.notificationsEnabled.collectAsState()
    val rainAlertsEnabled by viewModel.rainAlertsEnabled.collectAsState()

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState)
    ) {
        Text(
            text = "App Preferences",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            text = "Tune metrics and notification alerts",
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.7f)
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Card 1: Temperature Unit Selector
        SettingsGroupHeader(title = "METRICS & UNITS")
        
        MetricOptionCard(
            title = "Temperature Unit",
            subtitle = if (tempUnit == "C") "Celsius (°C)" else "Fahrenheit (°F)",
            icon = Icons.Outlined.DeviceThermostat,
            actions = {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White.copy(alpha = 0.1f))
                        .padding(2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (tempUnit == "C") Color.White.copy(alpha = 0.25f) else Color.Transparent)
                            .clickable { viewModel.setTempUnit("C") }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                            .testTag("temp_unit_c")
                    ) {
                        Text("°C", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (tempUnit == "F") Color.White.copy(alpha = 0.25f) else Color.Transparent)
                            .clickable { viewModel.setTempUnit("F") }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                            .testTag("temp_unit_f")
                    ) {
                        Text("°F", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Wind Speed Unit Option
        MetricOptionCard(
            title = "Wind Speed Unit",
            subtitle = "Currently showing in $windUnit",
            icon = Icons.Outlined.Air,
            actions = {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White.copy(alpha = 0.1f))
                        .padding(2.dp)
                ) {
                    listOf("km/h", "mph", "m/s").forEach { unit ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (windUnit == unit) Color.White.copy(alpha = 0.25f) else Color.Transparent)
                                .clickable { viewModel.setWindSpeedUnit(unit) }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                .testTag("wind_unit_$unit")
                        ) {
                            Text(unit, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Card 2: Alerts and Notifications
        SettingsGroupHeader(title = "ALERTS & NOTIFICATIONS")

        ToggleOptionCard(
            title = "Severe Weather Alert",
            subtitle = "Enable system-channel alert banners",
            icon = Icons.Outlined.NotificationsActive,
            checked = notificationsEnabled,
            onCheckedChange = { viewModel.setNotificationsEnabled(it) },
            testTag = "notification_toggle"
        )

        Spacer(modifier = Modifier.height(12.dp))

        ToggleOptionCard(
            title = "Precipitation (Rain) Alerts",
            subtitle = "Highlight active rain clouds predictions",
            icon = Icons.Outlined.WaterDrop,
            checked = rainAlertsEnabled,
            onCheckedChange = { viewModel.setRainAlertsEnabled(it) },
            testTag = "rain_alert_toggle"
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Credits / Technical Info Block
        GlassmorphicCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "⛅",
                    fontSize = 32.sp,
                    modifier = Modifier.padding(end = 16.dp)
                )
                Column {
                    Text(
                        text = "TrueCast Premium Weather",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "Data seamlessly synchronized in real-time utilizing the Open-Meteo Public API. Clean, responsive glassmorphism UI.",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(64.dp))
    }
}

@Composable
fun SettingsGroupHeader(title: String) {
    Text(
        text = title,
        fontWeight = FontWeight.ExtraBold,
        color = Color.White.copy(alpha = 0.5f),
        fontSize = 11.sp,
        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
    )
}

@Composable
fun MetricOptionCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    actions: @Composable () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.12f))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = title, tint = Color.White)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(title, fontWeight = FontWeight.Bold, color = Color.White)
                Text(subtitle, color = Color.White.copy(alpha = 0.65f), fontSize = 12.sp)
            }
        }
        actions()
    }
}

@Composable
fun ToggleOptionCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.12f))
            .clickable { onCheckedChange(!checked) }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = title, tint = Color.White)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(title, fontWeight = FontWeight.Bold, color = Color.White)
                Text(subtitle, color = Color.White.copy(alpha = 0.65f), fontSize = 12.sp)
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = { onCheckedChange(it) },
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF38BDF8),
                uncheckedThumbColor = Color.LightGray,
                uncheckedTrackColor = Color.White.copy(alpha = 0.2f),
                uncheckedBorderColor = Color.Transparent
            ),
            modifier = Modifier.testTag(testTag)
        )
    }
}
