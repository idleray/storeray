package com.rayject.storeray.provider.playstore

import com.rayject.storeray.model.LocalizationInfo
import com.rayject.storeray.model.Subscription
import com.rayject.storeray.provider.IapService

class PlayStoreIapService : IapService {
    override suspend fun fetchSubscriptions(): List<Subscription> {
        throw UnsupportedOperationException("Play Store IAP sync is not supported yet")
    }

    override suspend fun fetchLocalizations(subscriptionId: String): Map<String, LocalizationInfo> {
        throw UnsupportedOperationException("Play Store IAP sync is not supported yet")
    }

    override suspend fun createLocalization(subscriptionId: String, locale: String, name: String, description: String) {
        throw UnsupportedOperationException("Play Store IAP sync is not supported yet")
    }

    override suspend fun updateLocalization(localizationId: String, name: String, description: String) {
        throw UnsupportedOperationException("Play Store IAP sync is not supported yet")
    }
}
