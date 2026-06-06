package com.example.ui.viewmodel

import android.annotation.SuppressLint
import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.api.RetrofitClient
import com.example.data.db.AppDatabase
import com.example.data.db.FavoriteCity
import com.example.data.model.GeocodingResult
import com.example.data.model.WeatherResponse
import com.example.data.repository.WeatherRepository
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface WeatherUiState {
    object Loading : WeatherUiState
    data class Success(
        val weather: WeatherResponse,
        val cityName: String,
        val country: String?,
        val isFavorite: Boolean,
        val geminiAdvice: String? = null,
        val isGeminiLoading: Boolean = false
    ) : WeatherUiState
    data class Error(val message: String) : WeatherUiState
}

class WeatherViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = "WeatherViewModel"
    private val repository: WeatherRepository

    init {
        val database = AppDatabase.getDatabase(application)
        repository = WeatherRepository(
            apiService = RetrofitClient.apiService,
            favoriteCityDao = database.favoriteCityDao()
        )
    }

    val favoriteCities: StateFlow<List<FavoriteCity>> = repository.favoriteCities
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _uiState = MutableStateFlow<WeatherUiState>(WeatherUiState.Loading)
    val uiState: StateFlow<WeatherUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<List<GeocodingResult>>(emptyList())
    val searchResults = _searchResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching = _isSearching.asStateFlow()

    private val fusedLocationClient by lazy {
        LocationServices.getFusedLocationProviderClient(application)
    }

    init {
        // Load default city (Cairo)
        fetchWeatherForLocation(30.0444, 31.2357, "القاهرة", "مصر")
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
        if (query.trim().length >= 2) {
            viewModelScope.launch {
                _isSearching.value = true
                try {
                    val response = repository.searchCities(query.trim())
                    _searchResults.value = response.results ?: emptyList()
                } catch (e: Exception) {
                    Log.e(TAG, "Search error: ${e.message}", e)
                    _searchResults.value = emptyList()
                } finally {
                    _isSearching.value = false
                }
            }
        } else {
            _searchResults.value = emptyList()
        }
    }

    fun clearSearch() {
        _searchQuery.value = ""
        _searchResults.value = emptyList()
    }

    fun fetchWeatherForLocation(latitude: Double, longitude: Double, cityName: String, country: String? = null) {
        viewModelScope.launch {
            _uiState.value = WeatherUiState.Loading
            clearSearch()
            try {
                val forecast = repository.getForecast(latitude, longitude)
                val isFav = repository.isFavorite(cityName)
                _uiState.value = WeatherUiState.Success(
                    weather = forecast,
                    cityName = cityName,
                    country = country,
                    isFavorite = isFav
                )
            } catch (e: Exception) {
                Log.e(TAG, "Forecast fetch error: ${e.message}", e)
                _uiState.value = WeatherUiState.Error("فشل في تحميل بيانات الطقس: ${e.localizedMessage ?: "عطل في الشبكة"}")
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun fetchWeatherFromGPS(onPermissionRequired: () -> Unit) {
        viewModelScope.launch {
            try {
                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                    if (location != null) {
                        fetchWeatherForLocation(
                            latitude = location.latitude,
                            longitude = location.longitude,
                            cityName = "الموقع الحالي",
                            country = "تحديد آلي"
                        )
                    } else {
                        _uiState.value = WeatherUiState.Error("لم نتمكن من تحديد موقعك الحالي. يرجى التأكد من تفعيل خدمة الـ GPS بالهاتف ثم تحديث الشاشة.")
                    }
                }.addOnFailureListener {
                    _uiState.value = WeatherUiState.Error("خطأ أثناء جلب موقع GPS: ${it.localizedMessage}")
                }
            } catch (e: SecurityException) {
                onPermissionRequired()
            }
        }
    }

    fun toggleFavorite() {
        val currentState = _uiState.value
        if (currentState is WeatherUiState.Success) {
            viewModelScope.launch {
                if (currentState.isFavorite) {
                    repository.removeFavoriteByName(currentState.cityName)
                    _uiState.value = currentState.copy(isFavorite = false)
                } else {
                    repository.addFavorite(
                        FavoriteCity(
                            name = currentState.cityName,
                            country = currentState.country ?: "",
                            latitude = currentState.weather.latitude,
                            longitude = currentState.weather.longitude,
                            countryCode = null
                        )
                    )
                    _uiState.value = currentState.copy(isFavorite = true)
                }
            }
        }
    }

    fun triggerGeminiAdvice() {
        val currentState = _uiState.value
        if (currentState is WeatherUiState.Success) {
            viewModelScope.launch {
                _uiState.value = currentState.copy(isGeminiLoading = true)
                try {
                    val currentDetails = currentState.weather.current
                    val temp = currentDetails?.temperature2m ?: 25.0
                    val apparentTemp = currentDetails?.apparentTemperature ?: 25.0
                    val humidity = currentDetails?.relativeHumidity2m ?: 60
                    val wind = currentDetails?.windSpeed10m ?: 10.0
                    val code = currentDetails?.weatherCode ?: 0
                    val desc = translateWeatherCode(code)

                    val advice = repository.getGeminiAdvice(
                        city = currentState.cityName,
                        temp = temp,
                        apparentTemp = apparentTemp,
                        humidity = humidity,
                        windSpeed = wind,
                        description = desc
                    )
                    val activeState = _uiState.value
                    if (activeState is WeatherUiState.Success && activeState.cityName == currentState.cityName) {
                        _uiState.value = activeState.copy(
                            geminiAdvice = advice,
                            isGeminiLoading = false
                        )
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Gemini load error: ${e.message}", e)
                    val activeState = _uiState.value
                    if (activeState is WeatherUiState.Success) {
                        _uiState.value = activeState.copy(
                            geminiAdvice = "عذراً، لم نتمكن من الحصول على نصيحة حية حالياً بسبب خطأ في الإعدادات أو انقطاع الاتصال.",
                            isGeminiLoading = false
                        )
                    }
                }
            }
        }
    }

    fun translateWeatherCode(code: Int): String {
        return when (code) {
            0 -> "سماء صافية تماماً ☀️"
            1, 2, 3 -> "غائم جزئياً 🌤️"
            45, 48 -> "ضباب كثيف 🌫️"
            51, 53, 55 -> "رذاذ مطر خفيف 🌦️"
            56, 57 -> "رذاذ متجمد ❄️"
            61, 63, 65 -> "مطر متوسط أو غزير 🌧️"
            66, 67 -> "أمطار متجمدة 🌨️"
            71, 73, 75 -> "ثلوج متساقطة ❄️"
            77 -> "ثلوج ناعمة متناثرة ❄️"
            80, 81, 82 -> "زخات مطر قوية ⛈️"
            85, 86 -> "زخات ثلج قوية ❄️"
            95 -> "عواصف رعدية خفيفة ⚡"
            96, 99 -> "عاصفة رعدية قوية مصحوبة بالبرد ⛈️"
            else -> "أجواء غامضة 🌥️"
        }
    }
}
