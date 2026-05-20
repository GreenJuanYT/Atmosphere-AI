package com.example.api

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

// --- Models ---
data class OutlookForecast(
    val dayLabel: String, // e.g., "Mon", "Tue"
    val dateText: String, // e.g., "May 20"
    val tempMax: Float,
    val tempMin: Float,
    val condition: String,
    val precipitationPercent: Int
)

data class WeatherDetails(
    val city: String,
    val country: String,
    val tempCelsius: Float,
    val condition: String, // e.g., "Sunny", "Rain", "Cloudy", "Stormy", "Snow"
    val humidityPercent: Int,
    val windSpeedKmh: Float,
    val precipitationPercent: Int,
    val airQualityIndex: Int, // 1 to 5 index (1=Good, 5=Hazardous)
    val airQualityDescription: String,
    val alertTitle: String?,
    val alertSeverity: String?, // "Severe", "Moderate", "None"
    val alertDescription: String?,
    val historyList: List<HistoricalWeather>,
    val weatherNewsList: List<WeatherNewsArticle>,
    val uvIndex: Int,
    val uvDescription: String,
    val uvSafetyAdvice: String,
    val visibilityKm: Float,
    val pressureHpa: Float,
    val dewPointCelsius: Float,
    val heatIndexCelsius: Float,
    val outlookList: List<OutlookForecast>
)

data class HistoricalWeather(
    val dayLabel: String, // e.g., 'Mon', 'Tue'
    val dateText: String,
    val tempCelsius: Float,
    val condition: String,
    val summary: String
)

data class WeatherNewsArticle(
    val title: String,
    val summary: String,
    val publisher: String,
    val publishedTime: String,
    val webUrl: String
)

data class GlobalNewsArticle(
    val title: String,
    val description: String,
    val content: String,
    val source: String,
    val publishedAt: String,
    val imageUrl: String,
    val category: String,
    val url: String
)

object GeminiClient {
    private const val TAG = "GeminiClient"
    private const val MODEL_NAME = "gemini-3.5-flash"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/$MODEL_NAME:generateContent"

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val mediaTypeJson = "application/json; charset=utf-8".toMediaType()

