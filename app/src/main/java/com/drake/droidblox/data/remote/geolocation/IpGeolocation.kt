package com.drake.droidblox.data.remote.geolocation

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request

class IpGeolocation {

    private val client = OkHttpClient.Builder().build()
    private val json = Json { ignoreUnknownKeys = true }

    private var datacenterIps: Set<String>? = null

    suspend fun fetchDatacenterIps(): Set<String> {
        datacenterIps?.let { return it }
        return try {
            val request = Request.Builder()
                .url("https://raw.githubusercontent.com/RoSeal-Extension/Top-Secret-Thing/refs/heads/main/data/datacenters.json")
                .get()
                .build()
            val response = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                client.newCall(request).execute()
            }
            val body = response.body?.string() ?: throw Exception("Empty")
            val parsed = json.decodeFromString<List<String>>(body)
            parsed.toSet().also { datacenterIps = it }
        } catch (e: Exception) {
            println("Failed to fetch datacenter IPs: ${e.message}")
            emptySet()
        }
    }

    suspend fun isDatacenter(ip: String): Boolean {
        val datacenters = fetchDatacenterIps()
        return datacenters.contains(ip)
    }

    suspend fun lookupLocation(ip: String): String? {
        return try {
            val request = Request.Builder()
                .url("https://ipinfo.io/$ip/json")
                .get()
                .build()
            val response = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                client.newCall(request).execute()
            }
            val body = response.body?.string() ?: return null
            val data = json.parseToJsonElement(body).jsonObject
            val city = data["city"]?.jsonPrimitive?.contentOrNull
            val region = data["region"]?.jsonPrimitive?.contentOrNull
            val country = data["country"]?.jsonPrimitive?.contentOrNull
            listOfNotNull(city, region, country).ifEmpty { null }?.joinToString(", ")
        } catch (e: Exception) {
            null
        }
    }
}
