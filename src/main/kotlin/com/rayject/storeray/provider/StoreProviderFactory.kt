package com.rayject.storeray.provider

import com.rayject.storeray.config.StoreConfig

object StoreProviderFactory {
    fun create(platform: Platform, config: StoreConfig): StoreProvider = when (platform) {
        Platform.APP_STORE -> TODO("AppStoreProvider 待实现")
        Platform.PLAY_STORE -> TODO("Play Store 支持待实现")
    }
}
