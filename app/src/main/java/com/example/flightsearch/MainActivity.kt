package com.example.flightsearch

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.flightsearch.data.HomeViewModel
import com.example.flightsearch.ui.theme.FlightSearchTheme
import com.example.flightsearch.ui.theme.HomeScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FlightSearchTheme {
                FlightSearchApp()
            }
        }
    }
}

@Composable
fun FlightSearchApp(
    viewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsState()

    HomeScreen(
        searchQuery = uiState.searchQuery,
        onSearchQueryChange = { query ->
            viewModel.updateSearchQuery(query)
        },
        selectedAirport = uiState.selectedAirport,
        onAirportSelected = { airport ->
            viewModel.selectAirport(airport)
        },
        airportsList = uiState.airportsList,
        flightRoutes = uiState.flightRoutes,
        favoriteRoutes = uiState.favoriteRoutes,
        onFavoriteClick = { route ->
            viewModel.toggleFavorite(route)
        }
    )
}
