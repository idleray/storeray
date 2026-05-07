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
        Console.step(if (dryRun) "预览模式（不会实际修改）" else "执行同步")

        if (products.isEmpty()) {
            Console.error("未在工作区找到任何 IAP 配置文件。请检查 metadata/iap 目录。")
            return
        }

        try {
            Console.info("正在获取远端订阅列表...")
            val subscriptions = iapService.fetchSubscriptions()
            Console.info("共找到 ${subscriptions.size} 个远端订阅产品")

            var totalChanges = 0
            var totalErrors = 0

            for (productConfig in products) {
                Console.divider()
                Console.info("处理: ${productConfig.referenceName} (ID: ${productConfig.productId})")

                val subscription = subscriptions.find { it.productId == productConfig.productId }
                if (subscription == null) {
                    Console.error("未在远端找到此产品或不是订阅类型")
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
                    Console.error("验证失败:")
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
            Console.info("同步摘要:")
            if (totalChanges == 0 && totalErrors == 0) {
                Console.success("所有产品已是最新状态")
            } else {
                Console.info("总变更数: $totalChanges")
                if (totalErrors > 0) Console.error("总错误数: $totalErrors")
            }

            if (dryRun && totalChanges > 0) {
                Console.info("💡 提示: 使用 --apply 参数执行实际同步")
            }
        } catch (e: Exception) {
            Console.error("同步过程中发生异常: ${e.message}")
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
                    Console.detail("[~] $locale: 更新本地化")
                    if (existing.name != locMap.name) Console.detail("    名称: \"${existing.name}\" → \"${locMap.name}\"")
                    if (existing.description != locMap.description) Console.detail("    描述: \"${existing.description}\" → \"${locMap.description}\"")
                    
                    changesCount++
                    if (!dryRun) {
                        try {
                            iapService.updateLocalization(existing.id, locMap.name, locMap.description)
                            Console.success("    更新成功")
                        } catch (e: Exception) {
                            Console.error("    更新失败: ${e.message}")
                            errorsCount++
                        }
                    }
                }
            } else {
                // 需要新建
                Console.detail("[+] $locale: 创建新本地化")
                Console.detail("    名称: ${locMap.name}")
                Console.detail("    描述: ${locMap.description}")
                
                changesCount++
                if (!dryRun) {
                    try {
                        iapService.createLocalization(subscriptionId, locale, locMap.name, locMap.description)
                        Console.success("    创建成功")
                    } catch (e: Exception) {
                        Console.error("    创建失败: ${e.message}")
                        errorsCount++
                    }
                }
            }
        }
        
        if (changesCount == 0) {
            Console.success("    无需更新")
        }
        
        return Pair(changesCount, errorsCount)
    }
}
