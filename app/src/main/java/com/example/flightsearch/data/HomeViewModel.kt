package com.example.flightsearch.data

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.flightsearch.FlightSearchApplication
import com.example.flightsearch.ui.theme.AirportModel
import com.example.flightsearch.ui.theme.FlightRouteModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

class HomeViewModel(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val flightsRepository: FlightsRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")

    private val _selectedAirport = MutableStateFlow<AirportEntity?>(null)

    private var saveSearchJob: Job? = null

    init {
        viewModelScope.launch {
            val initialQuery = userPreferencesRepository.searchQuery.first()
            _searchQuery.value = initialQuery
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    private val airportsListFlow: Flow<List<AirportEntity>> = _searchQuery
        .debounce(300L.milliseconds)
        .flatMapLatest { query ->
            if (query.isBlank()) {
                flowOf(emptyList())
            } else {
                flightsRepository.getAllAirportsByNameStream(query)
            }
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val flightRoutesFlow: Flow<List<AirportEntity>> = _selectedAirport.flatMapLatest { airport ->
        if (airport == null) {
            flowOf(emptyList())
        } else {
            flightsRepository.getAllDestinationsStream(airport.iataCode)
        }
    }

    private val favoritesFlow: Flow<List<FavoriteEntity>> = flightsRepository.getAllFavorites()
    private val allAirportsFlow: Flow<List<AirportEntity>> = flightsRepository.getAllAirports()

    private val favoriteRoutesFlow: Flow<List<FlightRouteModel>> = combine(
        favoritesFlow,
        allAirportsFlow
    ) { favorites, allAirports ->
        val allAirportsMap = allAirports.associateBy { it.iataCode }
        favorites.map { fav ->
            val depAirport = allAirportsMap[fav.departureCode]?.toAirportModel()
                ?: AirportModel(0, fav.departureCode, fav.departureCode)
            val destAirport = allAirportsMap[fav.destinationCode]?.toAirportModel()
                ?: AirportModel(0, fav.destinationCode, fav.destinationCode)

            FlightRouteModel(
                id = fav.id,
                departureAirport = depAirport,
                destinationAirport = destAirport,
                isFavorite = true
            )
        }
    }

    val uiState: StateFlow<HomeUiState> = combine(
        _searchQuery,
        _selectedAirport,
        airportsListFlow,
        flightRoutesFlow,
        favoriteRoutesFlow
    ) { query, selectedAirport, airports, destinations, favoriteRoutes ->

        val airportsListModel = airports.map { it.toAirportModel() }
        val selectedAirportModel = selectedAirport?.toAirportModel()

        val routesModel = if (selectedAirport != null) {
            destinations.map { dest ->
                val isFav = favoriteRoutes.any { fav ->
                    fav.departureAirport.iataCode == selectedAirport.iataCode &&
                            fav.destinationAirport.iataCode == dest.iataCode
                }
                FlightRouteModel(
                    id = dest.id,
                    departureAirport = selectedAirportModel!!,
                    destinationAirport = dest.toAirportModel(),
                    isFavorite = isFav
                )
            }
        } else {
            emptyList()
        }

        HomeUiState(
            searchQuery = query,
            selectedAirport = selectedAirportModel,
            airportsList = airportsListModel,
            flightRoutes = routesModel,
            favoriteRoutes = favoriteRoutes
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = HomeUiState()
    )

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        if (query.isBlank()) {
            _selectedAirport.value = null
        }

        saveSearchJob?.cancel()
        saveSearchJob = viewModelScope.launch {
            delay(500L.milliseconds)
            userPreferencesRepository.saveSearchQuery(query)
        }
    }

    fun selectAirport(airportModel: AirportModel) {
        viewModelScope.launch {
            val entity = flightsRepository.getAirportsByIataCode(airportModel.iataCode)
            _selectedAirport.value = entity
        }
    }

    fun toggleFavorite(route: FlightRouteModel) {
        viewModelScope.launch {
            if (route.isFavorite) {
                flightsRepository.deleteFavoriteByCodes(
                    departureCode = route.departureAirport.iataCode,
                    destinationCode = route.destinationAirport.iataCode
                )
            } else {
                flightsRepository.insertFavorite(
                    FavoriteEntity(
                        departureCode = route.departureAirport.iataCode,
                        destinationCode = route.destinationAirport.iataCode
                    )
                )
            }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as FlightSearchApplication)
                HomeViewModel(
                    userPreferencesRepository = application.appContainer.userPreferencesRepository,
                    flightsRepository = application.appContainer.flightsRepository
                )
            }
        }
    }
}

data class HomeUiState(
    val searchQuery: String = "",
    val selectedAirport: AirportModel? = null,
    val airportsList: List<AirportModel> = emptyList(),
    val flightRoutes: List<FlightRouteModel> = emptyList(),
    val favoriteRoutes: List<FlightRouteModel> = emptyList()
)

fun AirportEntity.toAirportModel(): AirportModel = AirportModel(
    id = id,
    iataCode = iataCode,
    name = name,
    passengers = passengers
)
