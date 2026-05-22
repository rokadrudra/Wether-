package com.example.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteCityDao {
    @Query("SELECT * FROM favorite_cities ORDER BY name ASC")
    fun getAllFavorites(): Flow<List<FavoriteCity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(city: FavoriteCity)

    @Delete
    suspend fun deleteFavorite(city: FavoriteCity)

    @Query("DELETE FROM favorite_cities WHERE id = :cityId")
    suspend fun deleteFavoriteById(cityId: Long)

    @Query("SELECT EXISTS(SELECT * FROM favorite_cities WHERE id = :cityId)")
    fun isFavorite(cityId: Long): Flow<Boolean>
}
