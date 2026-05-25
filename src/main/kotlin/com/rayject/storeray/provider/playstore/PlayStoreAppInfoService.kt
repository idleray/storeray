package com.rayject.storeray.provider.playstore

import com.rayject.storeray.model.AppInfoData
import com.rayject.storeray.provider.AppInfoService
import com.rayject.storeray.provider.playstore.api.PlayStorePublisherApi

class PlayStoreAppInfoService(
    private val api: PlayStorePublisherApi
) : AppInfoService {

    override suspend fun fetch(): Map<String, AppInfoData> {
        val listings = api.fetchListings()

        return listings.mapNotNull { listing ->
            val playStoreLocale = listing.language ?: return@mapNotNull null
            if (playStoreLocale.isBlank()) return@mapNotNull null
            val appStoreLocale = PlayStoreLocaleMapper.toAppStoreLocale(playStoreLocale)
            if (appStoreLocale.isBlank()) return@mapNotNull null
            appStoreLocale to AppInfoData(
                video = listing.video ?: ""
            )
        }.toMap()
    }
}
