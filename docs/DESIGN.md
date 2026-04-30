# StoreRay — 技术设计文档

> 一个轻量级的 Kotlin CLI 工具，用于管理 App Store Connect 和 Google Play Store。

## 1. 项目概述

### 1.1 背景

移动应用发布到 App Store / Play Store 后，仍然需要频繁管理商店侧的内容：更新版本说明、同步 IAP 产品的多语言文案等。现有方案（如 fastlane）功能全面但过于庞大，而手写脚本（Ruby / Shell）又存在以下问题：

- **技术栈割裂**：Kotlin 项目却要维护 Ruby 脚本，上下文切换成本高
- **依赖管理分散**：脚本依赖 gem / pip 等独立包管理器，CI 环境配置复杂
- **扩展性差**：脚本通常只覆盖单一功能，新增场景需从零搭建
- **难以复用**：认证逻辑、API 封装散落在脚本中，无法被其他工具引用

### 1.2 目标

构建一个独立的、类似 fastlane 的轻量级 CLI 工具，用 Kotlin 实现：

1. **独立工具**：独立项目，可用于任意移动应用的商店管理
2. **可扩展架构**：通过 Provider 抽象，轻松接入不同商店平台
3. **CLI 优先**：命令行界面，方便本地使用和 CI/CD 集成
4. **渐进式开发**：MVP 只做最核心的两个功能，后续按需扩展

### 1.3 命名

项目名 **`storeray`**。

---

## 2. MVP 功能范围

### 2.1 功能清单

| # | 功能 | 说明 | 来源 |
|---|------|------|------|
| 1 | **Release Notes 管理** | 更新 App Store Connect 上指定版本的多语言更新说明 | 新功能 |
| 2 | **IAP 本地化同步** | 同步订阅产品的多语言名称和描述到 App Store Connect | 移植自 Ruby 脚本 |

### 2.2 MVP 限定

- **只实现 App Store Connect**，但架构预留 Play Store 扩展点
- **只支持订阅类 IAP**（与现有 Ruby 脚本一致）
- **不做** App 元数据管理（截图、描述、关键词等）
- **不做** 构建和提审流程

### 2.3 CLI 命令预览

```bash
# Release Notes —— 更新版本说明
storeray release-notes update --platform appstore --version 1.2.0

# IAP —— 同步订阅本地化
storeray iap sync                    # 预览模式（dry-run）
storeray iap sync --apply            # 执行同步
storeray iap inspect <product_id>    # 查看单个产品详情
```

### 2.4 配置文件结构（沿用现有格式）

```
storeray/
├── storeray.yaml               # 全局配置（认证、App 信息）
├── products.yaml               # IAP 产品定义
├── localizations/              # IAP 本地化文案 (JSON)
│   ├── monthly.json
│   └── yearly.json
└── release-notes/              # 版本更新说明
    └── 1.2.0/
        ├── en-US.txt
        ├── zh-Hans.txt
        └── zh-Hant.txt
```

---

## 3. 技术选型

### 3.1 运行时与构建

| 项目 | 选择 | 理由 |
|------|------|------|
| **语言** | Kotlin (JVM) | 生态成熟，Kotlin 开发者可直接上手 |
| **构建** | Gradle (独立项目) | 完全独立的 Gradle 工程，不依赖其他项目 |
| **分发** | Fat JAR → 可选 GraalVM native-image | 初期用 `java -jar`，后续可编译为原生二进制 |

### 3.2 核心依赖

