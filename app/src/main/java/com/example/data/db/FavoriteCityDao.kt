package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteCityDao {
    @Query("SELECT * FROM favorite_cities ORDER BY addedAt DESC")
    fun getAllFavorites(): Flow<List<FavoriteCity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(city: FavoriteCity)

    @Delete
    suspend fun deleteFavorite(city: FavoriteCity)

    @Query("DELETE FROM favorite_cities WHERE name = :name")
    suspend fun deleteFavoriteByName(name: String)

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_cities WHERE name = :name LIMIT 1)")
    suspend fun isFavorite(name: String): Boolean
}
