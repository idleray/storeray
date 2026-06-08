package com.rayject.storeray.provider.appstore

import com.rayject.storeray.config.StoreConfig
import com.rayject.storeray.provider.AppInfoService
import com.rayject.storeray.provider.IapService
import com.rayject.storeray.provider.Platform
import com.rayject.storeray.provider.ReleaseNotesService
import com.rayject.storeray.provider.StoreProvider
import com.rayject.storeray.provider.appstore.api.AppStoreConnectApi
import com.rayject.storeray.provider.appstore.jwt.JwtSigner

class AppStoreProvider(config: StoreConfig) : StoreProvider {
    override val platform: Platform = Platform.APP_STORE

    private val api: AppStoreConnectApi

    init {
        // 构造时生成 Token 并初始化 API 客户端
        val token = JwtSigner.generateToken(
            keyId = config.keyId,
            issuerId = config.issuerId,
            keyFilePath = config.keyFilePath,
            expirationMinutes = 20
        )
        api = AppStoreConnectApi(token)
    }

    // 延迟初始化具体的 Service
    private val releaseNotesService by lazy { AppStoreReleaseNotesService(api, config.bundleId) }
    private val iapService by lazy { AppStoreIapService(api, config.bundleId) }
    private val appInfoService by lazy { AppStoreAppInfoService(api, config.bundleId) }

    override fun releaseNotes(track: String): ReleaseNotesService = releaseNotesService

    override fun iap(): IapService = iapService

    override fun appInfo(): AppInfoService = appInfoService
}
