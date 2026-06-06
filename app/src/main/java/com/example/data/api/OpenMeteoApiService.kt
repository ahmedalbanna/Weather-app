package com.example.data.api

import com.example.data.model.GeocodingResponse
import com.example.data.model.WeatherResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface OpenMeteoApiService {
    @GET("https://api.open-meteo.com/v1/forecast")
    suspend fun getForecast(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("current") current: String = "temperature_2m,relative_humidity_2m,apparent_temperature,precipitation,rain,showers,snowfall,weather_code,cloud_cover,pressure_msl,wind_speed_10m",
        @Query("hourly") hourly: String = "temperature_2m,relative_humidity_2m,weather_code",
        @Query("daily") daily: String = "weather_code,temperature_2m_max,temperature_2m_min,sunrise,sunset",
        @Query("timezone") timezone: String = "auto"
    ): WeatherResponse

    @GET("https://geocoding-api.open-meteo.com/v1/search")
    suspend fun searchCities(
        @Query("name") cityName: String,
        @Query("count") count: Int = 10,
        @Query("language") language: String = "ar"
    ): GeocodingResponse
}
