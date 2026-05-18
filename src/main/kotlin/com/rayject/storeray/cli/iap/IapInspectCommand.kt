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
    override fun help(context: com.github.ajalt.clikt.core.Context) = "Inspect details and online localizations of a single IAP product"

    private val productId by argument(help = "Product ID (e.g., com.rayject.fluente.xxx)")
    
    private val platform by option("-p", "--platform", help = "Target store platform (appstore, playstore)").default("appstore")
    
    override fun run() = runBlocking {
        try {
            val dir = com.rayject.storeray.cli.GlobalState.workspaceDir
            val storeConfig = ConfigLoader.loadStoreConfig("$dir/storeray.json")
            
            val platformEnum = when (platform.lowercase()) {
                "appstore" -> Platform.APP_STORE
                "playstore" -> throw UnsupportedOperationException("Play Store is not supported yet")
                else -> throw IllegalArgumentException("Unknown platform: $platform")
            }
            
            val provider = StoreProviderFactory.create(platformEnum, storeConfig)
            val iapService = provider.iap()
            
            Console.info("Fetching remote data...")
            val subscriptions = iapService.fetchSubscriptions()
            val subscription = subscriptions.find { it.productId == productId }
            
            if (subscription == null) {
                Console.error("Product not found: $productId or it is not a subscription type")
                return@runBlocking
            }
            
            Console.step("Product Information")
            Console.detail("ID:          ${subscription.productId}")
            Console.detail("ASC Internal ID: ${subscription.id}")
            Console.detail("Reference Name:    ${subscription.referenceName}")
            Console.detail("Current State:    ${subscription.state}")
            
            Console.step("Online Localizations")
            val localizations = iapService.fetchLocalizations(subscription.id)
            if (localizations.isEmpty()) {
                Console.detail("(No localization config found)")
            } else {
                for ((locale, info) in localizations) {
                    Console.info("🌐 $locale (ID: ${info.id})")
                    Console.detail("Name: ${info.name}")
                    Console.detail("Description: ${info.description}")
                    println()
                }
            }
            
        } catch (e: Exception) {
            Console.error("Execution failed: ${e.message}")
        }
    }
}
