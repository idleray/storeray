package com.rayject.storeray.provider.playstore

import com.rayject.storeray.config.PlayStoreConfig
import com.rayject.storeray.provider.AppInfoService
import com.rayject.storeray.provider.IapService
import com.rayject.storeray.provider.Platform
import com.rayject.storeray.provider.ReleaseNotesService
import com.rayject.storeray.provider.StoreProvider
import com.rayject.storeray.provider.playstore.api.PlayStorePublisherApi

class PlayStoreProvider(config: PlayStoreConfig) : StoreProvider {
    override val platform: Platform = Platform.PLAY_STORE

    private val api = PlayStorePublisherApi(
        serviceAccountJsonPath = config.serviceAccountJsonPath,
        packageName = config.packageName
    )

    private val releaseNotesService by lazy {
        PlayStoreReleaseNotesService(api)
    }

    private val iapService by lazy {
        PlayStoreIapService()
    }

    private val appInfoService by lazy {
        PlayStoreAppInfoService(api)
    }

    override fun releaseNotes(): ReleaseNotesService = releaseNotesService

    override fun iap(): IapService = iapService

    override fun appInfo(): AppInfoService = appInfoService
}
