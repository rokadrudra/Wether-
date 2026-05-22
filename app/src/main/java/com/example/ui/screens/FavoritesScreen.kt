package com.example.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.api.GeocodingResult
import com.example.data.db.FavoriteCity
import com.example.ui.WeatherViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun FavoritesScreen(
    viewModel: WeatherViewModel,
    onFavoriteSelected: () -> Unit,
    modifier: Modifier = Modifier
) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val searchLoading by viewModel.searchLoading.collectAsState()
    val favorites by viewModel.favoriteCities.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Saved Locations",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            text = "Search and manage your cities",
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.7f)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Search Text Field
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.updateSearchQuery(it) },
            placeholder = { Text("Search city (e.g., London, New York)", color = Color.White.copy(alpha = 0.5f)) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.White.copy(alpha = 0.7f)) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear", tint = Color.White.copy(alpha = 0.7f))
                    }
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Color.White,
                unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                focusedContainerColor = Color.White.copy(alpha = 0.08f),
                unfocusedContainerColor = Color.White.copy(alpha = 0.08f),
                cursorColor = Color.White
            ),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("location_search_input")
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Loading or List of Search Results
        if (searchLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color.White)
            }
        } else if (searchQuery.length >= 2 && searchResults.isEmpty()) {
            Text(
                text = "No cities matching query found.",
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            )
        } else if (searchResults.isNotEmpty()) {
            Text(
                text = "Search Results",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(alpha = 0.8f)
            )
            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(searchResults) { result ->
                    SearchResultRow(
                        result = result,
                        onResultClick = {
                            viewModel.selectCity(result)
                            onFavoriteSelected()
                        },
                        onAddFavorite = {
                            // Convert result to entity
                            viewModel.toggleFavorite(
                                cityId = result.id,
                                name = result.name,
                                country = result.country,
                                state = result.admin1,
                                lat = result.latitude,
                                lon = result.longitude,
                                isFav = false
                            )
                        },
                        isFav = favorites.any { it.id == result.id }
                    )
                }
            }
        } else {
            // Favorites List
            if (favorites.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Text(
                            text = "🏙️",
                            fontSize = 48.sp,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        Text(
                            text = "No saved locations",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Start typing above to search and pin your favorite cities.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(8.dp))
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(favorites) { favorite ->
                        FavoriteCityRow(
                            city = favorite,
                            onCityClick = {
                                viewModel.loadWeather(
                                    lat = favorite.latitude,
                                    lon = favorite.longitude,
                                    name = favorite.name
                                )
                                onFavoriteSelected()
                            },
                            onDelete = {
                                viewModel.toggleFavorite(
                                    cityId = favorite.id,
                                    name = favorite.name,
                                    country = favorite.country,
                                    state = favorite.state,
                                    lat = favorite.latitude,
                                    lon = favorite.longitude,
                                    isFav = true
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SearchResultRow(
    result: GeocodingResult,
    onResultClick: () -> Unit,
    onAddFavorite: () -> Unit,
    isFav: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.1f))
            .clickable { onResultClick() }
            .padding(12.dp)
            .testTag("search_result_item"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = result.name,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontSize = 16.sp
            )
            val subLabel = buildString {
                if (!result.admin1.isNullOrEmpty()) append(result.admin1)
                if (!result.country.isNullOrEmpty()) {
                    if (isNotEmpty()) append(", ")
                    append(result.country)
                }
            }
            if (subLabel.isNotEmpty()) {
                Text(
                    text = subLabel,
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 12.sp
                )
            }
        }

        IconButton(onClick = onAddFavorite) {
            Icon(
                imageVector = if (isFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                contentDescription = "Pin Favorite",
                tint = if (isFav) Color.Red else Color.White
            )
        }
    }
}

@Composable
fun FavoriteCityRow(
    city: FavoriteCity,
    onCityClick: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.12f))
            .clickable { onCityClick() }
            .padding(16.dp)
            .testTag("favorite_city_item"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                text = city.name,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                fontSize = 18.sp
            )
            val regionDetails = buildString {
                if (!city.state.isNullOrEmpty()) append(city.state)
                if (!city.country.isNullOrEmpty()) {
                    if (isNotEmpty()) append(", ")
                    append(city.country)
                }
            }
            if (regionDetails.isNotEmpty()) {
                Text(
                    text = regionDetails,
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 13.sp
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${String.format("%.2f", city.latitude)}°N, ${String.format("%.2f", city.longitude)}°E",
                fontSize = 11.sp,
                color = Color.White.copy(alpha = 0.5f)
            )
        }

        IconButton(
            onClick = onDelete,
            modifier = Modifier.background(Color.White.copy(alpha = 0.1f), CircleShape)
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Delete Pin",
                tint = Color.White.copy(alpha = 0.8f)
            )
        }
    }
}
