package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorite_cities")
data class FavoriteCity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val country: String,
    val latitude: Double,
    val longitude: Double,
    val countryCode: String?,
    val addedAt: Long = System.currentTimeMillis()
)
