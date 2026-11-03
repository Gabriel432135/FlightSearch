package com.example.flightsearch.ui.theme

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp

// --- Modelos de Dados Mockados (para pré-visualização e testes da UI) ---
data class AirportModel(
    val id: Int,
    val iataCode: String,
    val name: String,
    val passengers: Int = 0
)

data class FlightRouteModel(
    val id: Int,
    val departureAirport: AirportModel,
    val destinationAirport: AirportModel,
    val isFavorite: Boolean = false
)

// --- Composable Principal da Tela ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    searchQuery: String = "",
    onSearchQueryChange: (String) -> Unit = {},
    selectedAirport: AirportModel? = null,
    onAirportSelected: (AirportModel) -> Unit = {},
    airportsList: List<AirportModel> = emptyList(),
    flightRoutes: List<FlightRouteModel> = emptyList(),
    favoriteRoutes: List<FlightRouteModel> = emptyList(),
    onFavoriteClick: (FlightRouteModel) -> Unit = {}
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Consulta de Voos",
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(AppTheme.dimensions.paddingMedium)
        ) {
            // Campo de busca
            FlightSearchBar(
                query = searchQuery,
                onQueryChange = onSearchQueryChange,
                onClearQuery = {
                    onSearchQueryChange("")
                }
            )

            Spacer(modifier = Modifier.height(AppTheme.dimensions.paddingMedium))

            // Conteúdo dinâmico com base no estado da busca
            when {
                // Estado 1: Usuário digitando -> mostra lista de sugestões de aeroportos
                searchQuery.isNotBlank() && selectedAirport == null -> {
                    Text(
                        text = "Aeroportos Encontrados",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = AppTheme.dimensions.paddingSmall)
                    )
                    AirportSearchResultList(
                        airports = airportsList,
                        onAirportClick = onAirportSelected
                    )
                }

                // Estado 2: Aeroporto selecionado -> mostra lista de voos saindo daquele aeroporto
                selectedAirport != null -> {
                    Text(
                        text = "Voos saindo de ${selectedAirport.iataCode}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = AppTheme.dimensions.paddingSmall)
                    )
                    FlightRoutesList(
                        routes = flightRoutes,
                        onFavoriteClick = onFavoriteClick
                    )
                }

                // Estado 3: Busca vazia -> mostra lista de rotas favoritas salvas
                else -> {
                    Text(
                        text = "Rotas Favoritas",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = AppTheme.dimensions.paddingSmall)
                    )
                    if (favoriteRoutes.isEmpty()) {
                        EmptyFavoritesMessage()
                    } else {
                        FlightRoutesList(
                            routes = favoriteRoutes,
                            onFavoriteClick = onFavoriteClick
                        )
                    }
                }
            }
        }
    }
}

// --- Campo de Busca ---
@Composable
fun FlightSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClearQuery: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text("Digite o código IATA ou nome do aeroporto") },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Buscar"
            )
        },
        trailingIcon = {
            AnimatedVisibility(visible = query.isNotEmpty()) {
                IconButton(onClick = onClearQuery) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Limpar busca"
                    )
                }
            }
        },
        singleLine = true,
        shape = AppTheme.shape.extraLarge,
        modifier = modifier.fillMaxWidth()
    )
}

// --- Lista de Resultados da Busca de Aeroportos ---
@Composable
fun AirportSearchResultList(
    airports: List<AirportModel>,
    onAirportClick: (AirportModel) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(AppTheme.dimensions.paddingSmall),
        modifier = modifier
    ) {
        items(airports, key = { it.id }) { airport ->
            AirportListItem(
                airport = airport,
                onClick = { onAirportClick(airport) }
            )
        }
    }
}

