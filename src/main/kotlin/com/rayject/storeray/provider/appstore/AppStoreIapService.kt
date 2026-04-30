package com.rayject.storeray.provider.appstore

import com.rayject.storeray.model.LocalizationInfo
import com.rayject.storeray.model.Subscription
import com.rayject.storeray.provider.IapService
import com.rayject.storeray.provider.appstore.api.AppStoreConnectApi
import kotlinx.serialization.json.jsonPrimitive

class AppStoreIapService(
    private val api: AppStoreConnectApi,
    private val bundleId: String
) : IapService {

    private var appIdCache: String? = null
    
    private suspend fun getAppId(): String {
        if (appIdCache == null) {
            val app = api.fetchApp(bundleId) ?: throw RuntimeException("未找到 Bundle ID 为 $bundleId 的 App")
            appIdCache = app.id
        }
        return appIdCache!!
    }

    override suspend fun fetchSubscriptions(): List<Subscription> {
        val appId = getAppId()
        // 1. 获取所有的 IAP 以备后续匹配 product_id 和 reference_name
        val iaps = api.fetchIaps(appId)
        
        // 2. 获取 productId 到 subscriptionId 的映射
        val subMap = api.fetchSubscriptions(appId)

        val subscriptions = mutableListOf<Subscription>()
        
        for (iap in iaps) {
            val productId = iap.attributes?.get("productId")?.jsonPrimitive?.content ?: continue
            val referenceName = iap.attributes["referenceName"]?.jsonPrimitive?.content ?: ""
            val state = iap.attributes["state"]?.jsonPrimitive?.content ?: ""
            
            val subId = subMap[productId]
            if (subId != null) {
                subscriptions.add(
                    Subscription(
                        id = subId,
                        productId = productId,
                        referenceName = referenceName,
                        state = state
                    )
                )
            }
        }
        
        return subscriptions
    }

    override suspend fun fetchLocalizations(subscriptionId: String): Map<String, LocalizationInfo> {
        val ascLocales = api.fetchLocalizations(subscriptionId)
        
        return ascLocales.associate { loc ->
            val locale = loc.attributes?.get("locale")?.jsonPrimitive?.content ?: ""
            val name = loc.attributes?.get("name")?.jsonPrimitive?.content ?: ""
            val description = loc.attributes?.get("description")?.jsonPrimitive?.content ?: ""
            
            locale to LocalizationInfo(
                id = loc.id,
                locale = locale,
                name = name,
                description = description
            )
        }.filterKeys { it.isNotEmpty() }
    }

    override suspend fun createLocalization(
        subscriptionId: String,
        locale: String,
        name: String,
        description: String
    ) {
        api.createLocalization(subscriptionId, locale, name, description)
    }

    override suspend fun updateLocalization(
        localizationId: String,
        name: String,
        description: String
    ) {
        api.updateLocalization(localizationId, name, description)
    }
}
