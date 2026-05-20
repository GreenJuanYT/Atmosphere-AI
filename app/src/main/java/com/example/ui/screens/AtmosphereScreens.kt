package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.api.GlobalNewsArticle
import com.example.api.HistoricalWeather
import com.example.api.WeatherDetails
import com.example.api.OutlookForecast
import com.example.data.BookmarkedArticle
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import com.example.ui.viewmodel.AtmosphereViewModel
import com.example.ui.viewmodel.NewsUiState
import com.example.ui.viewmodel.Screen
import com.example.ui.viewmodel.WeatherUiState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainDashboard(viewModel: AtmosphereViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    val notificationCount by viewModel.notifications.collectAsState()
    val unreadCount = notificationCount.size

    Scaffold(
        topBar = {
            AtmosphereTopBar(
                currentScreen = currentScreen,
                unreadNotifications = unreadCount,
                onNotificationClick = { viewModel.navigateTo(Screen.CONFIG) }
            )
        },
        bottomBar = {
            AtmosphereNavigationBar(
                currentScreen = currentScreen,
                onTabSelected = { viewModel.navigateTo(it) }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentScreen) {
                Screen.WEATHER -> WeatherScreen(viewModel)
                Screen.NEWS -> NewsScreen(viewModel)
                Screen.BOOKMARKS -> BookmarksScreen(viewModel)
                Screen.CONFIG -> ConfigScreen(viewModel)
            }
        }
    }
}

// --- Top Bar ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AtmosphereTopBar(
    currentScreen: Screen,
    unreadNotifications: Int,
    onNotificationClick: () -> Unit
) {
    val titleText = when (currentScreen) {
        Screen.WEATHER -> "Atmosphere Weather"
        Screen.NEWS -> "Pulse Global News"
        Screen.BOOKMARKS -> "Saved Desks"
        Screen.CONFIG -> "Control Center"
    }

    val icon = when (currentScreen) {
        Screen.WEATHER -> Icons.Default.CloudQueue
        Screen.NEWS -> Icons.Default.Public
        Screen.BOOKMARKS -> Icons.Default.Bookmarks
        Screen.CONFIG -> Icons.Default.Settings
    }

    CenterAlignedTopAppBar(
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = titleText,
                    fontWeight = FontWeight.Bold,
                    fontSize = 19.sp,
                    fontFamily = FontFamily.SansSerif,
                    letterSpacing = 0.5.sp
                )
            }
        },
        actions = {
            Box(
                modifier = Modifier
                    .padding(end = 8.dp)
                    .clickable { onNotificationClick() }
            ) {
                IconButton(onClick = onNotificationClick) {
                    Icon(
                        imageVector = Icons.Outlined.NotificationsActive,
                        contentDescription = "Simulated Notifications Center"
                    )
                }
                if (unreadNotifications > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = (-2).dp, y = (4).dp)
                            .background(MaterialTheme.colorScheme.error, CircleShape)
                            .size(18.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = unreadNotifications.toString(),
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    )
}

// --- Bottom Navigation ---
@Composable
fun AtmosphereNavigationBar(
    currentScreen: Screen,
    onTabSelected: (Screen) -> Unit
) {
    NavigationBar(
        tonalElevation = 6.dp
    ) {
        NavigationBarItem(
            icon = { Icon(Icons.Default.Cloud, contentDescription = "Weather") },
            label = { Text("Weather", fontSize = 11.sp) },
            selected = currentScreen == Screen.WEATHER,
            onClick = { onTabSelected(Screen.WEATHER) },
            modifier = Modifier.testTag("nav_weather_tab")
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Newspaper, contentDescription = "Global News") },
            label = { Text("World News", fontSize = 11.sp) },
            selected = currentScreen == Screen.NEWS,
            onClick = { onTabSelected(Screen.NEWS) },
            modifier = Modifier.testTag("nav_news_tab")
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Bookmark, contentDescription = "Bookmarks") },
            label = { Text("Bookmarks", fontSize = 11.sp) },
            selected = currentScreen == Screen.BOOKMARKS,
            onClick = { onTabSelected(Screen.BOOKMARKS) },
            modifier = Modifier.testTag("nav_bookmarks_tab")
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Tune, contentDescription = "Control Center") },
            label = { Text("Configure", fontSize = 11.sp) },
            selected = currentScreen == Screen.CONFIG,
            onClick = { onTabSelected(Screen.CONFIG) },
            modifier = Modifier.testTag("nav_config_tab")
        )
    }
}

