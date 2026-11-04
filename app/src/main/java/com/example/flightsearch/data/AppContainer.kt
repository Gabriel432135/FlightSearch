package com.example.flightsearch.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.dataStore
import androidx.datastore.preferences.core.Preferences

interface AppContainer{
    val userPreferencesRepository: UserPreferencesRepository
    val flightsRepository: FlightsRepository
}

class DefaultAppContainer(private val context: Context, private val dataStore: DataStore<Preferences>) : AppContainer{
    override val userPreferencesRepository: UserPreferencesRepository by lazy {
        UserPreferencesRepository(dataStore)
    }
    override val flightsRepository: FlightsRepository by lazy {
        OfflineFlightsRepository(FlightSearchDatabase.getDatabase(context).flightsDao())
    }
}