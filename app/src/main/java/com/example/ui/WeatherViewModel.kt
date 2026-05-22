package com.example.ui

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.api.GeocodingResult
import com.example.data.db.FavoriteCity
import com.example.data.preferences.PreferencesManager
import com.example.data.repository.CombinedWeatherData
import com.example.data.repository.WeatherRepository
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.Locale

sealed interface WeatherUiState {
    object Loading : WeatherUiState
    data class Success(val data: CombinedWeatherData, val cityName: String) : WeatherUiState
    data class Error(val message: String) : WeatherUiState
}

class WeatherViewModel(
    private val repository: WeatherRepository,
    val preferencesManager: PreferencesManager,
    private val context: Context
) : ViewModel() {

    // Selected location
    private val _currentLatitude = MutableStateFlow(35.6762) // Default Tokyo
    private val _currentLongitude = MutableStateFlow(139.6503)
    private val _currentLocationName = MutableStateFlow("Tokyo")

    val currentLatitude: StateFlow<Double> = _currentLatitude.asStateFlow()
    val currentLongitude: StateFlow<Double> = _currentLongitude.asStateFlow()
    val currentLocationName: StateFlow<String> = _currentLocationName.asStateFlow()

    // Main weather state
    private val _uiState = MutableStateFlow<WeatherUiState>(WeatherUiState.Loading)
    val uiState: StateFlow<WeatherUiState> = _uiState.asStateFlow()

    // Search query & results
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<List<GeocodingResult>>(emptyList())
    val searchResults: StateFlow<List<GeocodingResult>> = _searchResults.asStateFlow()

    private val _searchLoading = MutableStateFlow(false)
    val searchLoading: StateFlow<Boolean> = _searchLoading.asStateFlow()

    // Favorites
    val favoriteCities: StateFlow<List<FavoriteCity>> = repository.getFavorites()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Unit settings preferences (flows directly from PreferencesManager)
    val tempUnit = preferencesManager.tempUnit
    val windUnit = preferencesManager.windUnit
    val notificationsEnabled = preferencesManager.notificationsEnabled
    val rainAlertsEnabled = preferencesManager.rainAlertsEnabled

    private val exceptionHandler = CoroutineExceptionHandler { _, exception ->
        val errorMsg = exception.localizedMessage ?: "No Internet Connection. Please check your network and retry."
        _uiState.value = WeatherUiState.Error(errorMsg)
    }

    init {
        // Load initial weather for Tokyo
        loadWeather(_currentLatitude.value, _currentLongitude.value, _currentLocationName.value)
    }

    fun loadWeather(lat: Double, lon: Double, name: String) {
        viewModelScope.launch(exceptionHandler) {
            _uiState.value = WeatherUiState.Loading
            _currentLatitude.value = lat
            _currentLongitude.value = lon
            _currentLocationName.value = name

            repository.getWeatherData(lat, lon)
                .onSuccess { data ->
                    _uiState.value = WeatherUiState.Success(data, name)
                }
                .onFailure { exception ->
                    val errorMsg = exception.localizedMessage ?: "No Internet Connection. Please check your network and retry."
                    _uiState.value = WeatherUiState.Error(errorMsg)
                }
        }
    }

    fun refreshWeather() {
        loadWeather(_currentLatitude.value, _currentLongitude.value, _currentLocationName.value)
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        if (query.length < 2) {
            _searchResults.value = emptyList()
            return
        }

        viewModelScope.launch(exceptionHandler) {
            _searchLoading.value = true
            repository.searchCities(query)
                .onSuccess { response ->
                    _searchResults.value = response.results ?: emptyList()
                    _searchLoading.value = false
                }
                .onFailure {
                    _searchResults.value = emptyList()
                    _searchLoading.value = false
                }
        }
    }

    fun selectCity(city: GeocodingResult) {
        val nameWithRegion = buildString {
            append(city.name)
            if (!city.admin1.isNullOrEmpty()) append(", ${city.admin1}")
            else if (!city.country.isNullOrEmpty()) append(", ${city.country}")
        }
        loadWeather(city.latitude, city.longitude, nameWithRegion)
        _searchQuery.value = ""
        _searchResults.value = emptyList()
    }

    // Toggle favorites
    fun toggleFavorite(cityId: Long, name: String, country: String?, state: String?, lat: Double, lon: Double, isFav: Boolean) {
        viewModelScope.launch {
            if (isFav) {
                repository.removeFavoriteById(cityId)
            } else {
                repository.addFavorite(
                    FavoriteCity(
                        id = cityId,
                        name = name,
                        country = country,
                        state = state,
                        latitude = lat,
                        longitude = lon
                    )
                )
            }
        }
    }

    fun isFavoriteCity(cityId: Long): Flow<Boolean> {
        return repository.isFavorite(cityId)
    }

    // Settings adjustments
    fun setTempUnit(unit: String) {
        preferencesManager.setTempUnit(unit)
    }

    fun setWindSpeedUnit(unit: String) {
        preferencesManager.setWindUnit(unit)
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        preferencesManager.setNotificationsEnabled(enabled)
    }

    fun setRainAlertsEnabled(enabled: Boolean) {
        preferencesManager.setRainAlertsEnabled(enabled)
    }

    // Auto location detection
    @SuppressLint("MissingPermission")
    fun detectLocation() {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        _uiState.value = WeatherUiState.Loading
        try {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location: Location? ->
                    viewModelScope.launch {
                        if (location != null) {
                            val lat = location.latitude
                            val lon = location.longitude
                            var cityName = "Current Location"

                            // Try geocoding city name on Dispatchers.IO
                            try {
                                val resolvedCityName = withContext(Dispatchers.IO) {
                                    val geocoder = Geocoder(context, Locale.getDefault())
                                    val addresses = geocoder.getFromLocation(lat, lon, 1)
                                    if (!addresses.isNullOrEmpty()) {
                                        val address = addresses[0]
                                        address.locality ?: address.subAdminArea ?: address.adminArea ?: "My Location"
                                    } else {
                                        null
                                    }
                                }
                                if (resolvedCityName != null) {
                                    cityName = resolvedCityName
                                }
                            } catch (e: Exception) {
                                // Geocoder may fail without network - ignore and fallback
                            }

                            loadWeather(lat, lon, cityName)
                        } else {
                            // Location is null, try defaults but show toast / state notice
                            loadWeather(40.7128, -74.0060, "New York") // Fallback
                        }
                    }
                }
                .addOnFailureListener {
                    // Location client failed
                    loadWeather(40.7128, -74.0060, "New York") // Fallback
                }
        } catch (e: SecurityException) {
            _uiState.value = WeatherUiState.Error("Location permission required to detect automatically.")
        }
    }
}

class WeatherViewModelFactory(
    private val repository: WeatherRepository,
    private val preferencesManager: PreferencesManager,
    private val context: Context
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WeatherViewModel::class.java)) {
            return WeatherViewModel(repository, preferencesManager, context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
