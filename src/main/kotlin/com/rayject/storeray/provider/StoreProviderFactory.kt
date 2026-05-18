package com.rayject.storeray.provider

import com.rayject.storeray.config.StoreConfig
import com.rayject.storeray.config.WorkspaceConfig
import com.rayject.storeray.provider.appstore.AppStoreProvider
import com.rayject.storeray.provider.playstore.PlayStoreProvider

object StoreProviderFactory {
    fun create(platform: Platform, config: WorkspaceConfig): StoreProvider = when (platform) {
        Platform.APP_STORE -> AppStoreProvider(
            config.appStore ?: throw IllegalArgumentException("配置中缺少 app_store 部分")
        )
        Platform.PLAY_STORE -> PlayStoreProvider(
            config.playStore ?: throw IllegalArgumentException("配置中缺少 play_store 部分")
        )
    }

    fun create(platform: Platform, config: StoreConfig): StoreProvider = when (platform) {
        Platform.APP_STORE -> AppStoreProvider(config)
        Platform.PLAY_STORE -> throw IllegalArgumentException("Play Store requires play_store config")
    }
}
