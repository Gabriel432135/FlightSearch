package com.example.flightsearch

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Preview
import com.example.flightsearch.ui.theme.AirportModel
import com.example.flightsearch.ui.theme.FlightRouteModel
import com.example.flightsearch.ui.theme.FlightSearchTheme
import com.example.flightsearch.ui.theme.HomeScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FlightSearchTheme {
                FlightSearchAppDemo()
            }
        }
    }
}

// Dados falsos de exemplo para testar a UI interativa enquanto a camada de dados não é conectada
private val mockAirports = listOf(
    AirportModel(1, "SFO", "San Francisco International Airport", 15000),
    AirportModel(2, "JFK", "John F. Kennedy International Airport", 20000),
    AirportModel(3, "GRU", "São Paulo/Guarulhos International Airport", 18000),
    AirportModel(4, "GIG", "Rio de Janeiro/Galeão International Airport", 12000)
)

private val mockRoutesList = listOf(
    FlightRouteModel(1, mockAirports[0], mockAirports[1], isFavorite = true),
    FlightRouteModel(2, mockAirports[0], mockAirports[2], isFavorite = false),
    FlightRouteModel(3, mockAirports[0], mockAirports[3], isFavorite = false)
)

@Composable
fun FlightSearchAppDemo() {
    var query by remember { mutableStateOf("") }
    var selectedAirport by remember { mutableStateOf<AirportModel?>(null) }
    var routes by remember { mutableStateOf(mockRoutesList) }

    HomeScreen(
        searchQuery = query,
        onSearchQueryChange = { newQuery ->
            query = newQuery
            if (newQuery.isEmpty()) {
                selectedAirport = null
            }
        },
        selectedAirport = selectedAirport,
        onAirportSelected = { airport ->
            selectedAirport = airport
        },
        airportsList = mockAirports.filter {
            it.iataCode.contains(query, ignoreCase = true) ||
                    it.name.contains(query, ignoreCase = true)
        },
        flightRoutes = routes,
        favoriteRoutes = routes.filter { it.isFavorite },
        onFavoriteClick = { clickedRoute ->
            routes = routes.map { route ->
                if (route.id == clickedRoute.id) route.copy(isFavorite = !route.isFavorite) else route
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
fun FlightSearchAppDemoPreview() {
    FlightSearchTheme {
        FlightSearchAppDemo()
    }
}
