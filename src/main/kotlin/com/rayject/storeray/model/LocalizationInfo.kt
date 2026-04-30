package com.rayject.storeray.model

/**
 * 订阅产品的本地化信息
 */
data class LocalizationInfo(
    val id: String,                    // 本地化记录 ID（用于更新）
    val locale: String,                // 语言代码 (如 en-US)
    val name: String,                  // 显示名称
    val description: String            // 描述
)
