package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class WeatherResponse(
    val latitude: Double,
    val longitude: Double,
    @Json(name = "generationtime_ms") val generationTimeMs: Double,
    @Json(name = "utc_offset_seconds") val utcOffsetSeconds: Int,
    val timezone: String,
    @Json(name = "timezone_abbreviation") val timezoneAbbreviation: String,
    val elevation: Double,
    @Json(name = "current_units") val currentUnits: Map<String, String>?,
    val current: CurrentWeather?,
    @Json(name = "hourly_units") val hourlyUnits: Map<String, String>?,
    val hourly: HourlyWeather?,
    @Json(name = "daily_units") val dailyUnits: Map<String, String>?,
    val daily: DailyWeather?
)

@JsonClass(generateAdapter = true)
data class CurrentWeather(
    val time: String,
    val interval: Int?,
    @Json(name = "temperature_2m") val temperature2m: Double,
    @Json(name = "relative_humidity_2m") val relativeHumidity2m: Int,
    @Json(name = "apparent_temperature") val apparentTemperature: Double,
    val precipitation: Double,
    val rain: Double?,
    val showers: Double?,
    val snowfall: Double?,
    @Json(name = "weather_code") val weatherCode: Int,
    @Json(name = "cloud_cover") val cloudCover: Int,
    @Json(name = "pressure_msl") val pressureMsl: Double,
    @Json(name = "wind_speed_10m") val windSpeed10m: Double
)

@JsonClass(generateAdapter = true)
data class HourlyWeather(
    val time: List<String>,
    @Json(name = "temperature_2m") val temperature2m: List<Double>,
    @Json(name = "relative_humidity_2m") val relativeHumidity2m: List<Int>,
    @Json(name = "weather_code") val weatherCode: List<Int>
)

@JsonClass(generateAdapter = true)
data class DailyWeather(
    val time: List<String>,
    @Json(name = "weather_code") val weatherCode: List<Int>,
    @Json(name = "temperature_2m_max") val temperature2mMax: List<Double>,
    @Json(name = "temperature_2m_min") val temperature2mMin: List<Double>,
    val sunrise: List<String>,
    val sunset: List<String>
)

@JsonClass(generateAdapter = true)
data class GeocodingResponse(
    val results: List<GeocodingResult>?
)

@JsonClass(generateAdapter = true)
data class GeocodingResult(
    val id: Long,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val elevation: Double?,
    val country: String?,
    @Json(name = "country_code") val countryCode: String?,
    val admin1: String?
)
