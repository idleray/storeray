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

### 1.3 非目标

- **不是 fastlane 的全面替代**：不做构建、签名、提审等 CI/CD 流程
- **不做截图管理**：截图、预览视频等媒体资源暂不涉及
- **不追求跨平台运行**：只在 JVM 上运行，不考虑 native 编译

### 1.4 命名

项目名 **`storeray`**。

---

## 2. MVP 功能范围

### 2.1 功能清单

| # | 功能 | 说明 | 来源 |
|---|------|------|------|
| 1 | **Release Notes 管理** | 更新 App Store Connect / Google Play Store 上指定版本的多语言更新说明 | 新功能 |
| 2 | **IAP 本地化同步** | 同步订阅产品的多语言名称和描述到 App Store Connect | 移植自 Ruby 脚本 |
| 3 | **App Info 管理** | 管理应用描述、名称、副标题、关键词等多语言元数据，支持从线上 fetch 到本地，以及比对后增量 sync | 新功能 |

### 2.2 MVP 限定

- **Release Notes 支持 App Store Connect 与 Google Play Store production release**
- **IAP 只实现 App Store Connect**，但架构预留 Play Store 扩展点
- **只支持订阅类 IAP**（与现有 Ruby 脚本一致）
- **App Info 以 App Store 为基准**，Play Store 仅补充其独有字段（如 promo video）
- **不做** 截图管理
- **不做** 构建和提审流程

### 2.3 CLI 命令帮助与说明

工具采用标准的命令行子命令架构。你可以通过 `--help` 随时查看各层级的可用参数和功能。

**1. 根命令 (storeray)**
```text
Usage: storeray [<options>] <command> [<args>]...

  StoreRay — A lightweight CLI for App Store Connect & Google Play

Options:
  -d, --dir=<text>  Workspace directory (default: ./storeray)
  -h, --help        Show this message and exit

Commands:
  init           Initialize storeray workspace with templates
  iap            IAP (In-App Purchase) management tools
  release-notes  Release Notes management tools [alias: rn]
```

**2. 初始化命令 (init)**
```text
Usage: storeray init [<options>]

  Initialize storeray workspace with templates

Options:
  -h, --help  Show this message and exit
```

**3. IAP 同步命令 (iap sync)**
```text
Usage: storeray iap sync [<options>]

  Sync IAP localization metadata

Options:
  --apply                Apply changes to the store (default: dry-run)
  -p, --platform=<text>  Target store platform (appstore, playstore)
  -h, --help             Show this message and exit
```

**4. IAP 查看命令 (iap inspect)**
```text
Usage: storeray iap inspect [<options>] <product_id>

  Inspect details and online localizations of a single IAP product

Options:
  -p, --platform=<text>  Target store platform (appstore, playstore)
  -h, --help             Show this message and exit
```

**5. App Info 拉取命令 (appinfo fetch)**
```text
Usage: storeray appinfo fetch [<options>]

  Fetch app info metadata from store to local

Options:
  --force                Overwrite local data even if remote fields are empty
  -p, --platform=<text>  Target store platform (appstore, playstore). If omitted, runs both in order.
  -h, --help             Show this message and exit
```

> **说明**：App Store 为数据基准，拉取全部字段（name, subtitle, description, keywords, promotionalText 等）；Play Store 仅补充 App Store 没有的字段（如 `video`）。不传 `-p` 时依次执行两个平台。默认仅合并差异（已有非空字段不被覆盖），加 `--force` 则以远程数据完全覆盖本地。

**6. App Info 同步命令 (appinfo sync)**
```text
Usage: storeray appinfo sync [<options>]

  Sync app info metadata to store (compare local vs remote, then create/update)

Options:
  --apply                Apply changes to the store (default: dry-run)
  -p, --platform=<text>  Target store platform (appstore, playstore). If omitted, runs both in order.
  -h, --help             Show this message and exit
```

> **说明**：sync 会逐 locale 比对本地与远程数据，显示每个字段的差异。新增 locale 自动 CREATE，已有字段变更执行 UPDATE，内容一致的跳过。Play Store 只支持 name/subtitle/description/video 四个字段。

**7. Release Notes 更新命令 (release-notes update)**
```text
Usage: storeray release-notes update [<options>]

  Update Release Notes for the pending version

Options:
  --apply                Apply changes to the store (default: dry-run)
  -p, --platform=<text>  Target store platform (appstore, playstore). If omitted, runs both in order.
  -h, --help             Show this message and exit
```