@Composable
fun AirportListItem(
    airport: AirportModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = AppTheme.shape.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppTheme.dimensions.paddingMedium)
        ) {
            Text(
                text = airport.iataCode,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(AppTheme.dimensions.paddingMedium))
            Text(
                text = airport.name,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// --- Lista de Cards de Rotas de Voo ---
@Composable
fun FlightRoutesList(
    routes: List<FlightRouteModel>,
    onFavoriteClick: (FlightRouteModel) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(AppTheme.dimensions.spacingMedium),
        modifier = modifier
    ) {
        items(routes, key = { it.id }) { route ->
            FlightRouteCard(
                route = route,
                onFavoriteClick = { onFavoriteClick(route) }
            )
        }
    }
}

// --- Card Individual de Rota ---
@Composable
fun FlightRouteCard(
    route: FlightRouteModel,
    onFavoriteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = AppTheme.shape.large,
        elevation = CardDefaults.cardElevation(defaultElevation = AppTheme.dimensions.cardElevation),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppTheme.dimensions.paddingMedium)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                // Origem
                Text(
                    text = "DE",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.outline
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = route.departureAirport.iataCode,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.width(AppTheme.dimensions.paddingSmall))
                    Text(
                        text = route.departureAirport.name,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )
                }

                Spacer(modifier = Modifier.height(AppTheme.dimensions.spacingMedium))

                // Destino
                Text(
                    text = "PARA",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.outline
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = route.destinationAirport.iataCode,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.width(AppTheme.dimensions.paddingSmall))
                    Text(
                        text = route.destinationAirport.name,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )
                }
            }

            // Botão de Favorito (Estrela)
            IconButton(onClick = onFavoriteClick) {
                Icon(
                    imageVector = if (route.isFavorite) Icons.Filled.Star else Icons.Outlined.Star,
                    contentDescription = "Favoritar rota",
                    tint = if (route.isFavorite) Color(0xFFFFC107) else MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

// --- Mensagem de Favoritos Vazios ---
@Composable
fun EmptyFavoritesMessage() {
    Card(
        shape = AppTheme.shape.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "Nenhuma rota salva nos favoritos ainda.\nPesquise um aeroporto acima para encontrar voos!",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.padding(AppTheme.dimensions.paddingLarge)
        )
    }
}

// --- Previews para o Android Studio ---

// Dados falsos de exemplo
private val mockAirports = listOf(
    AirportModel(1, "SFO", "San Francisco International Airport", 15000),
    AirportModel(2, "JFK", "John F. Kennedy International Airport", 20000),
    AirportModel(3, "GRU", "São Paulo/Guarulhos International Airport", 18000),
    AirportModel(4, "GIG", "Rio de Janeiro/Galeão International Airport", 12000)
)

private val mockRoutes = listOf(
    FlightRouteModel(1, mockAirports[0], mockAirports[1], isFavorite = true),
    FlightRouteModel(2, mockAirports[0], mockAirports[2], isFavorite = false),
    FlightRouteModel(3, mockAirports[0], mockAirports[3], isFavorite = false)
)

@Preview(showBackground = true)
@Composable
fun HomeScreenStateInterativoPreview() {
    var query by remember { mutableStateOf("SF") }
    var selectedAirport by remember { mutableStateOf<AirportModel?>(null) }
    var routes by remember { mutableStateOf(mockRoutes) }

    FlightSearchTheme {
        HomeScreen(
            searchQuery = query,
            onSearchQueryChange = {
                query = it
                if (it.isEmpty()) selectedAirport = null
            },
            selectedAirport = selectedAirport,
            onAirportSelected = { selectedAirport = it },
            airportsList = mockAirports.filter {
                it.iataCode.contains(query, ignoreCase = true) ||
                        it.name.contains(query, ignoreCase = true)
            },
            flightRoutes = routes,
            favoriteRoutes = routes.filter { it.isFavorite },
            onFavoriteClick = { clickedRoute ->
                routes = routes.map {
                    if (it.id == clickedRoute.id) it.copy(isFavorite = !it.isFavorite) else it
                }
            }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenFavoritesPreview() {
    FlightSearchTheme {
        HomeScreen(
            searchQuery = "",
            favoriteRoutes = mockRoutes.filter { it.isFavorite }
        )
    }
}
