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

    // Estado local instantâneo da busca para a UI (corrige o pulo do cursor)
    private val _searchQuery = MutableStateFlow("")

    private val _selectedAirport = MutableStateFlow<AirportEntity?>(null)

    private var saveSearchJob: Job? = null

    init {
        // Carrega o termo salvo no DataStore ao iniciar o ViewModel
        viewModelScope.launch {
            val initialQuery = userPreferencesRepository.searchQuery.first()
            _searchQuery.value = initialQuery
        }
    }

    // Flow com Debounce de 300ms para evitar buscas no banco a cada letra digitada
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

    val uiState: StateFlow<HomeUiState> = combine(
        _searchQuery,
        _selectedAirport,
        airportsListFlow,
        flightRoutesFlow,
        favoritesFlow
    ) { query, selectedAirport, airports, destinations, favorites ->

        val airportsListModel = airports.map { it.toAirportModel() }
        val selectedAirportModel = selectedAirport?.toAirportModel()

        val routesModel = if (selectedAirport != null) {
            destinations.map { dest ->
                val isFav = favorites.any { fav ->
                    fav.departureCode == selectedAirport.iataCode && fav.destinationCode == dest.iataCode
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

        val favoriteRoutesModel = favorites.map { fav ->
            val depAirport = flightsRepository.getAirportsByIataCode(fav.departureCode)?.toAirportModel()
                ?: AirportModel(0, fav.departureCode, fav.departureCode)
            val destAirport = flightsRepository.getAirportsByIataCode(fav.destinationCode)?.toAirportModel()
                ?: AirportModel(0, fav.destinationCode, fav.destinationCode)

            FlightRouteModel(
                id = fav.id,
                departureAirport = depAirport,
                destinationAirport = destAirport,
                isFavorite = true
            )
        }

        HomeUiState(
            searchQuery = query,
            selectedAirport = selectedAirportModel,
            airportsList = airportsListModel,
            flightRoutes = routesModel,
            favoriteRoutes = favoriteRoutesModel
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = HomeUiState()
    )

    fun updateSearchQuery(query: String) {
        // 1. Atualiza o estado síncrono instantaneamente para o OutlinedTextField (sem pulo de cursor)
        _searchQuery.value = query
        if (query.isBlank()) {
            _selectedAirport.value = null
        }

        // 2. Debounce de 500ms para salvar no DataStore de forma assíncrona
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
            val favEntity = FavoriteEntity(
                departureCode = route.departureAirport.iataCode,
                destinationCode = route.destinationAirport.iataCode
            )
            if (route.isFavorite) {
                flightsRepository.deleteFavorite(favEntity)
            } else {
                flightsRepository.insertFavorite(favEntity)
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