> **说明**：不指定 `--platform` 时会依次执行 App Store Connect 和 Google Play Store。App Store Connect 会自动检测处于 `PREPARE_FOR_SUBMISSION` 状态的版本，并以该版本号去 `metadata/release_notes/` 目录下匹配对应的 JSON 文件（如 `1.3.0.json`）。Google Play Store 只支持 `production` track 上的 `draft` 或 `completed` release，优先选择 `draft`；会从 release name 的 `version_code(version_name)` 或 `version_code (version_name)` 格式中解析 `version_name`，再匹配同名 JSON 文件。

**常用执行示例：**
```bash
# 预览 Release Notes 更新（默认依次执行 App Store Connect 和 Google Play Store）
storeray rn update
# 也可以使用完整命令：storeray release-notes update

# 确认并执行 Release Notes 更新
storeray rn update --apply

# 预览 Google Play production release 的 Release Notes 更新
storeray rn update --platform playstore

# 预览 IAP 同步变化（dry-run）
storeray iap sync --platform appstore

# 确认并执行 IAP 同步（带上 --apply）
storeray iap sync --platform appstore --apply

# 预览 App Info 同步（diff only）
storeray appinfo sync

# 确认并执行 App Info 同步
storeray appinfo sync --apply

# 只同步 App Store
storeray appinfo sync --apply -p appstore

# 查看线上指定产品的详细信息
storeray iap inspect --platform appstore com.rayject.fluente.subscription.monthly

# 指定工作区目录（多项目管理）
storeray iap sync -d /Users/xxx/StoreMetadata/App1
```

`storeray.json` 示例：

```json
{
  "app_store": {
    "key_id": "YOUR_KEY_ID_HERE",
    "issuer_id": "YOUR_ISSUER_ID_HERE",
    "key_file_path": "AuthKey_YOUR_KEY_ID_HERE.p8",
    "bundle_id": "com.example.app"
  },
  "play_store": {
    "service_account_json_path": "google-play-service-account.json",
    "package_name": "com.example.app"
  }
}
```

### 2.4 工作区与配置文件结构

工具采用**“工作区（Workspace）”**的设计理念。所有配置和业务数据被高度内聚在一个文件夹中。配置文件使用 JSON 格式，通过 `kotlinx.serialization` 原生解析。

```text
storeray/                      # 工具的工作区目录（可通过 --dir 参数指定位置）
├── storeray.json              # 全局配置（认证信息、App 信息等）
├── metadata/                  # 业务数据根目录
│   ├── iap/                   # IAP 翻译数据（自包含：产品属性 + 所有语言翻译）
│   │   ├── monthly.json
│   │   └── yearly.json
│   ├── app_info/              # App 元数据（每个语言一个子目录）
│   │   ├── en-US/
│   │   │   ├── info.json              # 短字段: name, subtitle, keywords, urls
│   │   │   ├── description.txt        # 长文本: description
│   │   │   └── promotional_text.txt   # 宣传文本: promotionalText
│   │   ├── zh-Hans/
│   │   │   └── ...
│   │   └── ja/
│   │       └── ...
│   └── release_notes/         # 版本更新说明（自包含：每个版本一个文件，包含所有语言）
│       ├── 1.1.0.json
│       └── 1.2.0.json
```

**设计特点：**
- **位置解耦**：工作区文件夹无需强制放在宿主工程代码中，可通过 `--dir` 指定任意路径，实现多项目集中管理或代码数据分离。
- **自包含实体**：每个 JSON 文件就是一个完整的逻辑实体（一个内购产品，或一个版本的更新说明），无需跨文件关联，易于查阅和自动化处理。
- **数据驱动同步**：工具直接根据 JSON 文件内的语言键值（如 `"en-US"`, `"zh-Hans"`）动态同步商店，无需全局维护固定的 locales 列表。
- **动态发现机制**：新增产品或新版本只需在 `metadata/` 对应层级下添加 JSON 文件，工具自动扫描读取，无需维护额外的索引清单。
- **长短分离**：`app_info/` 按语言分子目录，短字段（name, subtitle, keywords, URLs）放在 `info.json` 中，长文本（description, promotionalText）分别放在独立的 `.txt` 文件中，便于 diff 和人工编辑。

#### App Info 数据设计原则

**1. 按语言分目录，长短文本分离**

每个语言一个目录，三种文件类型：

