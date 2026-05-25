package com.rayject.storeray.provider.appstore.api

import com.rayject.storeray.provider.appstore.api.model.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.*

class AppStoreConnectApi(private val token: String) {
    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
    }

    private val baseUrl = "https://api.appstoreconnect.apple.com/v1"

    suspend fun fetchApp(bundleId: String): AscResource? {
        val response = get("$baseUrl/apps", mapOf("filter[bundleId]" to bundleId))
        val body: AscListResponse<AscResource> = response.body()
        return body.data.find { 
            it.attributes?.get("bundleId")?.jsonPrimitive?.content == bundleId 
        }
    }

    suspend fun fetchSubscriptions(appId: String): Map<String, String> {
        // 1. 获取订阅组
        val groupRes = get("$baseUrl/apps/$appId", mapOf("include" to "subscriptionGroups"))
        val groupBody: AscResponse<AscResource> = groupRes.body()
        val group = groupBody.included?.find { it.type == "subscriptionGroups" } ?: return emptyMap()

        // 2. 获取订阅组中的订阅
        val subRes = get("$baseUrl/subscriptionGroups/${group.id}", mapOf("include" to "subscriptions"))
        val subBody: AscResponse<AscResource> = subRes.body()
        val subscriptions = subBody.included?.filter { it.type == "subscriptions" } ?: emptyList()

        // 返回 productId -> subscriptionId 的映射
        return subscriptions.associate { sub ->
            val productId = sub.attributes?.get("productId")?.jsonPrimitive?.content ?: ""
            productId to sub.id
        }.filterKeys { it.isNotEmpty() }
    }

    suspend fun fetchIaps(appId: String): List<AscResource> {
        val response = get("$baseUrl/apps/$appId", mapOf("include" to "inAppPurchases"))
        val body: AscResponse<AscResource> = response.body()
        return body.included?.filter { it.type == "inAppPurchases" } ?: emptyList()
    }

    suspend fun fetchLocalizations(subscriptionId: String): List<AscResource> {
        val response = get("$baseUrl/subscriptions/$subscriptionId/subscriptionLocalizations")
        val body: AscListResponse<AscResource> = response.body()
        return body.data.filter { it.type == "subscriptionLocalizations" }
    }

    suspend fun createLocalization(subscriptionId: String, locale: String, name: String, description: String) {
        val body = buildJsonObject {
            put("data", buildJsonObject {
                put("type", "subscriptionLocalizations")
                put("attributes", buildJsonObject {
                    put("locale", locale)
                    put("name", name)
                    put("description", description)
                })
                put("relationships", buildJsonObject {
                    put("subscription", buildJsonObject {
                        put("data", buildJsonObject {
                            put("type", "subscriptions")
                            put("id", subscriptionId)
                        })
                    })
                })
            })
        }
        post("$baseUrl/subscriptionLocalizations", body)
    }

    suspend fun updateLocalization(localizationId: String, name: String, description: String) {
        val body = buildJsonObject {
            put("data", buildJsonObject {
                put("type", "subscriptionLocalizations")
                put("id", localizationId)
                put("attributes", buildJsonObject {
                    put("name", name)
                    put("description", description)
                })
            })
        }
        patch("$baseUrl/subscriptionLocalizations/$localizationId", body)
    }

    suspend fun fetchAppStoreVersions(appId: String, versionString: String): List<AscResource> {
        val response = get("$baseUrl/apps/$appId/appStoreVersions", mapOf("filter[versionString]" to versionString))
        val body: AscListResponse<AscResource> = response.body()
        return body.data.filter { it.type == "appStoreVersions" }
    }

    suspend fun fetchEditableAppStoreVersions(appId: String): List<AscResource> {
        val response = get("$baseUrl/apps/$appId/appStoreVersions", mapOf(
            "filter[appStoreState]" to "PREPARE_FOR_SUBMISSION"
        ))
        val body: AscListResponse<AscResource> = response.body()
        return body.data.filter { it.type == "appStoreVersions" }
    }

    suspend fun fetchAppStoreVersionLocalizations(versionId: String): List<AscResource> {
        val response = get("$baseUrl/appStoreVersions/$versionId/appStoreVersionLocalizations")
        val body: AscListResponse<AscResource> = response.body()
        return body.data.filter { it.type == "appStoreVersionLocalizations" }
    }

    suspend fun fetchAppInfos(appId: String): List<AscResource> {
        val response = get("$baseUrl/apps/$appId/appInfos")
        val body: AscListResponse<AscResource> = response.body()
        return body.data
    }

    suspend fun fetchAppInfoLocalizations(appInfoId: String): List<AscResource> {
        val response = get("$baseUrl/appInfos/$appInfoId/appInfoLocalizations")
        val body: AscListResponse<AscResource> = response.body()
        return body.data
    }

    suspend fun updateAppStoreVersionLocalization(localizationId: String, whatsNew: String) {
        val body = buildJsonObject {
            put("data", buildJsonObject {
                put("type", "appStoreVersionLocalizations")
                put("id", localizationId)
                put("attributes", buildJsonObject {
                    put("whatsNew", whatsNew)
                })
            })
        }
        patch("$baseUrl/appStoreVersionLocalizations/$localizationId", body)
    }

    suspend fun createAppStoreVersionLocalization(versionId: String, locale: String, attributes: JsonObject) {
        val body = buildJsonObject {
            put("data", buildJsonObject {
                put("type", "appStoreVersionLocalizations")
                put("attributes", buildJsonObject {
                    put("locale", locale)
                    attributes.forEach { (key, value) -> put(key, value) }
                })
                put("relationships", buildJsonObject {
                    put("appStoreVersion", buildJsonObject {
                        put("data", buildJsonObject {
                            put("type", "appStoreVersions")
                            put("id", versionId)
                        })
                    })
                })
            })
        }
        post("$baseUrl/appStoreVersionLocalizations", body)
    }

    suspend fun updateAppStoreVersionLocalizationAttributes(localizationId: String, attributes: JsonObject) {
        val body = buildJsonObject {
            put("data", buildJsonObject {
                put("type", "appStoreVersionLocalizations")
                put("id", localizationId)
                put("attributes", attributes)
            })
        }
        patch("$baseUrl/appStoreVersionLocalizations/$localizationId", body)
    }

    suspend fun createAppInfoLocalization(appInfoId: String, locale: String, attributes: JsonObject) {
        val body = buildJsonObject {
            put("data", buildJsonObject {
                put("type", "appInfoLocalizations")
                put("attributes", buildJsonObject {
                    put("locale", locale)
                    attributes.forEach { (key, value) -> put(key, value) }
                })
                put("relationships", buildJsonObject {
                    put("appInfo", buildJsonObject {
                        put("data", buildJsonObject {
                            put("type", "appInfos")
                            put("id", appInfoId)
                        })
                    })
                })
            })
        }
        post("$baseUrl/appInfoLocalizations", body)
    }

    suspend fun updateAppInfoLocalization(localizationId: String, attributes: JsonObject) {
        val body = buildJsonObject {
            put("data", buildJsonObject {
                put("type", "appInfoLocalizations")
                put("id", localizationId)
                put("attributes", attributes)
            })
        }
        patch("$baseUrl/appInfoLocalizations/$localizationId", body)
    }

    private suspend fun get(url: String, params: Map<String, String> = emptyMap()): HttpResponse {
        val response = client.get(url) {
            header(HttpHeaders.Authorization, "Bearer $token")
            params.forEach { (k, v) -> parameter(k, v) }
        }
        checkError(response)
        return response
    }

    private suspend fun post(url: String, body: JsonObject): HttpResponse {
        val response = client.post(url) {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(body)
        }
        checkError(response)
        return response
    }

    private suspend fun patch(url: String, body: JsonObject): HttpResponse {
        val response = client.patch(url) {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(body)
        }
        checkError(response)
        return response
    }

    private suspend fun checkError(response: HttpResponse) {
        if (!response.status.isSuccess()) {
            val errorBody = try {
                response.body<AscErrorResponse>()
            } catch (e: Exception) {
                null
            }
            val errorMsg = errorBody?.errors?.joinToString(", ") { "${it.title}: ${it.detail}" }
                ?: response.bodyAsText()
            throw RuntimeException("API 错误 ${response.status.value}: $errorMsg")
        }
    }
}
