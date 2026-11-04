package com.example.flightsearch.data

import kotlinx.coroutines.flow.Flow

interface FlightsRepository {
    fun getAllAirportsByNameStream(query: String): Flow<List<AirportEntity>>
    fun getAllAirports(): Flow<List<AirportEntity>>
    fun getAllDestinationsStream(departureCode: String): Flow<List<AirportEntity>>
    suspend fun getAirportsByIataCode(iataCode: String): AirportEntity?

    fun getAllFavorites(): Flow<List<FavoriteEntity>>
    suspend fun insertFavorite(favorite: FavoriteEntity)
    suspend fun deleteFavoriteByCodes(departureCode: String, destinationCode: String)
}

class OfflineFlightsRepository(private val flightsDao: FlightsDao) : FlightsRepository {

    override fun getAllAirportsByNameStream(query: String): Flow<List<AirportEntity>> =
        flightsDao.searchAirports(query)

    override fun getAllAirports(): Flow<List<AirportEntity>> =
        flightsDao.getAllAirports()

    override fun getAllDestinationsStream(departureCode: String): Flow<List<AirportEntity>> =
        flightsDao.getAllDestinations(departureCode)

    override suspend fun getAirportsByIataCode(iataCode: String): AirportEntity?  =
        flightsDao.getAirportByIataCode(iataCode)

    override fun getAllFavorites(): Flow<List<FavoriteEntity>> = flightsDao.getAllFavorites()

    override suspend fun insertFavorite(favorite: FavoriteEntity) = flightsDao.insertFavorite(favorite)

    override suspend fun deleteFavoriteByCodes(departureCode: String, destinationCode: String) =
        flightsDao.deleteFavoriteByCodes(departureCode, destinationCode)
}