| 文件 | 用途 | 包含字段 | 理由 |
|------|------|----------|------|
| `info.json` | 结构化短字段 | name, subtitle, keywords, supportUrl, marketingUrl, privacyPolicyUrl, video | 字段少且结构固定，JSON 一目了然 |
| `description.txt` | 纯文本长描述 | description（4000 字符） | 可分段、可换行，diff 干净，不会被 JSON 转义污染 |
| `promotional_text.txt` | 纯文本宣传语 | promotionalText（170 字符） | 同上，语义上与 description 同属"长文本" |

分界原则：**500 字符以上或需要分段换行的文本**走 `.txt`，否则留 `info.json`。

**2. App Store locale 为统一主键**

本地目录名（如 `en-US/`、`zh-Hans/`）使用 App Store Connect locale shortcode。Play Store 的 BCP-47 locale 通过 `PlayStoreLocaleMapper` 双向映射。所有本地数据以 App Store locale 为主键存储。

**3. 以 App Store 为数据基准，Play Store 为补充**

两个平台的数据字段对比：

| 本地字段 | App Store 来源 | Play Store 来源 | 说明 |
|----------|---------------|----------------|------|
| name | `AppInfoLocalization.name` | `Listing.title` | 以 App Store 为准 |
| subtitle | `AppInfoLocalization.subtitle` | `Listing.shortDescription` | 以 App Store 为准 |
| keywords | `AppStoreVersionLocalization.keywords` | — | Play Store 无此概念 |
| description | `AppStoreVersionLocalization.description` | `Listing.fullDescription` | 以 App Store 为准 |
| promotionalText | `AppStoreVersionLocalization.promotionalText` | — | Play Store 无此概念 |
| supportUrl | `AppStoreVersionLocalization.supportUrl` | — | Play Store 无此概念 |
| marketingUrl | `AppStoreVersionLocalization.marketingUrl` | — | Play Store 无此概念 |
| privacyPolicyUrl | `AppInfoLocalization.privacyPolicyUrl` | — | Play Store 无此概念 |
| video | — | `Listing.video` | App Store 无此概念，唯一下游补充字段 |

`fetch -p playstore` 时只返回 `video` 字段，其余字段留空。本地 `AppInfoLoader.save()` 在写入时执行 merge：**新数据非空字段覆盖已有，空字段保留已有数据**，确保 Play Store 不会清空 App Store 的字段。

**4. 新增语言只需创建目录**

新增一种语言支持只需：
```bash
mkdir -p app_info/ko-KR
# 创建 info.json、description.txt、promotional_text.txt
```
工具自动发现并同步，无需维护索引清单。

**5. info.json 字段定义**

```json
{
  "name": "Fluente",
  "subtitle": "Learn Languages",
  "keywords": "language,learning,spanish",
  "supportUrl": "https://support.fluente.app",
  "marketingUrl": "https://fluente.app",
  "privacyPolicyUrl": "https://fluente.app/privacy",
  "video": "https://youtu.be/xxx"
}
```

所有字段可选，空字段会被忽略（不写入 `.txt` 文件；已存在的 `.txt` 文件会被删除）。

**6. sync 流程（差异比较后增量更新）**

`appinfo sync` 的执行流程：

```
                          ┌─────────────────────┐
                          │  读取本地 app_info/  │
                          │  每 locale 一组数据  │
                          └─────────┬───────────┘
                                    │
                          ┌─────────▼───────────┐
                          │  fetch 远程数据      │
                          │  (同时缓存 locale →  │
                          │   localization ID)   │
                          └─────────┬───────────┘
                                    │ 逐 locale 比较
                          ┌─────────▼───────────┐
                          │  差异比较             │
                          │  ┌────────────────┐  │
                          │  │ 本地有,远程无    │→→ CREATE
                          │  │ 本地有,远程有,   │
                          │  │ 内容不同         │→→ UPDATE
                          │  │ 本地有,远程有,   │
                          │  │ 内容相同         │→→ SKIP
                          │  │ 本地无,远程有    │→→ SKIP(不删除)
                          │  └────────────────┘  │
                          └─────────┬───────────┘
                                    │
                    ┌───────────────┴───────────────┐
                    │  dry-run?                      │
                    │  ├─ Yes → 打印差异，停止       │
                    │  └─ No  → 调用 update()        │
                    │          ├─ CREATE(新 locale)  │
                    │          └─ UPDATE(已有 locale)│
                    └───────────────────────────────┘
```

