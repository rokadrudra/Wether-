package com.example.data.repository

import com.example.data.api.AirQualityResponse
import com.example.data.api.GeocodingResponse
import com.example.data.api.WeatherApiClient
import com.example.data.api.WeatherResponse
import com.example.data.db.FavoriteCity
import com.example.data.db.FavoriteCityDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext

data class CombinedWeatherData(
    val weather: WeatherResponse,
    val aqi: AirQualityResponse?
)

class WeatherRepository(private val favoriteCityDao: FavoriteCityDao) {

    suspend fun searchCities(query: String): Result<GeocodingResponse> = withContext(Dispatchers.IO) {
        try {
            val response = WeatherApiClient.geocodingService.searchCity(query)
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getWeatherData(latitude: Double, longitude: Double): Result<CombinedWeatherData> = withContext(Dispatchers.IO) {
        try {
            val weather = WeatherApiClient.forecastService.getForecast(latitude, longitude)
            val aqi = try {
                WeatherApiClient.airQualityService.getAirQuality(latitude, longitude)
            } catch (e: Exception) {
                null // If AQI fails, don't crash main weather display
            }
            Result.success(CombinedWeatherData(weather, aqi))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Database Actions
    fun getFavorites(): Flow<List<FavoriteCity>> {
        return favoriteCityDao.getAllFavorites()
    }

    suspend fun addFavorite(city: FavoriteCity) = withContext(Dispatchers.IO) {
        favoriteCityDao.insertFavorite(city)
    }

    suspend fun removeFavorite(city: FavoriteCity) = withContext(Dispatchers.IO) {
        favoriteCityDao.deleteFavorite(city)
    }

    suspend fun removeFavoriteById(cityId: Long) = withContext(Dispatchers.IO) {
        favoriteCityDao.deleteFavoriteById(cityId)
    }

    fun isFavorite(cityId: Long): Flow<Boolean> {
        return favoriteCityDao.isFavorite(cityId)
    }
}
