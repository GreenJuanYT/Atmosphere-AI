package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.api.GeminiClient
import com.example.api.GlobalNewsArticle
import com.example.api.WeatherDetails
import com.example.data.AppDatabase
import com.example.data.AtmosphereRepository
import com.example.data.BookmarkedArticle
import com.example.data.NewsPreference
import com.example.data.SevereAlertSetting
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class Screen {
    WEATHER, NEWS, BOOKMARKS, CONFIG
}

sealed interface WeatherUiState {
    object Loading : WeatherUiState
    data class Success(val details: WeatherDetails) : WeatherUiState
    data class Error(val message: String) : WeatherUiState
}

sealed interface NewsUiState {
    object Loading : NewsUiState
    data class Success(val articles: List<GlobalNewsArticle>) : NewsUiState
    data class Error(val message: String) : NewsUiState
}

data class NotificationLog(
    val id: Long = System.currentTimeMillis() + (1..1000).random(),
    val title: String,
    val text: String,
    val type: String, // "WEATHER_ALERT", "BREAKING_NEWS"
    val timestamp: String = "Just now",
    val isRead: Boolean = false
)

class AtmosphereViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: AtmosphereRepository = AtmosphereRepository(AppDatabase.getDatabase(application))

    // --- Screen State ---
    private val _currentScreen = MutableStateFlow(Screen.WEATHER)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    fun navigateTo(screen: Screen) {
        _currentScreen.value = screen
    }

    // --- Weather States ---
    private val _weatherSearchQuery = MutableStateFlow("Chicago")
    val weatherSearchQuery: StateFlow<String> = _weatherSearchQuery.asStateFlow()

    private val _weatherUiState = MutableStateFlow<WeatherUiState>(WeatherUiState.Loading)
    val weatherUiState: StateFlow<WeatherUiState> = _weatherUiState.asStateFlow()

    fun updateWeatherQuery(query: String) {
        _weatherSearchQuery.value = query
    }

    fun searchWeather(city: String) {
        viewModelScope.launch {
            _weatherUiState.value = WeatherUiState.Loading
            val result = GeminiClient.fetchWeather(city)
            if (result != null) {
                _weatherUiState.value = WeatherUiState.Success(result)
                // If the dynamic city has a severe alert, register it to simulated notification log!
                if (!result.alertTitle.isNullOrEmpty() && result.alertSeverity != "None") {
                    createSimulatedNotification(
                        title = "SEVERE ALERT: ${result.alertTitle} in ${result.city}",
                        text = result.alertDescription ?: "Meteorological alert threshold reached.",
                        type = "WEATHER_ALERT"
                    )
                }
            } else {
                _weatherUiState.value = WeatherUiState.Error("Failed to fetch weather forecast. Please check internet connection.")
            }
        }
    }

    // --- Global News States ---
    private val _newsSearchQuery = MutableStateFlow("")
    val newsSearchQuery: StateFlow<String> = _newsSearchQuery.asStateFlow()

    private val _selectedNewsCategory = MutableStateFlow("Trending")
    val selectedNewsCategory: StateFlow<String> = _selectedNewsCategory.asStateFlow()

    private val _newsUiState = MutableStateFlow<NewsUiState>(NewsUiState.Loading)
    val newsUiState: StateFlow<NewsUiState> = _newsUiState.asStateFlow()

    fun updateNewsSearchQuery(query: String) {
        _newsSearchQuery.value = query
    }

    fun selectNewsCategory(category: String) {
        _selectedNewsCategory.value = category
        loadNews(category, _newsSearchQuery.value)
    }

    fun executeNewsSearch() {
        loadNews(_selectedNewsCategory.value, _newsSearchQuery.value)
    }

    fun loadNews(category: String, query: String = "") {
        viewModelScope.launch {
            _newsUiState.value = NewsUiState.Loading
            val articles = GeminiClient.fetchGlobalNews(category, query)
            _newsUiState.value = NewsUiState.Success(articles)
        }
    }

    // --- Room Database Subscriptions ---
    val bookmarkedArticles: StateFlow<List<BookmarkedArticle>> = repository.allBookmarks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val newsPreferences: StateFlow<List<NewsPreference>> = repository.allPreferences
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val severeAlertSettings: StateFlow<List<SevereAlertSetting>> = repository.allAlertSettings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Room Writes ---
    fun toggleBookmark(article: GlobalNewsArticle) {
        viewModelScope.launch {
            val dbArticle = BookmarkedArticle(
                url = article.url,
                title = article.title,
                description = article.description,
                source = article.source,
                publishedAt = article.publishedAt,
                imageUrl = article.imageUrl,
                category = article.category
            )
            val isAlreadyBookmarked = repository.isBookmarked(article.url)
            if (isAlreadyBookmarked) {
                repository.removeBookmark(dbArticle)
            } else {
                repository.addBookmark(dbArticle)
            }
        }
    }

    fun toggleBookmarkState(article: BookmarkedArticle) {
        viewModelScope.launch {
            repository.removeBookmark(article)
        }
    }

    fun togglePreference(pref: NewsPreference) {
        viewModelScope.launch {
            repository.savePreference(pref.copy(isEnabled = !pref.isEnabled))
        }
    }

    fun saveAlertSetting(city: String, isEnabled: Boolean, tempMax: Float, tempMin: Float, notificationEnabled: Boolean) {
        viewModelScope.launch {
            val setting = SevereAlertSetting(
                city = city,
                isEnabled = isEnabled,
                tempMaxThreshold = tempMax,
                tempMinThreshold = tempMin,
                notificationEnabled = notificationEnabled
            )
            repository.saveAlertSetting(setting)
            searchWeather(city) // Search the city immediately to check safety warnings
        }
    }

    fun removeAlertSetting(setting: SevereAlertSetting) {
        viewModelScope.launch {
            repository.deleteAlertSetting(setting)
        }
    }

    private suspend fun prefetchDefaultPreferences() {
        val defaultCategories = listOf("Trending", "World", "Technology", "Science", "Business", "Entertainment", "Health")
        defaultCategories.forEach { cat ->
            if (repository.isCategoryEnabled(cat)) {
                repository.savePreference(NewsPreference(cat, true))
            }
        }
        
        // Also save a couple of initial Severe alert settings for showcase
        repository.saveAlertSetting(SevereAlertSetting("Miami", true, 35f, 15f))
        repository.saveAlertSetting(SevereAlertSetting("London", true, 30f, 0f))
    }

    // --- Simulated Active Breaking Push Notifications Engine ---
    private val _notifications = MutableStateFlow<List<NotificationLog>>(
        listOf(
            NotificationLog(
                title = "⚡ Breaking Weather Alert",
                text = "Extremely wild pressure shifts recorded near Chicago. Peak winds crossing 62km/h over coastal channels.",
                type = "WEATHER_ALERT",
                timestamp = "3 mins ago"
            ),
            NotificationLog(
                title = "📰 Trending Global Update",
                text = "Renewable Fusion Cells reach first sustainable multi-hour test cycle, accelerating smart energy integration.",
                type = "BREAKING_NEWS",
                timestamp = "24 mins ago"
            )
        )
    )
    val notifications: StateFlow<List<NotificationLog>> = _notifications.asStateFlow()

    init {
        // Load initial defaults
        viewModelScope.launch {
            prefetchDefaultPreferences()
        }
        
        // Trigger initial data loads
        searchWeather("Chicago")
        loadNews("Trending")
    }

    fun createSimulatedNotification(title: String, text: String, type: String) {
        val newLog = NotificationLog(
            title = title,
            text = text,
            type = type
        )
        // Keep unique and prepend
        val currentList = _notifications.value.toMutableList()
        currentList.add(0, newLog)
        _notifications.value = currentList.take(20) // Keep standard cache limit
    }

    fun dismissNotification(id: Long) {
        _notifications.value = _notifications.value.filter { it.id != id }
    }

    fun clearAllNotifications() {
        _notifications.value = emptyList()
    }
}