App Store 端会同时更新两个资源：
- `AppInfoLocalization`：name, subtitle, privacyPolicyUrl
- `AppStoreVersionLocalization`：description, keywords, marketingUrl, promotionalText, supportUrl

Play Store 端通过 `edits().listings().update()` 更新，支持 title(name)、shortDescription(subtitle)、fullDescription(description)、video 四个字段。

#### Release Notes 语言代码映射

本地 `metadata/release_notes/{version}.json` 以 App Store Connect 的 locale shortcode 为主。Google Play 使用 BCP-47 language tag，因此 Play Store Provider 在读取、缺失语言检查和写入时会做映射。

映射依据：
- App Store Connect：`Managing metadata in your app by using locale shortcodes`
- Google Play：Play Console `Translate and localize your app` 中的 store listing translation language code

| App Store | Google Play |
|---|---|
| ar-SA | ar |
| ca | ca |
| zh-Hans | zh-CN |
| zh-Hant | zh-HK, zh-TW |
| hr | hr |
| cs | cs-CZ |
| da | da-DK |
| nl-NL | nl-NL |
| en-AU | en-AU |
| en-CA | en-CA |
| en-GB | en-GB |
| en-US | en-US |
| fi | fi-FI |
| fr-FR | fr-FR |
| fr-CA | fr-CA |
| de-DE | de-DE |
| el | el-GR |
| he | iw-IL |
| hi | hi-IN |
| hu | hu-HU |
| id | id |
| it | it-IT |
| ja | ja-JP |
| ko | ko-KR |
| ms | ms |
| no | no-NO |
| pl | pl-PL |
| pt-BR | pt-BR |
| pt-PT | pt-PT |
| ro | ro |
| ru | ru-RU |
| sk | sk |
| es-MX | es-419 |
| es-ES | es-ES |
| sv | sv-SE |
| th | th |
| tr | tr-TR |
| uk | uk |
| vi | vi |

---

## 3. 技术选型

### 3.1 运行时与构建

| 项目 | 选择 | 理由 |
|------|------|------|
| **语言** | Kotlin (JVM) | 生态成熟，Kotlin 开发者可直接上手 |
| **构建** | Gradle (独立项目) | 完全独立的 Gradle 工程，不依赖其他项目 |
| **分发** | Fat JAR | 通过 `java -jar storeray.jar` 运行 |

### 3.2 核心依赖