| 库 | 用途 | 版本 |
|----|------|------|
| **[Clikt](https://ajalt.github.io/clikt/)** | CLI 参数解析框架 | 5.x |
| **[Ktor Client (CIO)](https://ktor.io/)** | HTTP 客户端 | 与主项目同版本 (3.4.0) |
| **[kotlinx.serialization](https://github.com/Kotlin/kotlinx.serialization)** | JSON 序列化 | 与主项目同版本 |
| **[kaml](https://github.com/charleskorn/kaml)** | YAML 解析（基于 kotlinx.serialization） | 0.67.x |
| **[java-jwt](https://github.com/auth0/java-jwt)** | JWT 签名（ES256） | 4.x |

### 3.3 为什么选择这些库

- **Clikt**：Kotlin 社区最流行的 CLI 框架，API 简洁，支持子命令、帮助文档自动生成
- **Ktor Client**：已在主项目中大量使用，团队熟悉，轻量且协程友好
- **kaml**：与 `kotlinx.serialization` 无缝集成，不需要额外的反射或注解处理
- **java-jwt**：Auth0 出品，API 简洁，ES256 开箱即用

---

## 4. 架构设计

### 4.1 分层架构

```
┌─────────────────────────────────────────────────┐
│                   CLI Layer                      │
│         (Clikt Commands & Subcommands)           │
│   StoreRay → ReleaseNotesCmd / IapCmd            │
└──────────────────────┬──────────────────────────┘
                       │ 调用
┌──────────────────────▼──────────────────────────┐
│                 UseCase Layer                    │
│         (业务逻辑编排，平台无关)                    │
│   SyncIapUseCase / UpdateReleaseNotesUseCase     │
└──────────────────────┬──────────────────────────┘
                       │ 依赖接口
┌──────────────────────▼──────────────────────────┐
│               Provider Layer                     │
│          (商店平台抽象 + 具体实现)                   │
│                                                  │
│  ┌─────────────┐          ┌─────────────────┐   │
│  │ StoreProvider│◄─────────│AppStoreProvider │   │
│  │  (interface) │          └─────────────────┘   │
│  │              │          ┌─────────────────┐   │
│  │              │◄─────────│PlayStoreProvider│   │
│  └─────────────┘          │   (future)      │   │
│                            └─────────────────┘   │
└──────────────────────┬──────────────────────────┘
                       │ 使用
┌──────────────────────▼──────────────────────────┐
│                  API Layer                       │
│        (底层 HTTP 客户端 & 认证)                   │
│   AppStoreConnectApi / JwtTokenGenerator         │
└─────────────────────────────────────────────────┘
```

### 4.2 Provider 抽象（核心扩展点）

Provider 是整个架构的核心抽象。每个商店平台实现同一组接口，UseCase 层只依赖接口，不感知具体平台：

```kotlin
// === 核心接口 ===

interface StoreProvider {
    val platform: Platform
    fun connect(config: StoreConfig)

    fun releaseNotes(): ReleaseNotesService
    fun iap(): IapService
}

interface ReleaseNotesService {
    /** 获取指定版本当前的 release notes */
    suspend fun fetch(appVersion: String): Map<Locale, String>

    /** 更新指定版本的 release notes */
    suspend fun update(appVersion: String, notes: Map<Locale, String>)
}

interface IapService {
    /** 获取所有订阅产品 */
    suspend fun fetchSubscriptions(): List<Subscription>

    /** 获取指定产品的本地化 */
    suspend fun fetchLocalizations(subscriptionId: String): Map<Locale, LocalizationInfo>

    /** 创建本地化 */
    suspend fun createLocalization(subscriptionId: String, locale: Locale, name: String, description: String)

    /** 更新本地化 */
    suspend fun updateLocalization(localizationId: String, name: String, description: String)
}

enum class Platform { APP_STORE, PLAY_STORE }
```

### 4.3 接入新平台的扩展方式

以后接入 Play Store 时，只需要：

1. 实现 `PlayStoreProvider : StoreProvider`
2. 实现 `PlayStoreReleaseNotesService : ReleaseNotesService`
3. 实现 `PlayStoreIapService : IapService`
4. 在 CLI 层注册新平台

**UseCase 和 CLI 层的代码完全不需要修改。**

### 4.4 项目结构

```
storeray/
├── build.gradle.kts                    # 构建配置
├── settings.gradle.kts
├── gradle/
│   └── libs.versions.toml              # 版本目录
│
├── src/main/kotlin/com/rayject/storeray/
│   │
│   ├── Main.kt                         # 入口 (main函数)
│   │
│   ├── cli/                            # CLI 层 —— Clikt 命令
│   │   ├── StoreRay.kt                 # 根命令
│   │   ├── iap/
│   │   │   ├── IapCommand.kt           # `iap` 子命令组
│   │   │   ├── IapSyncCommand.kt       # `iap sync`
│   │   │   └── IapInspectCommand.kt    # `iap inspect`
│   │   └── releasenotes/
│   │       ├── ReleaseNotesCommand.kt  # `release-notes` 子命令组
│   │       └── UpdateCommand.kt        # `release-notes update`
│   │
│   ├── usecase/                        # UseCase 层 —— 业务逻辑
│   │   ├── SyncIapUseCase.kt
│   │   └── UpdateReleaseNotesUseCase.kt
│   │
│   ├── provider/                       # Provider 层 —— 平台抽象
│   │   ├── StoreProvider.kt            # 接口定义
│   │   ├── ReleaseNotesService.kt
│   │   ├── IapService.kt
│   │   └── appstore/                   # App Store Connect 实现
│   │       ├── AppStoreProvider.kt
│   │       ├── AppStoreReleaseNotesService.kt
│   │       ├── AppStoreIapService.kt
│   │       └── api/                    # 底层 API 封装
│   │           ├── AppStoreConnectApi.kt
│   │           ├── JwtTokenGenerator.kt
│   │           └── model/              # API 响应模型
│   │               ├── AppResponse.kt
│   │               ├── SubscriptionResponse.kt
│   │               └── LocalizationResponse.kt
│   │
│   ├── config/                         # 配置加载
│   │   ├── StoreCliConfig.kt           # 全局配置模型
│   │   ├── ProductsConfig.kt           # 产品配置模型
│   │   └── ConfigLoader.kt            # YAML/JSON 加载器
│   │
│   ├── validator/                      # 校验逻辑
│   │   └── LocalizationValidator.kt
│   │
│   └── util/                           # 工具类
│       ├── Console.kt                  # 终端输出美化 (emoji, 颜色)
│       └── Locale.kt                   # Locale 类型定义
│
├── src/test/kotlin/                    # 单元测试
│
└── docs/
    └── DESIGN.md                       # 本文档
```

### 4.5 关键设计决策

| 决策 | 选择 | 考量 |
|------|------|------|
| UseCase 是否用协程 | 是，`suspend fun` | Ktor Client 天然协程，保持一致 |
| Provider 如何选择 | CLI 参数 `--platform` | 默认 `appstore`，未来支持 `playstore` |
| 配置文件格式 | YAML (全局/产品) + JSON (本地化) | 沿用现有 Ruby 脚本的格式，降低迁移成本 |
| 错误处理 | 自定义异常 + Clikt 的 `PrintMessage` | CLI 工具需要友好的错误输出，不能抛 stacktrace |
| 日志/输出 | 自定义 Console 工具类 | 带 emoji 和颜色，与现有 Ruby 脚本体验一致 |
