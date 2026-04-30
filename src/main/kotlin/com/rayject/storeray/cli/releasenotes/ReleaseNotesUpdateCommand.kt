package com.rayject.storeray.cli.releasenotes

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.rayject.storeray.config.ConfigLoader
import com.rayject.storeray.provider.Platform
import com.rayject.storeray.provider.StoreProviderFactory
import com.rayject.storeray.usecase.SyncReleaseNotesUseCase
import kotlinx.coroutines.runBlocking

class ReleaseNotesUpdateCommand : CliktCommand(
    name = "update"
) {
    override fun help(context: com.github.ajalt.clikt.core.Context) = "更新指定版本的 Release Notes"

    private val appVersion by argument(help = "App Store 上的版本号 (如 1.2.0)")

    private val apply by option("--apply", help = "执行实际同步（默认是预览模式）").flag(default = false)
    
    private val platform by option("--platform", help = "目标商店平台 (appstore, playstore)").default("appstore")
    
    private val configPath by option("-c", "--config", help = "配置文件路径（默认: storeray.json）").default("storeray.json")
    
    private val productsPath by option("-p", "--products", help = "产品配置文件路径（默认: products.json）").default("products.json")
    
    private val dir by option("--dir", help = "本地化文本目录（默认: release_notes）").default("release_notes")

    override fun run() = runBlocking {
        try {
            val storeConfig = ConfigLoader.loadStoreConfig(configPath)
            val productsConfig = ConfigLoader.loadProductsConfig(productsPath)
            
            val platformEnum = when (platform.lowercase()) {
                "appstore" -> Platform.APP_STORE
                "playstore" -> throw UnsupportedOperationException("暂不支持 Play Store")
                else -> throw IllegalArgumentException("未知的平台: $platform")
            }
            
            val provider = StoreProviderFactory.create(platformEnum, storeConfig)
            val useCase = SyncReleaseNotesUseCase(
                releaseNotesService = provider.releaseNotes(),
                productsConfig = productsConfig,
                releaseNotesDir = dir
            )
            
            useCase.execute(appVersion = appVersion, dryRun = !apply)
            
        } catch (e: Exception) {
            echo("❌ 错误: ${e.message}", err = true)
        }
    }
}
