package com.rayject.storeray.provider

import com.rayject.storeray.config.StoreConfig
import com.rayject.storeray.provider.appstore.AppStoreProvider

object StoreProviderFactory {
    fun create(platform: Platform, config: StoreConfig): StoreProvider = when (platform) {
        Platform.APP_STORE -> AppStoreProvider(config)
        Platform.PLAY_STORE -> TODO("Play Store 支持待实现")
    }
}
