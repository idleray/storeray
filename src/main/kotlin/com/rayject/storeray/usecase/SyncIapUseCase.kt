package com.rayject.storeray.usecase

import com.rayject.storeray.config.IapProductConfig
import com.rayject.storeray.config.IapLocalizationConfig
import com.rayject.storeray.model.LocalizationInfo
import com.rayject.storeray.model.Subscription
import com.rayject.storeray.provider.IapService
import com.rayject.storeray.util.Console

class SyncIapUseCase(
    private val iapService: IapService,
    private val products: List<IapProductConfig>
) {

    suspend fun execute(dryRun: Boolean) {
        Console.step(if (dryRun) "Dry-run mode (no changes will be applied)" else "Executing sync")

        if (products.isEmpty()) {
            Console.error("No IAP config files found in the workspace. Please check the metadata/iap directory.")
            return
        }

        try {
            Console.info("Fetching remote subscription list...")
            val subscriptions = iapService.fetchSubscriptions()
            Console.info("Found ${subscriptions.size} remote subscription products")

            var totalChanges = 0
            var totalErrors = 0

            for (productConfig in products) {
                Console.divider()
                Console.info("Processing: ${productConfig.referenceName} (ID: ${productConfig.productId})")

                val subscription = subscriptions.find { it.productId == productConfig.productId }
                if (subscription == null) {
                    Console.error("Product not found on remote or it is not a subscription type")
                    totalErrors++
                    continue
                }

                Console.detail("Subscription ID: ${subscription.id}")

                val localizations = productConfig.localizations
                
                // 验证本地化文案的合法性
                val validationErrors = com.rayject.storeray.validator.LocalizationValidator.validate(
                    productId = productConfig.productId,
                    localizations = localizations
                )
                if (validationErrors.isNotEmpty()) {
                    Console.error("Validation failed:")
                    validationErrors.forEach { Console.detail("- $it") }
                    totalErrors += validationErrors.size
                    continue
                }

                // 获取远端现存的本地化信息
                val existingLocalizations = iapService.fetchLocalizations(subscription.id)

                // 比较并执行
                val (changes, errors) = syncProductLocalizations(
                    subscriptionId = subscription.id,
                    localizations = localizations,
                    existingLocalizations = existingLocalizations,
                    dryRun = dryRun
                )
                
                totalChanges += changes
                totalErrors += errors
            }

            Console.divider()
            Console.info("Sync Summary:")
            if (totalChanges == 0 && totalErrors == 0) {
                Console.success("All products are up to date")
            } else {
                Console.info("Total changes: $totalChanges")
                if (totalErrors > 0) Console.error("Total errors: $totalErrors")
            }

            if (dryRun && totalChanges > 0) {
                Console.info("💡 Hint: Use the --apply flag to execute the actual sync")
            }
        } catch (e: Exception) {
            Console.error("An error occurred during sync: ${e.message}")
            e.printStackTrace()
        }
    }

    private suspend fun syncProductLocalizations(
        subscriptionId: String,
        localizations: Map<String, IapLocalizationConfig>,
        existingLocalizations: Map<String, LocalizationInfo>,
        dryRun: Boolean
    ): Pair<Int, Int> { // Returns Pair<ChangesCount, ErrorsCount>
        var changesCount = 0
        var errorsCount = 0

        for ((locale, locMap) in localizations) {
            val existing = existingLocalizations[locale]

            if (existing != null) {
                // 判断是否需要更新
                if (existing.name != locMap.name || existing.description != locMap.description) {
                    Console.detail("[~] $locale: Updating localization")
                    if (existing.name != locMap.name) Console.detail("    Name: \"${existing.name}\" → \"${locMap.name}\"")
                    if (existing.description != locMap.description) Console.detail("    Description: \"${existing.description}\" → \"${locMap.description}\"")
                    
                    changesCount++
                    if (!dryRun) {
                        try {
                            iapService.updateLocalization(existing.id, locMap.name, locMap.description)
                            Console.success("    Update successful")
                        } catch (e: Exception) {
                            Console.error("    Update failed: ${e.message}")
                            errorsCount++
                        }
                    }
                }
            } else {
                // 需要新建
                Console.detail("[+] $locale: Creating new localization")
                Console.detail("    Name: ${locMap.name}")
                Console.detail("    Description: ${locMap.description}")
                
                changesCount++
                if (!dryRun) {
                    try {
                        iapService.createLocalization(subscriptionId, locale, locMap.name, locMap.description)
                        Console.success("    Creation successful")
                    } catch (e: Exception) {
                        Console.error("    Creation failed: ${e.message}")
                        errorsCount++
                    }
                }
            }
        }
        
        if (changesCount == 0) {
            Console.success("    No updates needed")
        }
        
        return Pair(changesCount, errorsCount)
    }
}
