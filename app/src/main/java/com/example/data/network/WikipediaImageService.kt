package com.example.data.network

import android.util.Log
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class WikipediaImageInfo(
    @Json(name = "source") val source: String? = null,
    @Json(name = "width") val width: Int? = null,
    @Json(name = "height") val height: Int? = null
)

@JsonClass(generateAdapter = true)
data class WikipediaContentUrls(
    @Json(name = "mobile") val mobile: WikipediaMobileUrl? = null
)

@JsonClass(generateAdapter = true)
data class WikipediaMobileUrl(
    @Json(name = "page") val page: String? = null
)

@JsonClass(generateAdapter = true)
data class WikipediaSummaryResponse(
    @Json(name = "title") val title: String? = null,
    @Json(name = "description") val description: String? = null,
    @Json(name = "extract") val extract: String? = null,
    @Json(name = "thumbnail") val thumbnail: WikipediaImageInfo? = null,
    @Json(name = "originalimage") val originalImage: WikipediaImageInfo? = null,
    @Json(name = "content_urls") val contentUrls: WikipediaContentUrls? = null
)

@JsonClass(generateAdapter = true)
data class WikipediaSearchApiResponse(
    @Json(name = "query") val query: WikipediaSearchQuery? = null
)

@JsonClass(generateAdapter = true)
data class WikipediaSearchQuery(
    @Json(name = "search") val search: List<WikipediaSearchItem> = emptyList()
)

@JsonClass(generateAdapter = true)
data class WikipediaSearchItem(
    @Json(name = "title") val title: String = ""
)

interface WikipediaRestApi {
    @GET("api/rest_v1/page/summary/{title}")
    suspend fun getPageSummary(
        @Path("title", encoded = true) encodedTitle: String
    ): WikipediaSummaryResponse

    @GET("w/api.php?action=query&list=search&format=json&srlimit=3")
    suspend fun searchPages(
        @Query("srsearch") searchQuery: String
    ): WikipediaSearchApiResponse
}

data class PictureSearchResult(
    val imageUrl: String,
    val title: String,
    val caption: String,
    val sourcePageUrl: String?
)

class WikipediaPictureSearchClient {

    private val api: WikipediaRestApi by lazy {
        val okHttp = OkHttpClient.Builder()
            .connectTimeout(12, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val req = chain.request().newBuilder()
                    .header("User-Agent", "AvyaktAiAndroid/1.0 (https://example.com; educational-assistant)")
                    .header("Accept", "application/json")
                    .build()
                chain.proceed(req)
            }
            .build()

        val moshi = Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()

        Retrofit.Builder()
            .baseUrl("https://en.wikipedia.org/")
            .client(okHttp)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(WikipediaRestApi::class.java)
    }

    suspend fun searchExistingPicture(topic: String): PictureSearchResult? =
        withContext(Dispatchers.IO) {
            val cleanTopic = topic.trim()
            if (cleanTopic.isBlank()) return@withContext null

            try {
                withTimeout(12_000L) {
                    val searchRes = api.searchPages(cleanTopic)
                    val candidateTitles = buildList {
                        searchRes.query?.search?.forEach { if (it.title.isNotBlank()) add(it.title) }
                        if (isEmpty()) add(cleanTopic)
                    }

                    for (title in candidateTitles) {
                        val slug = URLEncoder.encode(title.replace(" ", "_"), "UTF-8")
                            .replace("+", "%20")
                        val summary = runCatching { api.getPageSummary(slug) }.getOrNull() ?: continue
                        val imgUrl = summary.originalImage?.source ?: summary.thumbnail?.source
                        if (!imgUrl.isNullOrBlank() && imgUrl.startsWith("https://")) {
                            return@withTimeout PictureSearchResult(
                                imageUrl = imgUrl,
                                title = summary.title ?: title,
                                caption = summary.description?.takeIf { it.isNotBlank() }
                                    ?: summary.extract?.take(140)
                                    ?: title,
                                sourcePageUrl = summary.contentUrls?.mobile?.page
                            )
                        }
                    }
                    null
                }
            } catch (e: Exception) {
                Log.w(TAG, "Picture search failed for topic: $cleanTopic", e)
                null
            }
        }

    companion object {
        private const val TAG = "WikiPictureSearch"
    }
}
