package com.rayject.storeray.model

/**
 * 订阅产品信息
 */
data class Subscription(
    val id: String,                    // App Store Connect 内部 ID
    val productId: String,             // 产品标识符 (如 com.rayject.fluente.xxx)
    val referenceName: String,         // 引用名称
    val state: String                  // 状态
)
