package com.example.data.repository

import com.example.data.api.OpenMeteoApiService
import com.example.data.api.GeminiWeatherService
import com.example.data.db.FavoriteCity
import com.example.data.db.FavoriteCityDao
import com.example.data.model.GeocodingResponse
import com.example.data.model.WeatherResponse
import kotlinx.coroutines.flow.Flow

class WeatherRepository(
    private val apiService: OpenMeteoApiService,
    private val favoriteCityDao: FavoriteCityDao
) {
    val favoriteCities: Flow<List<FavoriteCity>> = favoriteCityDao.getAllFavorites()

    suspend fun getForecast(latitude: Double, longitude: Double): WeatherResponse {
        return apiService.getForecast(latitude, longitude)
    }

    suspend fun searchCities(cityName: String): GeocodingResponse {
        return apiService.searchCities(cityName)
    }

    suspend fun addFavorite(city: FavoriteCity) {
        favoriteCityDao.insertFavorite(city)
    }

    suspend fun removeFavorite(city: FavoriteCity) {
        favoriteCityDao.deleteFavorite(city)
    }

    suspend fun removeFavoriteByName(name: String) {
        favoriteCityDao.deleteFavoriteByName(name)
    }

    suspend fun isFavorite(name: String): Boolean {
        return favoriteCityDao.isFavorite(name)
    }

    suspend fun getGeminiAdvice(
        city: String,
        temp: Double,
        apparentTemp: Double,
        humidity: Int,
        windSpeed: Double,
        description: String
    ): String {
        return GeminiWeatherService.getWeatherAdvice(city, temp, apparentTemp, humidity, windSpeed, description)
    }
}
