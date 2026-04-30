package com.rayject.storeray.cli.iap

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.rayject.storeray.config.ConfigLoader
import com.rayject.storeray.provider.Platform
import com.rayject.storeray.provider.StoreProviderFactory
import com.rayject.storeray.util.Console
import kotlinx.coroutines.runBlocking

class IapInspectCommand : CliktCommand(
    name = "inspect"
) {
    override fun help(context: com.github.ajalt.clikt.core.Context) = "查看单个 IAP 产品的详细信息和在线文案"

    private val productId by argument(help = "产品 ID (如 com.rayject.fluente.xxx)")
    
    private val platform by option("--platform", help = "目标商店平台 (appstore, playstore)").default("appstore")
    
    private val configPath by option("-c", "--config", help = "配置文件路径（默认: storeray.json）").default("storeray.json")

    override fun run() = runBlocking {
        try {
            val storeConfig = ConfigLoader.loadStoreConfig(configPath)
            
            val platformEnum = when (platform.lowercase()) {
                "appstore" -> Platform.APP_STORE
                "playstore" -> throw UnsupportedOperationException("暂不支持 Play Store")
                else -> throw IllegalArgumentException("未知的平台: $platform")
            }
            
            val provider = StoreProviderFactory.create(platformEnum, storeConfig)
            val iapService = provider.iap()
            
            Console.info("正在获取远端数据...")
            val subscriptions = iapService.fetchSubscriptions()
            val subscription = subscriptions.find { it.productId == productId }
            
            if (subscription == null) {
                Console.error("未找到产品: $productId 或该产品不是订阅类型")
                return@runBlocking
            }
            
            Console.step("产品信息")
            Console.detail("ID:          ${subscription.productId}")
            Console.detail("ASC 内部 ID: ${subscription.id}")
            Console.detail("参考名称:    ${subscription.referenceName}")
            Console.detail("当前状态:    ${subscription.state}")
            
            Console.step("线上本地化文案")
            val localizations = iapService.fetchLocalizations(subscription.id)
            if (localizations.isEmpty()) {
                Console.detail("(暂无本地化配置)")
            } else {
                for ((locale, info) in localizations) {
                    Console.info("🌐 $locale (ID: ${info.id})")
                    Console.detail("名称: ${info.name}")
                    Console.detail("描述: ${info.description}")
                    println()
                }
            }
            
        } catch (e: Exception) {
            Console.error("执行失败: ${e.message}")
        }
    }
}