| 库 | 用途 | 备注 |
|----|------|------|
| **[Clikt](https://ajalt.github.io/clikt/)** | CLI 参数解析框架 | 5.x，Kotlin 社区标准 CLI 框架 |
| **[Ktor Client (CIO)](https://ktor.io/)** | HTTP 客户端 | 使用最新稳定版 |
| **[kotlinx.serialization](https://github.com/Kotlin/kotlinx.serialization)** | JSON 序列化/反序列化 | 同时用于配置解析和 API 通信 |
| **Google APIs Client Library for Java** | Google Play Developer API | 用于 Play Store 认证与 Android Publisher API 调用 |
| **JWT 签名** | ES256 Token 生成 | 方案见下文讨论 |
| **[JUnit 5](https://junit.org/junit5/) + [MockK](https://mockk.io/)** | 测试框架 | 单元测试与 Mock |

#### JWT 方案选择

App Store Connect API 认证需要 ES256 签名的 JWT Token。有以下方案：

| 方案 | 说明 | 优劣 |
|------|------|------|
| **JDK 标准库手写** | 用 `java.security` 直接签名，约 30 行代码 | 零依赖，但需自行维护 |
| **[jwt-kt](https://github.com/nicosantos/jwt-kt)** | Kotlin Multiplatform JWT 库 | Kotlin 风格，但引入额外依赖 |
| **[java-jwt](https://github.com/auth0/java-jwt)** | Auth0 出品，Java 库 | 成熟稳定，但非 Kotlin 原生 |

> 推荐：JWT 签名逻辑简单（仅生成 Token，不验证），优先考虑 **JDK 标准库手写**以避免额外依赖。

### 3.3 为什么选择这些库

- **Clikt**：Kotlin 社区最流行的 CLI 框架，API 简洁，支持子命令、帮助文档自动生成。JetBrains 的 `kotlinx-cli` 已不活跃，Clikt 是唯一成熟选择
- **Ktor Client**：轻量且协程友好，与 kotlinx.serialization 无缝集成
- **kotlinx.serialization (JSON)**：配置文件和 API 响应统一使用 JSON，一个库覆盖所有序列化需求。原本考虑用 kaml 支持 YAML 配置，但 kaml 已被作者 archived 不再维护，因此配置格式统一为 JSON

---

## 4. 架构设计

### 4.1 分层架构

```
┌─────────────────────────────────────────────────┐
│                   CLI Layer                      │
│         (Clikt Commands & Subcommands)           │
│   StoreRay → ReleaseNotesCmd / IapCmd / AppInfo  │
└──────────────────────┬──────────────────────────┘
                        │ 调用
┌──────────────────────▼──────────────────────────┐
│                 UseCase Layer                    │
│         (业务逻辑编排，平台无关)                    │
│   SyncIapUseCase / SyncReleaseNotesUseCase       │
│   → AppInfo 暂未抽象 UseCase (fetch 直接调用)     │
└──────────────────────┬──────────────────────────┘
                        │ 依赖接口
┌──────────────────────▼──────────────────────────┐
│               Provider Layer                     │
│          (商店平台抽象 + 具体实现)                   │
│                                                  │
│  ┌─────────────┐          ┌─────────────────┐   │
│  │ StoreProvider│◄─────────│AppStoreProvider │   │
│  │  (interface) │          │  ├─ api/        │   │
│  │              │          │  └─ jwt/        │   │
│  │              │          └─────────────────┘   │
│  │              │          ┌─────────────────┐   │
│  │              │◄─────────│PlayStoreProvider│   │
│  └─────────────┘          │  + app info      │   │
│                            └─────────────────┘   │
└─────────────────────────────────────────────────┘
```

API 客户端和 JWT 认证作为各 Provider 的**内部实现细节**，不单独成层。

### 4.2 核心模型与接口

#### 领域模型

```kotlin
data class Subscription(
    val id: String,                    // App Store Connect 内部 ID
    val productId: String,             // 产品标识符 (如 com.rayject.fluente.xxx)
    val referenceName: String,         // 引用名称
    val state: String                  // 状态
)

data class LocalizationInfo(
    val id: String,                    // 本地化记录 ID（用于更新）
    val locale: String,                // 语言代码 (如 en-US)
    val name: String,                  // 显示名称
    val description: String            // 描述
)

data class AppInfoData(
    val name: String = "",
    val subtitle: String = "",
    val keywords: String = "",
    val supportUrl: String = "",
    val marketingUrl: String = "",
    val privacyPolicyUrl: String = "",
    val video: String = "",
    val description: String = "",
    val promotionalText: String = ""
)
```

#### Provider 接口

```kotlin
interface StoreProvider {
    val platform: Platform
    fun releaseNotes(): ReleaseNotesService
    fun iap(): IapService
    fun appInfo(): AppInfoService
}

interface ReleaseNotesService {
    /** 获取可编辑版本号 */
    suspend fun fetchEditableVersion(): String

    /** 获取指定版本当前的 release notes */
    suspend fun fetch(appVersion: String): Map<String, String>

    /** 更新指定版本的 release notes */
    suspend fun update(appVersion: String, notes: Map<String, String>)
}

interface IapService {
    /** 获取所有订阅产品 */
    suspend fun fetchSubscriptions(): List<Subscription>

    /** 获取指定产品的本地化 */
    suspend fun fetchLocalizations(subscriptionId: String): Map<String, LocalizationInfo>

    /** 创建本地化 */
    suspend fun createLocalization(subscriptionId: String, locale: String, name: String, description: String)

    /** 更新本地化 */
    suspend fun updateLocalization(localizationId: String, name: String, description: String)
}

interface AppInfoService {
    /** 从商店拉取所有语言的 App 元数据 */
    suspend fun fetch(): Map<String, AppInfoData>

    /** 更新指定语言的 App 元数据（不存在则创建，存在则更新） */
    suspend fun update(data: Map<String, AppInfoData>)
}

enum class Platform { APP_STORE, PLAY_STORE }
```

#### Provider 工厂

Provider 通过工厂方法创建，**构造时即完成认证**，确保实例创建后可直接使用：

```kotlin
object StoreProviderFactory {
    fun create(platform: Platform, config: WorkspaceConfig): StoreProvider = when (platform) {
        Platform.APP_STORE -> AppStoreProvider(config.appStore)
        Platform.PLAY_STORE -> PlayStoreProvider(config.playStore)
    }
}
```

### 4.3 接入新平台的扩展方式

Play Store Release Notes 和 App Info 已实现。后续接入 Play Store IAP 时，需要：

1. 完成 `PlayStoreIapService : IapService`
2. 在 `iap` CLI 层开放 `--platform playstore`
3. 按 Google Play 商品模型处理订阅和本地化差异

UseCase 层的代码尽量不需要修改（视平台概念差异程度而定）。


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
│   │   ├── appinfo/
│   │   │   ├── AppInfoCommand.kt       # `appinfo` 子命令组
│   │   │   └── AppInfoFetchCommand.kt  # `appinfo fetch`
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
│   │   ├── SyncAppInfoUseCase.kt
│   │   └── UpdateReleaseNotesUseCase.kt
│   │
│   ├── provider/                       # Provider 层 —— 平台抽象
│   │   ├── StoreProvider.kt            # 接口定义
│   │   ├── StoreProviderFactory.kt     # Provider 工厂
│   │   ├── ReleaseNotesService.kt
│   │   ├── IapService.kt
│   │   ├── appstore/                   # App Store Connect 实现
│   │   │   ├── AppStoreProvider.kt
│   │   │   ├── AppStoreReleaseNotesService.kt
│   │   │   ├── AppStoreAppInfoService.kt
│   │   │   ├── AppStoreIapService.kt
│   │   │   ├── jwt/                    # JWT 签名实现
│   │   │   │   └── JwtSigner.kt
│   │   │   └── api/                    # 底层 API 封装
│   │   │       ├── AppStoreConnectApi.kt
│   │   │       └── model/              # API 响应模型
│   │   │
│   │   └── playstore/                  # Google Play Store 实现
│   │       ├── PlayStoreProvider.kt
│   │       ├── PlayStoreReleaseNotesService.kt
│   │       ├── PlayStoreAppInfoService.kt
│   │       ├── PlayStoreIapService.kt  # STUB
│   │       ├── PlayStoreLocaleMapper.kt
│   │       └── api/PlayStorePublisherApi.kt
│   │
│   ├── model/                          # 核心领域模型
│   │   ├── AppInfoData.kt
│   │   ├── Subscription.kt
│   │   └── LocalizationInfo.kt
│   │
│   ├── config/                         # 配置加载
│   │   ├── StoreConfig.kt              # 全局配置模型（纯认证/项目信息）
│   │   ├── IapProductConfig.kt         # IAP 产品自包含配置模型
│   │   ├── AppInfoLoader.kt            # app_info/ 目录读写（merge 策略）
│   │   └── ConfigLoader.kt             # JSON 目录加载器
│   │
│   └── util/                           # 工具类
│       └── Console.kt                  # 终端输出美化 (emoji, 颜色)
│
├── src/test/kotlin/                    # 单元测试
│
└── docs/
    └── DESIGN.md                       # 本文档
```

### 4.5 关键设计决策

| 决策 | 选择 | 考量 |
|------|------|------|
| **配置文件格式** | 全部使用 JSON | 依赖 kotlinx.serialization，零额外依赖，方便与现有的本地化 JSON 文件统一 |
| **Provider 创建** | 工厂模式 + 构造时认证 | 避免使用者忘记调用 `connect()`，保证 Provider 实例的可用性 |
| **DI 方式** | 手动依赖注入 | CLI 工具生命周期短，结构简单，无需引入 Koin/Kodein 增加体积 |
| **Token 刷新** | 暂不处理 | App Store JWT 有效期 20 分钟，足够覆盖一次典型的同步任务 |
| **并发策略** | 协程并行获取数据，串行提交修改 | 兼顾执行效率与避免触发 App Store Connect API 的速率限制 |
| **幂等性** | 差量更新 (Diff-based) | `iap sync` 会先获取远端数据进行对比，只提交变更，重复执行安全 |
| **App Info 存储** | 按语言分子目录，长短分离 | 短字段 JSON、长文本 .txt，便于 diff 和手动编辑 |
| **App Info 数据基准** | App Store 为基准，Play Store 补充 | App Store 覆盖全部字段，Play Store 仅贡献 `video` 字段 |
| **跨平台 fetch 合并** | 后写入不覆盖已有非空字段 | `AppInfoLoader.save()` 读取已有数据做 merge，第二个平台不会清空前一个平台的数据 |
| **错误处理** | 自定义异常 + `PrintMessage` | CLI 工具需要友好的终端错误输出，避免直接抛出 StackTrace |
