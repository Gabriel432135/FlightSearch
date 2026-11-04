package com.example.flightsearch.data

import kotlinx.coroutines.flow.Flow

interface FlightsRepository {
    fun getAllAirportsByNameStream(query: String): Flow<List<AirportEntity>>
    fun getAllDestinationsStream(departureCode: String): Flow<List<AirportEntity>>
    suspend fun getAirportsByIataCode(iataCode: String): AirportEntity?

    fun getAllFavorites(): Flow<List<FavoriteEntity>>
    suspend fun insertFavorite(favorite: FavoriteEntity)
    suspend fun deleteFavorite(favorite: FavoriteEntity)
}

class OfflineFlightsRepository(private val flightsDao: FlightsDao) : FlightsRepository {

    override fun getAllAirportsByNameStream(query: String): Flow<List<AirportEntity>> =
        flightsDao.searchAirports(query)

    override fun getAllDestinationsStream(departureCode: String): Flow<List<AirportEntity>> =
        flightsDao.getAllDestinations(departureCode)

    override suspend fun getAirportsByIataCode(iataCode: String): AirportEntity?  =
        flightsDao.getAirportByIataCode(iataCode)

    override fun getAllFavorites(): Flow<List<FavoriteEntity>> = flightsDao.getAllFavorites()

    override suspend fun insertFavorite(favorite: FavoriteEntity) = flightsDao.insertFavorite(favorite)

    override suspend fun deleteFavorite(favorite: FavoriteEntity) = flightsDao.deleteFavorite(favorite)

}