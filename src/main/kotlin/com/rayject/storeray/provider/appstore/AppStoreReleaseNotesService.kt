package com.rayject.storeray.provider.appstore

import com.rayject.storeray.provider.ReleaseNotesService
import com.rayject.storeray.provider.appstore.api.AppStoreConnectApi

class AppStoreReleaseNotesService(
    private val api: AppStoreConnectApi,
    private val bundleId: String
) : ReleaseNotesService {
    
    override suspend fun fetch(appVersion: String): Map<String, String> {
        TODO("Release Notes fetch 待实现")
    }

    override suspend fun update(appVersion: String, notes: Map<String, String>) {
        TODO("Release Notes update 待实现")
    }
}
