package com.drake.droidblox.data.remote.discord

import com.drake.droidblox.data.models.DiscordUser
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.putJsonArray
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class DiscordApi(private val tokenProvider: () -> String?) {

    private val client = OkHttpClient.Builder().build()
    private val json = Json { ignoreUnknownKeys = true }
    private val mediaType = "application/json".toMediaType()
    private val baseUrl = "https://discord.com/api/v9"
    private val appId = "1379313837169311825"

    suspend fun getUsername(token: String): Result<DiscordUser> {
        return makeGet("$baseUrl/users/@me", token) { body ->
            json.decodeFromString<DiscordUser>(body)
        }
    }

    suspend fun getMediaProxyUrls(token: String, urls: List<String>): Result<List<String>> {
        if (urls.isEmpty()) return Result.success(emptyList())
        return try {
            val jsonBody = buildJsonObject {
                putJsonArray("urls") {
                    urls.forEach { add(it) }
                }
            }
            val request = Request.Builder()
                .url("$baseUrl/applications/$appId/external-assets")
                .header("Authorization", "Bearer $token")
                .post(jsonBody.toString().toRequestBody(mediaType))
                .build()
            val response = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                client.newCall(request).execute()
            }
            val body = response.body?.string() ?: return Result.success(urls)
            if (!response.isSuccessful) return Result.success(urls)

            val assets = json.decodeFromString<List<AssetResponse>>(
                kotlinx.serialization.builtins.ListSerializer(AssetResponse.serializer()),
                json.parseToJsonElement(body).jsonObject["data"].toString()
            )
            Result.success(assets.map { it.url })
        } catch (e: Exception) {
            Result.success(urls)
        }
    }

    suspend fun logout(token: String): Result<Unit> {
        return makePost("$baseUrl/auth/logout", token, "{}")
    }

    private suspend fun <T> makeGet(url: String, token: String, parser: (String) -> T): Result<T> {
        return try {
            val request = Request.Builder()
                .url(url)
                .header("Authorization", "$token")
                .get()
                .build()
            val response = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                client.newCall(request).execute()
            }
            val body = response.body?.string() ?: throw Exception("Empty response")
            if (!response.isSuccessful) throw Exception("HTTP ${response.code()}: $body")
            Result.success(parser(body))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun makePost(url: String, token: String, jsonBody: String): Result<Unit> {
        return try {
            val request = Request.Builder()
                .url(url)
                .header("Authorization", "$token")
                .post(jsonBody.toRequestBody(mediaType))
                .build()
            val response = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                client.newCall(request).execute()
            }
            if (!response.isSuccessful) {
                response.body?.string()
                throw Exception("HTTP ${response.code()}")
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private data class AssetResponse(val id: String = "", val url: String = "")
}
