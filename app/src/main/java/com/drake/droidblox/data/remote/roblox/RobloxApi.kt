package com.drake.droidblox.data.remote.roblox

import com.drake.droidblox.data.models.*
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class RobloxApi {

    private val client = OkHttpClient.Builder().build()
    private val json = Json { ignoreUnknownKeys = true }
    private val mediaType = "application/json".toMediaType()

    suspend fun getGameInfo(universeIds: List<Long>): Result<List<GameInfo>> {
        if (universeIds.isEmpty()) return Result.success(emptyList())
        return try {
            val ids = universeIds.joinToString(",")
            val request = Request.Builder()
                .url("https://games.roblox.com/v1/games?universeIds=$ids")
                .get()
                .build()
            val response = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                client.newCall(request).execute()
            }
            val body = response.body?.string() ?: throw Exception("Empty")
            val data = json.parseToJsonElement(body).jsonObject["data"]
            val games = json.decodeFromString<List<GameInfo>>(
                kotlinx.serialization.builtins.ListSerializer(GameInfo.serializer()),
                data.toString()
            )
            Result.success(games)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getThumbnails(requests: List<ThumbnailRequest>): Result<List<ThumbnailItem>> {
        if (requests.isEmpty()) return Result.success(emptyList())
        return try {
            val body = json.encodeToString(
                kotlinx.serialization.builtins.ListSerializer(ThumbnailRequest.serializer()),
                requests
            )
            val request = Request.Builder()
                .url("https://thumbnails.roblox.com/v1/batch")
                .post(body.toRequestBody(mediaType))
                .build()
            val response = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                client.newCall(request).execute()
            }
            val responseBody = response.body?.string() ?: throw Exception("Empty")
            val result = json.decodeFromString<ThumbnailResponse>(responseBody)
            Result.success(result.data)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUserInfo(userId: Long): Result<RobloxUser> {
        return try {
            val request = Request.Builder()
                .url("https://users.roblox.com/v1/users/$userId")
                .get()
                .build()
            val response = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                client.newCall(request).execute()
            }
            val body = response.body?.string() ?: throw Exception("Empty")
            Result.success(json.decodeFromString(body))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
