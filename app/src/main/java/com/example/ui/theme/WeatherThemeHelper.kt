package com.example.ui.theme

import androidx.compose.ui.graphics.Color

data class WeatherStatusSpec(
    val description: String,
    val iconEmoji: String,
    val backgroundGradients: List<Color>,
    val accentColor: Color,
    val isDarkBackground: Boolean
)

object WeatherThemeHelper {
    // Elegant theme configurations for different weather states under Sleek Interface
    private val ClearSky = WeatherStatusSpec(
        description = "Clear Sky",
        iconEmoji = "☀️",
        backgroundGradients = listOf(Color(0xFF4FACFE), Color(0xFF00F2FE)),
        accentColor = Color(0xFF00F2FE),
        isDarkBackground = true
    )

    private val CloudHand = WeatherStatusSpec(
        description = "Partly Cloudy",
        iconEmoji = "⛅",
        backgroundGradients = listOf(Color(0xFF2E7BFF), Color(0xFF0DF2FE)),
        accentColor = Color(0xFFFFD43F),
        isDarkBackground = true
    )

    private val Overcast = WeatherStatusSpec(
        description = "Overcast",
        iconEmoji = "☁️",
        backgroundGradients = listOf(Color(0xFF2C5E8A), Color(0xFF7CB9E8)),
        accentColor = Color(0xFFCBD5E1),
        isDarkBackground = true
    )

    private val Foggy = WeatherStatusSpec(
        description = "Foggy",
        iconEmoji = "🌫️",
        backgroundGradients = listOf(Color(0xFF3B7294), Color(0xFF88C0D0)),
        accentColor = Color(0xFF94A3B8),
        isDarkBackground = true
    )

    private val Drizzle = WeatherStatusSpec(
        description = "Drizzle",
        iconEmoji = "🌧️",
        backgroundGradients = listOf(Color(0xFF1E5BB5), Color(0xFF4FACFE)),
        accentColor = Color(0xFF7DD3FC),
        isDarkBackground = true
    )

    private val Rainy = WeatherStatusSpec(
        description = "Heavy Rain",
        iconEmoji = "☔",
        backgroundGradients = listOf(Color(0xFF15337E), Color(0xFF2E89F2)),
        accentColor = Color(0xFF38BDF8),
        isDarkBackground = true
    )

    private val Snowy = WeatherStatusSpec(
        description = "Snowy",
        iconEmoji = "❄️",
        backgroundGradients = listOf(Color(0xFF5A9FFF), Color(0xFFB1E1FF)),
        accentColor = Color(0xFFE2E8F0),
        isDarkBackground = true
    )

    private val Stormy = WeatherStatusSpec(
        description = "Thunderstorm",
        iconEmoji = "⚡",
        backgroundGradients = listOf(Color(0xFF0F1A3C), Color(0xFF3866F2)),
        accentColor = Color(0xFFFBBF24),
        isDarkBackground = true
    )

    fun getWeatherSpec(weatherCode: Int): WeatherStatusSpec {
        return when (weatherCode) {
            0 -> ClearSky
            1, 2 -> CloudHand
            3 -> Overcast
            45, 48 -> Foggy
            51, 53, 55, 56, 57 -> Drizzle
            61, 63, 65, 66, 67 -> Rainy
            71, 73, 75, 77 -> Snowy
            80, 81, 82 -> Rainy
            85, 86 -> Snowy
            95, 96, 99 -> Stormy
            else -> ClearSky
        }
    }
}
