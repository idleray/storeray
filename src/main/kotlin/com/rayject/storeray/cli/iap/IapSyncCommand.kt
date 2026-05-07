package com.rayject.storeray.cli.iap

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.rayject.storeray.config.ConfigLoader
import com.rayject.storeray.provider.Platform
import com.rayject.storeray.provider.StoreProviderFactory
import com.rayject.storeray.usecase.SyncIapUseCase
import kotlinx.coroutines.runBlocking

class IapSyncCommand : CliktCommand(
    name = "sync"
) {
    override fun help(context: com.github.ajalt.clikt.core.Context) = "同步 IAP 的多语言本地化文案"

    private val apply by option("--apply", help = "执行实际同步（默认是预览模式）").flag(default = false)
    
    private val platform by option("--platform", help = "目标商店平台 (appstore, playstore)").default("appstore")
    
    override fun run() = runBlocking {
        try {
            val dir = com.rayject.storeray.cli.GlobalState.workspaceDir
            val storeConfig = ConfigLoader.loadStoreConfig("$dir/storeray.json")
            val products = ConfigLoader.loadIapProducts("$dir/metadata/iap")
            
            val platformEnum = when (platform.lowercase()) {
                "appstore" -> Platform.APP_STORE
                "playstore" -> throw UnsupportedOperationException("暂不支持 Play Store")
                else -> throw IllegalArgumentException("未知的平台: $platform")
            }
            
            val provider = StoreProviderFactory.create(platformEnum, storeConfig)
            val useCase = SyncIapUseCase(
                iapService = provider.iap(),
                products = products
            )
            
            useCase.execute(dryRun = !apply)
            
        } catch (e: Exception) {
            echo("❌ 错误: ${e.message}", err = true)
            // if (debug) e.printStackTrace() 也可以在这里添加 debug 支持
        }
    }
}
