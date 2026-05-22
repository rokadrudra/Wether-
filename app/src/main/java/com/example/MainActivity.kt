package com.example

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.data.db.WeatherDatabase
import com.example.data.preferences.PreferencesManager
import com.example.data.repository.WeatherRepository
import com.example.ui.WeatherUiState
import com.example.ui.WeatherViewModel
import com.example.ui.WeatherViewModelFactory
import com.example.ui.screens.FavoritesScreen
import com.example.ui.screens.ForecastScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.WeatherThemeHelper
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Persistent context loaders
        val database = WeatherDatabase.getDatabase(applicationContext)
        val repository = WeatherRepository(database.favoriteCityDao())
        val preferencesManager = PreferencesManager(applicationContext)
        val viewModelFactory = WeatherViewModelFactory(repository, preferencesManager, applicationContext)

        setContent {
            MyApplicationTheme {
                val viewModel: WeatherViewModel = ViewModelProvider(this, viewModelFactory)[WeatherViewModel::class.java]
                MainAppLayout(viewModel = viewModel)
            }
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MainAppLayout(viewModel: WeatherViewModel) {
    val navController = rememberNavController()
    val uiState by viewModel.uiState.collectAsState()
    var currentRoute by remember { mutableStateOf("home") }

    // Read weather code for dynamic background gradients
    val currentSpec = remember(uiState) {
        when (val state = uiState) {
            is WeatherUiState.Success -> WeatherThemeHelper.getWeatherSpec(state.data.weather.current?.weatherCode ?: 0)
            else -> null
        }
    }

    // Default premium sky gradients if loading or error
    val gradientColors = currentSpec?.backgroundGradients ?: listOf(Color(0xFF0F172A), Color(0xFF1E293B), Color(0xFF334155))

    // Smooth color crossfade transition
    val animColor1 by animateColorAsState(targetValue = gradientColors[0], animationSpec = tween(1200))
    val animColor2 by animateColorAsState(targetValue = gradientColors.getOrNull(1) ?: gradientColors[0], animationSpec = tween(1200))
    val animColor3 by animateColorAsState(targetValue = gradientColors.getOrNull(2) ?: gradientColors[1], animationSpec = tween(1200))

    // Check location permissions gracefully on launch
    val locationPermissionState = rememberPermissionState(permission = Manifest.permission.ACCESS_COARSE_LOCATION)

    LaunchedEffect(locationPermissionState.status.isGranted) {
        if (locationPermissionState.status.isGranted) {
            viewModel.detectLocation()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        bottomBar = {
            GlassyBottomNavigationBar(
                currentRoute = currentRoute,
                onNavigate = { route ->
                    currentRoute = route
                    navController.navigate(route) {
                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    ) { innerPadding ->
        // Render dynamic background gradient canvas behind screens
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(animColor1, animColor2, animColor3)
                    )
                )
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(bottom = innerPadding.calculateBottomPadding())
        ) {
            // Display location permission helper banner in-place if missing
            if (!locationPermissionState.status.isGranted) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .align(Alignment.TopCenter)
                        .testTag("permission_banner")
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "📍 Live Location Off",
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Enable GPS coordinates to detect local weather features automatically.",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 11.sp
                            )
                        }
                        Button(
                            onClick = { locationPermissionState.launchPermissionRequest() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.2f)),
                            modifier = Modifier.testTag("grant_permission_button")
                        ) {
                            Text("Grant", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }

            NavHost(
                navController = navController,
                startDestination = "home",
                modifier = Modifier.fillMaxSize()
            ) {
                composable("home") {
                    HomeScreen(viewModel = viewModel)
                }
                composable("forecast") {
                    ForecastScreen(viewModel = viewModel)
                }
                composable("favorites") {
                    FavoritesScreen(
                        viewModel = viewModel,
                        onFavoriteSelected = {
                            currentRoute = "home"
                            navController.navigate("home") {
                                popUpTo(navController.graph.startDestinationId)
                                launchSingleTop = true
                            }
                        }
                    )
                }
                composable("settings") {
                    SettingsScreen(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun GlassyBottomNavigationBar(
    currentRoute: String,
    onNavigate: (String) -> Unit
) {
    // Custom Sleek Pill-style suspended floating bottom navigation
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 24.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(32.dp))
                .background(Color.Black.copy(alpha = 0.25f))
                .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(32.dp))
                .padding(vertical = 8.dp, horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val navItems = listOf(
                NavItemSpec("home", "Home", Icons.Filled.Home, Icons.Outlined.Home),
                NavItemSpec("forecast", "Forecast", Icons.Filled.BarChart, Icons.Outlined.BarChart),
                NavItemSpec("favorites", "Saved", Icons.Filled.Favorite, Icons.Outlined.FavoriteBorder),
                NavItemSpec("settings", "Settings", Icons.Filled.Settings, Icons.Outlined.Settings)
            )

            navItems.forEach { item ->
                val isSelected = currentRoute == item.route
                
                // Outer circle container for standard high-fidelity touch indicators
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) Color.White else Color.Transparent)
                        .clickable { onNavigate(item.route) }
                        .testTag("nav_tab_${item.route}"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                        contentDescription = item.label,
                        tint = if (isSelected) Color(0xFF4FACFE) else Color.White.copy(alpha = 0.65f),
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        }
    }
}

data class NavItemSpec(
    val route: String,
    val label: String,
    val selectedIcon: androidx.compose.ui.graphics.vector.ImageVector,
    val unselectedIcon: androidx.compose.ui.graphics.vector.ImageVector
)