    suspend fun fetchWeather(city: String): WeatherDetails? = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            Log.e(TAG, "API Key is missing or invalid")
            return@withContext getFallbackWeather(city)
        }

        val systemPrompt = """
            You are an expert meteorological system giving extremely accurate-looking current weather details, customized severe alert updates, a 7-day historic trend from consecutive past days, premium skin protection/solar safety measures (UV index), technical parameters (dew point, visibility, air pressure, heat safety levels), and a 7-day forward weather outlook alongside standard regional climatic articles.
            Analyze the requested city and return a strictly structured JSON matching the requested schema.
        """.trimIndent()

        val userPrompt = """
            Provide a complete meteorological, historical, alert, forward outlook, and weather news profile for the city of "$city". 
            Generate realistic current indices, precipitation ratios, actual geographical positions, and active weather alerts if applicable (otherwise return empty severe alerts in fields).
            Generate 7 days of historical weather data representing the previous week.
            Generate 7 days of upcoming weather outlook forecasting the next week (e.g. from tomorrow onwards).
            Generate 3 interesting, local weather news or climate/seasonal transition articles for this region.
            
            Return ONLY a valid JSON object with the following structure. Do not wrap it in ```json``` markdown tags:
            {
               "city": "String",
               "country": "String",
               "tempCelsius": Float,
               "condition": "String (must be one of: Sunny, Cloudy, Rainy, Stormy, Windy, Snowy, Foggy)",
               "humidityPercent": Int (0 to 100),
               "windSpeedKmh": Float,
               "precipitationPercent": Int (0 to 100),
               "airQualityIndex": Int (1 to 5),
               "airQualityDescription": "String (e.g., Good, Fair, Unhealthy)",
               "alertTitle": "String or null",
               "alertSeverity": "String (must be one of: Severe, Moderate, None)",
               "alertDescription": "String or null",
               "uvIndex": Int (0 to 15),
               "uvDescription": "String (e.g., Low, Moderate, High, Very High, Extreme)",
               "uvSafetyAdvice": "String (e.g., 'Apply SPF 30+ every 2 hours, wear a wide-brim hat, seek shade.')",
               "visibilityKm": Float,
               "pressureHpa": Float,
               "dewPointCelsius": Float,
               "heatIndexCelsius": Float,
               "history": [
                   {
                      "dayLabel": "String (e.g., 'Mon', 'Sun')",
                      "dateText": "String",
                      "tempCelsius": Float,
                      "condition": "String",
                      "summary": "String"
                   }
               ],
               "outlook": [
                   {
                      "dayLabel": "String (e.g., 'Mon', 'Tue')",
                      "dateText": "String (e.g., 'May 21')",
                      "tempMax": Float,
                      "tempMin": Float,
                      "condition": "String (e.g., 'Sunny', 'Cloudy', 'Rainy')",
                      "precipitationPercent": Int (0 to 100)
                   }
               ],
               "weatherNews": [
                   {
                      "title": "String",
                      "summary": "String",
                      "publisher": "String",
                      "publishedTime": "String (e.g., '2 hours ago')",
                      "webUrl": "String"
                   }
               ]
            }
        """.trimIndent()

        val jsonRequest = buildRequestPlayload(userPrompt, systemPrompt)
        val url = "$BASE_URL?key=$apiKey"

        val request = Request.Builder()
            .url(url)
            .post(jsonRequest.toRequestBody(mediaTypeJson))
            .build()

        try {
            client.newCall(request).execute().use { response ->
                val bodyText = response.body?.string()
                if (!response.isSuccessful || bodyText == null) {
                    Log.e(TAG, "API Fail: code=${response.code}, body=$bodyText")
                    return@withContext getFallbackWeather(city)
                }

                val jsonResponse = JSONObject(bodyText)
                val textCandidate = extractCandidateText(jsonResponse) ?: return@withContext getFallbackWeather(city)
                val sanitizedJson = cleanRawJsonString(textCandidate)

                val resultObj = JSONObject(sanitizedJson)
                return@withContext parseWeatherObject(resultObj)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to pull weather from Gemini API, resolving fallback", e)
            return@withContext getFallbackWeather(city)
        }
    }

    suspend fun fetchGlobalNews(category: String, searchQuery: String = ""): List<GlobalNewsArticle> = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            Log.e(TAG, "API Key is missing or invalid")
            return@withContext getFallbackNews(category)
        }

        val systemPrompt = """
            You are a premier world news editor assembling headlines, trending alerts, breaking articles, and local viewpoints from verified channels globally. Output stories tailored perfectly for the category or query described.
        """.trimIndent()

        val userPrompt = """
            Assemble 7-8 news articles for the category: "$category"${if (searchQuery.isNotEmpty()) " and search query '$searchQuery'" else ""}.
            Include a mix of headline scoops, depth reporting, and feature viewpoints.
            Make it look highly professional with rich article content (2-3 complete sentences in content) and realistic source attributions, custom categories, realistic placeholder image links representing standard stock headers.
            
            Return ONLY a valid JSON array of articles. Do not wrap in ```json``` markdown tags.
            Structure:
            [
               {
                  "title": "String",
                  "description": "String",
                  "content": "String",
                  "source": "String",
                  "publishedAt": "String",
                  "imageUrl": "String",
                  "category": "String",
                  "url": "String (must be a unique string, e.g., 'news-url-slug-1')"
               }
            ]
        """.trimIndent()

        val jsonRequest = buildRequestPlayload(userPrompt, systemPrompt)
        val url = "$BASE_URL?key=$apiKey"

        val request = Request.Builder()
            .url(url)
            .post(jsonRequest.toRequestBody(mediaTypeJson))
            .build()

        try {
            client.newCall(request).execute().use { response ->
                val bodyText = response.body?.string()
                if (!response.isSuccessful || bodyText == null) {
                    Log.e(TAG, "API Fail for Global News: code=${response.code}")
                    return@withContext getFallbackNews(category)
                }

                val jsonResponse = JSONObject(bodyText)
                val textCandidate = extractCandidateText(jsonResponse) ?: return@withContext getFallbackNews(category)
                val sanitizedJson = cleanRawJsonString(textCandidate)

                val array = JSONArray(sanitizedJson)
                val list = mutableListOf<GlobalNewsArticle>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(
                        GlobalNewsArticle(
                            title = obj.optString("title"),
                            description = obj.optString("description"),
                            content = obj.optString("content"),
                            source = obj.optString("source"),
                            publishedAt = obj.optString("publishedAt"),
                            imageUrl = obj.optString("imageUrl", "https://images.unsplash.com/photo-1504711434969-e33886168f5c?auto=format&fit=crop&w=600&q=80"),
                            category = obj.optString("category", category),
                            url = obj.optString("url")
                        )
                    )
                }
                return@withContext list
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to pull news from Gemini API, return backup values", e)
            return@withContext getFallbackNews(category)
        }
    }

    private fun buildRequestPlayload(prompt: String, systemInstruction: String): String {
        return JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", prompt)
                        })
                    })
                })
            })
            put("systemInstruction", JSONObject().apply {
                put("parts", JSONArray().apply {
                    put(JSONObject().apply {
                        put("text", systemInstruction)
                    })
                })
            })
            // Structured Output format configuration
            put("generationConfig", JSONObject().apply {
                put("responseMimeType", "application/json")
            })
        }.toString()
    }

    private fun extractCandidateText(response: JSONObject): String? {
        return try {
            response.getJSONArray("candidates")
                .getJSONObject(0)
                .getJSONObject("content")
                .getJSONArray("parts")
                .getJSONObject(0)
                .getString("text")
        } catch (e: Exception) {
            null
        }
    }

    private fun cleanRawJsonString(raw: String): String {
        var clean = raw.trim()
        if (clean.startsWith("```json")) {
            clean = clean.removePrefix("```json")
        }
        if (clean.startsWith("```")) {
            clean = clean.removePrefix("```")
        }
        if (clean.endsWith("```")) {
            clean = clean.removeSuffix("```")
        }
        return clean.trim()
    }

    private fun parseWeatherObject(obj: JSONObject): WeatherDetails {
        val historyArr = obj.getJSONArray("history")
        val historyList = mutableListOf<HistoricalWeather>()
        for (i in 0 until historyArr.length()) {
            val h = historyArr.getJSONObject(i)
            historyList.add(
                HistoricalWeather(
                    dayLabel = h.optString("dayLabel"),
                    dateText = h.optString("dateText"),
                    tempCelsius = h.optDouble("tempCelsius").toFloat(),
                    condition = h.optString("condition"),
                    summary = h.optString("summary")
                )
            )
        }

        val outlookArr = obj.optJSONArray("outlook")
        val outlookList = mutableListOf<OutlookForecast>()
        if (outlookArr != null) {
            for (i in 0 until outlookArr.length()) {
                val o = outlookArr.getJSONObject(i)
                outlookList.add(
                    OutlookForecast(
                        dayLabel = o.optString("dayLabel"),
                        dateText = o.optString("dateText"),
                        tempMax = o.optDouble("tempMax").toFloat(),
                        tempMin = o.optDouble("tempMin").toFloat(),
                        condition = o.optString("condition"),
                        precipitationPercent = o.optInt("precipitationPercent")
                    )
                )
            }
        } else {
            // Self-populate dynamic outlook if API drops it
            val tempVal = obj.optDouble("tempCelsius").toFloat()
            val cond = obj.optString("condition")
            val p = obj.optInt("precipitationPercent")
            val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
            for (idx in days.indices) {
                outlookList.add(
                    OutlookForecast(
                        dayLabel = days[idx],
                        dateText = "May ${21 + idx}",
                        tempMax = tempVal + (idx % 3) - 1,
                        tempMin = tempVal - (idx % 2) - 4,
                        condition = cond,
                        precipitationPercent = p
                    )
                )
            }
        }

        val newsArr = obj.getJSONArray("weatherNews")
        val newsList = mutableListOf<WeatherNewsArticle>()
        for (i in 0 until newsArr.length()) {
            val n = newsArr.getJSONObject(i)
            newsList.add(
                WeatherNewsArticle(
                    title = n.optString("title"),
                    summary = n.optString("summary"),
                    publisher = n.optString("publisher"),
                    publishedTime = n.optString("publishedTime"),
                    webUrl = n.optString("webUrl")
                )
            )
        }

        val parsedTemp = obj.optDouble("tempCelsius").toFloat()

        return WeatherDetails(
            city = obj.optString("city"),
            country = obj.optString("country"),
            tempCelsius = parsedTemp,
            condition = obj.optString("condition"),
            humidityPercent = obj.optInt("humidityPercent"),
            windSpeedKmh = obj.optDouble("windSpeedKmh").toFloat(),
            precipitationPercent = obj.optInt("precipitationPercent"),
            airQualityIndex = obj.optInt("airQualityIndex"),
            airQualityDescription = obj.optString("airQualityDescription"),
            alertTitle = if (obj.isNull("alertTitle")) null else obj.optString("alertTitle"),
            alertSeverity = if (obj.isNull("alertSeverity")) "None" else obj.optString("alertSeverity"),
            alertDescription = if (obj.isNull("alertDescription")) null else obj.optString("alertDescription"),
            historyList = historyList,
            weatherNewsList = newsList,
            uvIndex = obj.optInt("uvIndex", if (parsedTemp > 30f) 8 else 4),
            uvDescription = obj.optString("uvDescription", if (parsedTemp > 30f) "Very High" else "Moderate"),
            uvSafetyAdvice = obj.optString("uvSafetyAdvice", if (parsedTemp > 30f) "Apply SPF 50+, wear wide-brim hat, seek shade." else "SPF 30 recommended for extended sun exposure."),
            visibilityKm = obj.optDouble("visibilityKm", 10.0).toFloat(),
            pressureHpa = obj.optDouble("pressureHpa", 1013.2).toFloat(),
            dewPointCelsius = obj.optDouble("dewPointCelsius", (parsedTemp - 5).toDouble()).toFloat(),
            heatIndexCelsius = obj.optDouble("heatIndexCelsius", (parsedTemp + 1).toDouble()).toFloat(),
            outlookList = outlookList
        )
    }

    // --- Fallbacks for demo if no API Key or Net failure ---
    fun getFallbackWeather(city: String): WeatherDetails {
        val normalizedCity = city.trim().lowercase().capitalize()
        val isHot = normalizedCity == "Miami" || normalizedCity == "Dubai"
        val isRainy = normalizedCity == "London" || normalizedCity == "Seattle"
        
        val temp = if (isHot) 34.5f else if (isRainy) 15.0f else 22.1f
        val condition = if (isHot) "Sunny" else if (isRainy) "Rainy" else "Cloudy"
        val precipitation = if (isRainy) 84 else 12
        val aqi = if (isHot) 3 else 1
        val alertTitle = if (isHot) "Heat Advisory" else if (isRainy) "Flood Alert" else null
        val alertSeverity = if (alertTitle != null) "Moderate" else "None"
        val alertDesc = if (alertTitle != null) "Severe conditions expected for the next few hours in $normalizedCity. Stay hydrated and safe." else null

        val historyList = listOf(
            HistoricalWeather("Mon", "May 13", temp - 2, condition, "Calm day, typical trend."),
            HistoricalWeather("Tue", "May 14", temp - 1, condition, "Cloud build up initially."),
            HistoricalWeather("Wed", "May 15", temp - 3, "Cloudy", "Dull sky, overcast."),
            HistoricalWeather("Thu", "May 16", temp, condition, "Nocturnal cooling with active breezes."),
            HistoricalWeather("Fri", "May 17", temp + 1, "Windy", "Steep pressure drops observed."),
            HistoricalWeather("Sat", "May 18", temp + 2, condition, "Sunny breaks after fog."),
            HistoricalWeather("Sun", "May 19", temp, "Rainy", "Persistent showers, strong winds.")
        )

        val outlookList = listOf(
            OutlookForecast("Wed", "May 20", temp + 1f, temp - 3f, condition, precipitation),
            OutlookForecast("Thu", "May 21", temp + 2f, temp - 2f, if (isHot) "Sunny" else "Cloudy", if (isHot) 5 else 30),
            OutlookForecast("Fri", "May 22", temp - 1f, temp - 4f, if (isRainy) "Rainy" else "Cloudy", if (isRainy) 90 else 45),
            OutlookForecast("Sat", "May 23", temp - 2f, temp - 5f, "Stormy", 80),
            OutlookForecast("Sun", "May 24", temp + 1f, temp - 3f, "Windy", 25),
            OutlookForecast("Mon", "May 25", temp + 3f, temp - 1f, "Sunny", 5),
            OutlookForecast("Tue", "May 26", temp + 2f, temp - 1f, "Sunny", 5)
        )

        val weatherNewsList = listOf(
            WeatherNewsArticle("Climate Anomalies over $normalizedCity", "Unprecedented air currents trigger localized heat pockets across the eastern district.", "MeteoGlobal", "3 hours ago", "slug-met-1"),
            WeatherNewsArticle("Solar Forecast & UV Shifts", "Experts recommend wearing SPF protection as seasonal changes impact solar radiation counts.", "SkinHealth Journal", "Yesterday", "slug-met-2")
        )

        val uvIdx = if (isHot) 11 else if (isRainy) 2 else 5
        val uvDesc = if (uvIdx >= 11) "Extreme" else if (uvIdx >= 6) "High" else "Moderate"
        val uvAdvice = if (uvIdx >= 11) {
            "Extreme risk! Apply SPF 50+, wear sunscreen every 2 hrs, shield eyes, and seek complete protection outdoors."
        } else if (uvIdx >= 5) {
            "Moderate sunburn risk. Generously apply SPF 30+, cover sensitive skin, and wear sunglasses."
        } else {
            "Low risk of damage. Normal outdoor exploration is fully safe."
        }

        return WeatherDetails(
            city = normalizedCity,
            country = "United States",
            tempCelsius = temp,
            condition = condition,
            humidityPercent = if (isHot) 40 else 78,
            windSpeedKmh = 14.2f,
            precipitationPercent = precipitation,
            airQualityIndex = aqi,
            airQualityDescription = if (aqi == 1) "Excellent" else "Moderate",
            alertTitle = alertTitle,
            alertSeverity = alertSeverity,
            alertDescription = alertDesc,
            historyList = historyList,
            weatherNewsList = weatherNewsList,
            uvIndex = uvIdx,
            uvDescription = uvDesc,
            uvSafetyAdvice = uvAdvice,
            visibilityKm = if (isRainy) 4.5f else 16.0f,
            pressureHpa = 1012.5f,
            dewPointCelsius = temp - 4f,
            heatIndexCelsius = if (isHot) temp + 3.1f else temp,
            outlookList = outlookList
        )
    }

    fun getFallbackNews(category: String): List<GlobalNewsArticle> {
        val list = mutableListOf<GlobalNewsArticle>()
        val unscaledCategory = category.capitalize()
        val titles = listOf(
            "Technological Strides in Advanced Energy Harnessing for Smart Cities",
            "International Cooperation Framework Announced to Stabilize Resource Logistics",
            "The Rise of Localized Deep-Tech Hubs Changing Demographics Worldwide",
            "Scientific Insights into Multi-Century Carbon Adaptation in High altitudes",
            "Market Index Gains Momentum with Tech Startup Breakthrough Appraisals",
            "Modern Arts Festival Embraces Architectural Prototyping Systems",
            "Wellness Science Emphasizes Consistent Diurnal Rhythm Habits"
        )
        val images = listOf(
            "https://images.unsplash.com/photo-1504384308090-c894fdcc538d?auto=format&fit=crop&w=600&q=80",
            "https://images.unsplash.com/photo-1451187580459-43490279c0fa?auto=format&fit=crop&w=600&q=80",
            "https://images.unsplash.com/photo-1526374965328-7f61d4dc18c5?auto=format&fit=crop&w=600&q=80",
            "https://images.unsplash.com/photo-1518152006813-95950aa006c4?auto=format&fit=crop&w=600&q=80",
            "https://images.unsplash.com/photo-1460925895917-afdab827c52f?auto=format&fit=crop&w=600&q=80"
        )
        for (i in titles.indices) {
            val img = images[i % images.size]
            list.add(
                GlobalNewsArticle(
                    title = titles[i],
                    description = "Recent breakthroughs and developments are steering key operations under the $unscaledCategory domain.",
                    content = "This exclusive report details how researchers, entrepreneurs, and communities are collaborating to transform standard models in $unscaledCategory. Multiple milestones were surpassed last week.",
                    source = "Global Dispatcher",
                    publishedAt = "${i + 1} hr${if (i > 0) "s" else ""} ago",
                    imageUrl = img,
                    category = category,
                    url = "news-fallback-slug-${category.lowercase()}-$i"
                )
            )
        }
        return list
    }
}