// ==================== SCREEN 1: WEATHER SCREEN ====================
@Composable
fun WeatherScreen(viewModel: AtmosphereViewModel) {
    val searchCity by viewModel.weatherSearchQuery.collectAsState()
    val weatherState by viewModel.weatherUiState.collectAsState()
    val keyboardController = LocalSoftwareKeyboardController.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Search Card
        OutlinedTextField(
            value = searchCity,
            onValueChange = { viewModel.updateWeatherQuery(it) },
            label = { Text("City, e.g. New York, Tokyo, Paris") },
            leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
            trailingIcon = {
                IconButton(
                    onClick = {
                        keyboardController?.hide()
                        viewModel.searchWeather(searchCity)
                    },
                    modifier = Modifier.testTag("weather_search_btn")
                ) {
                    Icon(Icons.Default.Search, contentDescription = "Search Weather")
                }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = {
                keyboardController?.hide()
                viewModel.searchWeather(searchCity)
            }),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("weather_search_input")
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Content
        when (val state = weatherState) {
            is WeatherUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            is WeatherUiState.Success -> {
                WeatherProfileContent(state.details, viewModel)
            }
            is WeatherUiState.Error -> {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Error Loading Station Weather",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = state.message,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = { viewModel.searchWeather(searchCity) }) {
                            Text("Retry Network Link")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WeatherProfileContent(details: WeatherDetails, viewModel: AtmosphereViewModel) {
    // Dynamic Weather-Adaptive Visual Atmosphere Brush
    val weatherBrush = remember(details.condition) {
        getAtmosphericBrush(details.condition)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Current Weather Main Panel
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(weatherBrush)
                        .padding(24.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = details.city,
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = details.country,
                                    fontSize = 14.sp,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                            }
                            
                            // Weather Icon
                            val iconPair = getWeatherIconAndAccentColor(details.condition)
                            Box(
                                modifier = Modifier
                                    .background(Color.White.copy(alpha = 0.2f), CircleShape)
                                    .padding(8.dp)
                            ) {
                                Icon(
                                    imageVector = iconPair.first,
                                    contentDescription = details.condition,
                                    tint = iconPair.second,
                                    modifier = Modifier.size(48.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Text(
                                text = "${details.tempCelsius}°C",
                                fontSize = 56.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = details.condition,
                                fontSize = 21.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.White.copy(alpha = 0.9f),
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                        }

                        // Auxiliary Stats Grid
                        Divider(color = Color.White.copy(alpha = 0.3f), modifier = Modifier.padding(vertical = 12.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            WeatherStatItem(Icons.Default.WaterDrop, "Humidity", "${details.humidityPercent}%")
                            WeatherStatItem(Icons.Default.Air, "Wind Speed", "${details.windSpeedKmh}km/h")
                            WeatherStatItem(Icons.Default.WaterDrop, "Rain Prob.", "${details.precipitationPercent}%")
                            WeatherStatItem(
                                icon = Icons.Default.FilterHdr,
                                label = "AQI",
                                value = "${details.airQualityIndex} (${details.airQualityDescription.take(8)})"
                            )
                        }
                    }
                }
            }
        }

        // Severe Emergency Alert Section
        if (details.alertTitle != null && details.alertSeverity != "None") {
            item {
                var expandedAlert by remember { mutableStateOf(false) }
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (details.alertSeverity == "Severe")
                            MaterialTheme.colorScheme.errorContainer
                        else
                            MaterialTheme.colorScheme.tertiaryContainer
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .clickable { expandedAlert = !expandedAlert }
                            .padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Severe Alert Warning",
                                tint = if (details.alertSeverity == "Severe")
                                    MaterialTheme.colorScheme.error
                                else
                                    MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "SEVERE ALARM TRIGGERED",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp,
                                    color = if (details.alertSeverity == "Severe")
                                        MaterialTheme.colorScheme.error
                                    else
                                        MaterialTheme.colorScheme.onTertiaryContainer
                                )
                                Text(
                                    text = details.alertTitle,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                            Icon(
                                imageVector = if (expandedAlert) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null
                            )
                        }
                        
                        AnimatedVisibility(visible = expandedAlert) {
                            Column {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = details.alertDescription ?: "No additional safety parameters provided.",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color.White.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                        .padding(8.dp)
                                ) {
                                    Text(
                                        text = "⚡ Action Recommended: Secure outdoor property, monitor regional advisories, and plan commutes safely.",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 7-Day Extended Weather Outlook
        item {
            ExtendedWeatherOutlook(outlookList = details.outlookList)
        }

        // Solar UV Safety and Protection Monitor
        item {
            UvSafetyMonitor(
                uvIndex = details.uvIndex,
                uvDescription = details.uvDescription,
                uvAdvice = details.uvSafetyAdvice
            )
        }

        // Technical Meteorological Analytics parameters
        item {
            AdvancedAtmosphereCard(
                dewPoint = details.dewPointCelsius,
                visibility = details.visibilityKm,
                pressure = details.pressureHpa,
                heatIndex = details.heatIndexCelsius
            )
        }

        // Historical Trend Section (Custom Canvas Bar Chart)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Historical Weather Trend",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "Previous 7 Days",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    WeatherHistoricalChart(details.historyList)
                }
            }
        }

        // Weather News Highlights Across the World
        item {
            Text(
                text = "Weather News Reports",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        items(details.weatherNewsList) { article ->
            var readExtendedMsg by remember { mutableStateOf(false) }
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { readExtendedMsg = !readExtendedMsg }
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = article.publisher,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = article.publishedTime,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = article.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = article.summary,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = if (readExtendedMsg) 10 else 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun WeatherStatItem(icon: ImageVector, label: String, value: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 10.sp,
            color = Color.White.copy(alpha = 0.7f)
        )
        Text(
            text = value,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

@Composable
fun UvSafetyMonitor(uvIndex: Int, uvDescription: String, uvAdvice: String) {
    val uvColor = when {
        uvIndex <= 2 -> Color(0xFF4CAF50) // Green (Low)
        uvIndex <= 5 -> Color(0xFFFFD54F) // Yellow (Moderate) - warmer, better contrast
        uvIndex <= 7 -> Color(0xFFFF9800) // Orange (High)
        uvIndex <= 10 -> Color(0xFFF44336) // Red (Very High)
        else -> Color(0xFF9C27B0) // Violet (Extreme)
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("uv_safety_monitor_card"),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.WbSunny,
                        contentDescription = "UV Solar Index",
                        tint = uvColor,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Solar UV Safety Monitor",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Box(
                    modifier = Modifier
                        .background(uvColor.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = uvDescription,
                        color = if (uvIndex in 3..5) Color(0xFF886600) else uvColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Circular arc scale indicator
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(72.dp)) {
                    Canvas(modifier = Modifier.size(64.dp)) {
                        // Background track
                        drawCircle(
                            color = Color.LightGray.copy(alpha = 0.15f),
                            radius = size.minDimension / 2f,
                            style = Stroke(width = 6.dp.toPx())
                        )
                        // Active portion indicator
                        val sweepAngle = (uvIndex.coerceIn(0, 15) / 15f) * 360f
                        drawArc(
                            color = uvColor,
                            startAngle = -90f,
                            sweepAngle = sweepAngle,
                            useCenter = false,
                            style = Stroke(width = 6.dp.toPx())
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = uvIndex.toString(),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "UV INDEX",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Protection Advice & Tips",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = uvAdvice,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
fun ExtendedWeatherOutlook(outlookList: List<OutlookForecast>) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("extended_outlook_card"),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = "Forecast",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Upcoming 7-Day Outlook",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
                Text(
                    text = "High / Low",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            outlookList.forEachIndexed { index, day ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Day name and date
                    Column(modifier = Modifier.width(80.dp)) {
                        Text(
                            text = day.dayLabel,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = day.dateText,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Condition and Rain Probability
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        val iconPair = getWeatherIconAndAccentColor(day.condition)
                        Icon(
                            imageVector = iconPair.first,
                            contentDescription = day.condition,
                            tint = iconPair.second,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = day.condition,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.widthIn(max = 68.dp)
                        )
                        if (day.precipitationPercent > 0) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.WaterDrop,
                                    contentDescription = "Rain Prob",
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(10.dp)
                                )
                                Text(
                                    text = "${day.precipitationPercent}%",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.secondary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    // Highest and Lowest Temps
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.End,
                        modifier = Modifier.width(80.dp)
                    ) {
                        Text(
                            text = "${day.tempMax.toInt()}°",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${day.tempMin.toInt()}°",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                }

                if (index < outlookList.size - 1) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                        thickness = 1.dp,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun AdvancedAtmosphereCard(
    dewPoint: Float,
    visibility: Float,
    pressure: Float,
    heatIndex: Float
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("advanced_atmosphere_card"),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = "Advanced Atmospheric Diagnostics",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    AdvancedMetricRow(
                        icon = Icons.Default.DeviceThermostat,
                        label = "Heat Index",
                        value = "${heatIndex.toInt()}°C",
                        desc = "Apparent feel temperature"
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    AdvancedMetricRow(
                        icon = Icons.Default.Speed,
                        label = "Pressure",
                        value = "${pressure.toInt()} hPa",
                        desc = "Atmospheric air pressure"
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    AdvancedMetricRow(
                        icon = Icons.Default.Opacity,
                        label = "Dew Point",
                        value = "${dewPoint.toInt()}°C",
                        desc = "Saturation threshold"
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    AdvancedMetricRow(
                        icon = Icons.Default.Visibility,
                        label = "Visibility",
                        value = "${visibility.toInt()} km",
                        desc = "Visual clear range"
                    )
                }
            }
        }
    }
}

@Composable
fun AdvancedMetricRow(
    icon: ImageVector,
    label: String,
    value: String,
    desc: String
) {
    Row(verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape)
                .padding(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(
                text = value,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = label,
                fontWeight = FontWeight.Medium,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = desc,
                fontSize = 9.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                lineHeight = 11.sp
            )
        }
    }
}

// A Beautiful custom canvas chart drawing temperatures over the previous 7 days
@Composable
fun WeatherHistoricalChart(history: List<HistoricalWeather>) {
    if (history.isEmpty()) return
    
    val maxTemp = history.maxOfOrNull { it.tempCelsius } ?: 40f
    val minTemp = history.minOfOrNull { it.tempCelsius } ?: 0f
    val range = (maxTemp - minTemp).coerceAtLeast(1f)

    Column {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
                .padding(horizontal = 8.dp, vertical = 12.dp)
        ) {
            val width = size.width
            val height = size.height
            val pointsCount = history.size
            val sectionWidth = width / (pointsCount - 1)

            val chartPoints = history.mapIndexed { i, hw ->
                val x = i * sectionWidth
                val tempRatio = (hw.tempCelsius - minTemp) / range
                val y = height - (tempRatio * height * 0.8f).toFloat() - (height * 0.1f).toFloat()
                Offset(x, y)
            }

            // Draw area gradient under curve
            val areaPath = Path().apply {
                moveTo(0f, height)
                lineTo(chartPoints[0].x, chartPoints[0].y)
                for (i in 1 until chartPoints.size) {
                    lineTo(chartPoints[i].x, chartPoints[i].y)
                }
                lineTo(width, height)
                close()
            }

            // Area Gradient Brush
            drawPath(
                path = areaPath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF3B82F6).copy(alpha = 0.3f),
                        Color(0xFF3B82F6).copy(alpha = 0.0f)
                    )
                )
            )

            // Draw line
            val linePath = Path().apply {
                moveTo(chartPoints[0].x, chartPoints[0].y)
                for (i in 1 until chartPoints.size) {
                    lineTo(chartPoints[i].x, chartPoints[i].y)
                }
            }
            drawPath(
                path = linePath,
                color = Color(0xFF3B82F6),
                style = Stroke(width = 4f)
            )

            // Draw node dots and temperatures text
            chartPoints.forEachIndexed { idx, point ->
                drawCircle(
                    color = Color.White,
                    radius = 8f,
                    center = point
                )
                drawCircle(
                    color = Color(0xFF3B82F6),
                    radius = 5f,
                    center = point
                )
            }
        }

        // X-Axis Labels Grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            history.forEach { hw ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = hw.dayLabel,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${hw.tempCelsius.toInt()}°",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}


// ==================== SCREEN 2: NEWS SCREEN ====================
@Composable
fun NewsScreen(viewModel: AtmosphereViewModel) {
    val newsState by viewModel.newsUiState.collectAsState()
    val categories = listOf("Trending", "World", "Technology", "Science", "Business", "Entertainment", "Health")
    val selectedCat by viewModel.selectedNewsCategory.collectAsState()
    val bookmarks by viewModel.bookmarkedArticles.collectAsState()
    val searchInput by viewModel.newsSearchQuery.collectAsState()
    val keyboardController = LocalSoftwareKeyboardController.current

    var selectedFullArticle by remember { mutableStateOf<GlobalNewsArticle?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Keyword Search Strip
        OutlinedTextField(
            value = searchInput,
            onValueChange = { viewModel.updateNewsSearchQuery(it) },
            label = { Text("Search stories globally") },
            placeholder = { Text("Enter keyword, e.g., AI, Fusion, Supercells") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (searchInput.isNotEmpty()) {
                    IconButton(onClick = { 
                        viewModel.updateNewsSearchQuery("")
                        viewModel.loadNews(selectedCat, "")
                    }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                    }
                }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = {
                keyboardController?.hide()
                viewModel.executeNewsSearch()
            }),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("news_search_field")
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Category Horizontal Row Selection
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(categories) { cat ->
                val isSelected = cat == selectedCat
                FilterChip(
                    selected = isSelected,
                    onClick = { viewModel.selectNewsCategory(cat) },
                    label = { Text(cat) },
                    leadingIcon = if (isSelected) {
                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    } else null,
                    modifier = Modifier.testTag("news_chip_$cat")
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // News Articles Stream
        when (val state = newsState) {
            is NewsUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            is NewsUiState.Success -> {
                if (state.articles.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No articles found matching that criteria.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(state.articles) { article ->
                            val isBookmarked = bookmarks.any { it.url == article.url }
                            NewsArticleRowCard(
                                article = article,
                                isBookmarked = isBookmarked,
                                onBookmarkToggle = { viewModel.toggleBookmark(article) },
                                onArticleClick = { selectedFullArticle = article }
                            )
                        }
                    }
                }
            }
            is NewsUiState.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Error loading global feeds: ${state.message}")
                }
            }
        }
    }

    // Modal Sheet representation for Full Article text reading
    selectedFullArticle?.let { article ->
        AlertDialog(
            onDismissRequest = { selectedFullArticle = null },
            confirmButton = {
                TextButton(onClick = { selectedFullArticle = null }) {
                    Text("Back to Stream")
                }
            },
            title = {
                Column {
                    Text(
                        text = article.source,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = article.title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(androidx.compose.foundation.rememberScrollState())
                ) {
                    AsyncImage(
                        model = article.imageUrl,
                        contentDescription = article.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = article.description,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = article.content,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 20.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Published at ${article.publishedAt}",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }
            }
        )
    }
}

@Composable
fun NewsArticleRowCard(
    article: GlobalNewsArticle,
    isBookmarked: Boolean,
    onBookmarkToggle: () -> Unit,
    onArticleClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onArticleClick() }
            .testTag("article_card")
    ) {
        Column {
            Box {
                AsyncImage(
                    model = article.imageUrl,
                    contentDescription = article.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                )

                // Bookmark Icon Button placed floating
                IconButton(
                    onClick = onBookmarkToggle,
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                    ),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .testTag("bookmark_toggle_${article.url.hashCode()}")
                ) {
                    Icon(
                        imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                        contentDescription = "Bookmark story",
                        tint = if (isBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = article.source,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = article.publishedAt,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = article.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = article.description,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}


// ==================== SCREEN 3: BOOKMARKS SCREEN ====================
@Composable
fun BookmarksScreen(viewModel: AtmosphereViewModel) {
    val bookmarks by viewModel.bookmarkedArticles.collectAsState()
    var selectedFullBookmark by remember { mutableStateOf<BookmarkedArticle?>(null) }

    if (bookmarks.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.BookmarkBorder,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "No saved articles yet.",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Text(
                    text = "Click the Bookmark button on news stories to read them anytime, even offline.",
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(
                text = "Your Bookmarked Desks (${bookmarks.size})",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(bookmarks) { article ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedFullBookmark = article }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = article.imageUrl,
                                contentDescription = article.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(70.dp)
                                    .clip(RoundedCornerShape(8.dp))
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = article.source,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = article.title,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = article.description,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            IconButton(onClick = { viewModel.toggleBookmarkState(article) }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Remove Bookmark",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    selectedFullBookmark?.let { bookmark ->
        AlertDialog(
            onDismissRequest = { selectedFullBookmark = null },
            confirmButton = {
                TextButton(onClick = { selectedFullBookmark = null }) {
                    Text("Dismiss")
                }
            },
            title = {
                Column {
                    Text(
                        text = bookmark.source,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = bookmark.title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Column(modifier = Modifier.verticalScroll(androidx.compose.foundation.rememberScrollState())) {
                    AsyncImage(
                        model = bookmark.imageUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = bookmark.description,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Historical reference logs preserved locally. Bookmark saved at ${bookmark.publishedAt}.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        )
    }
}


// ==================== SCREEN 4: CONTROL CENTER ====================
@Composable
fun ConfigScreen(viewModel: AtmosphereViewModel) {
    val preferences by viewModel.newsPreferences.collectAsState()
    val alertSettings by viewModel.severeAlertSettings.collectAsState()
    val notifications by viewModel.notifications.collectAsState()

    var showAddAlertCityDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // News Custom Feed preference
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.FilterList, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Customized News Feeds",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                    Text(
                        text = "Toggle your interest topics to design custom news deck flows.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    preferences.forEach { pref ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.togglePreference(pref) }
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(pref.category, fontSize = 14.sp)
                            Switch(
                                checked = pref.isEnabled,
                                onCheckedChange = { viewModel.togglePreference(pref) }
                            )
                        }
                    }
                }
            }
        }

        // Severe Alerts Monitor Center
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.FlashOn, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Extreme Weather Channels",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                        
                        IconButton(onClick = { showAddAlertCityDialog = true }) {
                            Icon(Icons.Default.AddCircle, contentDescription = "Add Severe Alert Channel")
                        }
                    }
                    Text(
                        text = "Select custom trigger cities to dispatch background severe warning sweeps.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))

                    if (alertSettings.isEmpty()) {
                        Text(
                            text = "No severe weather channels mapped.",
                            fontSize = 13.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else {
                        alertSettings.forEach { setting ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(setting.city, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text(
                                        text = "Temp limits: >${setting.tempMaxThreshold.toInt()}°C / <${setting.tempMinThreshold.toInt()}°C",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(onClick = { viewModel.removeAlertSetting(setting) }) {
                                    Icon(
                                        imageVector = Icons.Default.RemoveCircleOutline,
                                        contentDescription = "Remove setting",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Simulated Push Notifications Broadcast Center
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.ChatBubbleOutline, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Simulate Breaking Alerts",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                    Text(
                        text = "Test-notify real-time atmospheric hazards or flash worldwide developments immediately.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                viewModel.createSimulatedNotification(
                                    title = "🌪️ Extreme Tornado Watch: Central Plains",
                                    text = "Localized cell rotations spotted near baseline systems. Alert sirens engaged.",
                                    type = "WEATHER_ALERT"
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Test Weather Alarm", fontSize = 11.sp, textAlign = TextAlign.Center)
                        }

                        Button(
                            onClick = {
                                viewModel.createSimulatedNotification(
                                    title = "🔥 BREAKING NEWS: Volcanic Catalyst",
                                    text = "Mount Merapi volcanic ash cloud spreads 1.2km across high altitude paths, disrupting aerospace lanes.",
                                    type = "BREAKING_NEWS"
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Test News Alert", fontSize = 11.sp, textAlign = TextAlign.Center)
                        }
                    }
                }
            }
        }

        // Simulated Push logs and clear button
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Live Active Feed Alerts (${notifications.size})",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                if (notifications.isNotEmpty()) {
                    TextButton(onClick = { viewModel.clearAllNotifications() }) {
                        Text("Clear All")
                    }
                }
            }
        }

        if (notifications.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No active breaking alert push broadcasts.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            items(notifications) { log ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (log.type == "WEATHER_ALERT")
                            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                        else
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = if (log.type == "WEATHER_ALERT") "WEATHER WARNING" else "BREAKING SENSATIONAL NEWS",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (log.type == "WEATHER_ALERT")
                                    MaterialTheme.colorScheme.error
                                else
                                    MaterialTheme.colorScheme.primary
                            )
                            IconButton(
                                onClick = { viewModel.dismissNotification(log.id) },
                                modifier = Modifier.size(16.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Dismiss", modifier = Modifier.size(14.dp))
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(log.title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(log.text, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(log.timestamp, fontSize = 10.sp, color = Color.Gray)
                    }
                }
            }
        }
    }

    // Add alarm channel popup dialog
    if (showAddAlertCityDialog) {
        var cityText by remember { mutableStateOf("") }
        var maxTempLimit by remember { mutableStateOf("38") }
        var minTempLimit by remember { mutableStateOf("0") }

        AlertDialog(
            onDismissRequest = { showAddAlertCityDialog = false },
            confirmButton = {
                Button(
                    onClick = {
                        if (cityText.isNotBlank()) {
                            viewModel.saveAlertSetting(
                                city = cityText,
                                isEnabled = true,
                                tempMax = maxTempLimit.toFloatOrNull() ?: 38f,
                                tempMin = minTempLimit.toFloatOrNull() ?: 0f,
                                notificationEnabled = true
                            )
                        }
                        showAddAlertCityDialog = false
                    }
                ) {
                    Text("Add Alarm Channel")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddAlertCityDialog = false }) {
                    Text("Cancel")
                }
            },
            title = { Text("Add Weather Alarm Channel") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = cityText,
                        onValueChange = { cityText = it },
                        label = { Text("Target City, e.g. London") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = maxTempLimit,
                        onValueChange = { maxTempLimit = it },
                        label = { Text("Max Temp Threshold (°C)") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = minTempLimit,
                        onValueChange = { minTempLimit = it },
                        label = { Text("Min Temp Threshold (°C)") },
                        singleLine = true
                    )
                }
            }
        )
    }
}


// --- Helper styling configurations ---

private fun getAtmosphericBrush(condition: String): Brush {
    val norm = condition.trim().lowercase()
    return when {
        norm.contains("sunny") || norm.contains("clear") -> Brush.verticalGradient(
            colors = listOf(Color(0xFFF59E0B), Color(0xFF3B82F6))
        )
        norm.contains("rain") || norm.contains("shower") -> Brush.verticalGradient(
            colors = listOf(Color(0xFF374151), Color(0xFF1E3A8A))
        )
        norm.contains("storm") || norm.contains("thunder") -> Brush.verticalGradient(
            colors = listOf(Color(0xFF2E1065), Color(0xFF0F172A))
        )
        norm.contains("cloud") -> Brush.verticalGradient(
            colors = listOf(Color(0xFF64748B), Color(0xFF94A3B8))
        )
        norm.contains("wind") || norm.contains("breeze") -> Brush.verticalGradient(
            colors = listOf(Color(0xFF0D9488), Color(0xFF0891B2))
        )
        norm.contains("snow") || norm.contains("freeze") -> Brush.verticalGradient(
            colors = listOf(Color(0xFFE2E8F0), Color(0xFF38BDF8))
        )
        else -> Brush.verticalGradient(
            colors = listOf(Color(0xFF475569), Color(0xFF1E293B))
        )
    }
}

private fun getWeatherIconAndAccentColor(condition: String): Pair<ImageVector, Color> {
    val norm = condition.trim().lowercase()
    return when {
        norm.contains("sunny") || norm.contains("clear") -> Pair(Icons.Default.WbSunny, Color(0xFFFBBF24))
        norm.contains("rain") || norm.contains("shower") -> Pair(Icons.Default.Cloud, Color(0xFF60A5FA))
        norm.contains("storm") || norm.contains("thunder") -> Pair(Icons.Default.Warning, Color(0xFFF59E0B))
        norm.contains("cloud") -> Pair(Icons.Default.Cloud, Color(0xFF94A3B8))
        norm.contains("wind") || norm.contains("breeze") -> Pair(Icons.Default.Cloud, Color(0xFF2DD4BF))
        norm.contains("snow") || norm.contains("freeze") -> Pair(Icons.Default.Cloud, Color(0xFF38BDF8))
        else -> Pair(Icons.Default.Cloud, Color(0xFF94A3B8))
    }
}
