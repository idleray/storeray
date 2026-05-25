package com.rayject.storeray.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.rayject.storeray.util.Console
import java.io.File

class InitCommand : CliktCommand(
    name = "init"
) {
    override fun help(context: com.github.ajalt.clikt.core.Context) = "Initialize storeray workspace with templates"

    override fun run() {
        val dirPath = GlobalState.workspaceDir
        val workspaceDir = File(dirPath)

        Console.step("Initializing workspace at: ${workspaceDir.absolutePath}")

        if (!workspaceDir.exists()) {
            if (workspaceDir.mkdirs()) {
                Console.success("Created workspace directory: $dirPath")
            } else {
                Console.error("Failed to create workspace directory: $dirPath")
                return
            }
        }

        // Create metadata directories
        val metadataDir = File(workspaceDir, "metadata")
        val iapDir = File(metadataDir, "iap")
        val releaseNotesDir = File(metadataDir, "release_notes")

        listOf(iapDir, releaseNotesDir).forEach {
            if (!it.exists()) {
                if (it.mkdirs()) {
                    Console.success("Created directory: ${it.relativeTo(workspaceDir)}")
                }
            }
        }

        // Create app_info directories
        val appInfoDir = File(metadataDir, "app_info")
        val enUsDir = File(appInfoDir, "en-US")
        val zhHansDir = File(appInfoDir, "zh-Hans")

        listOf(appInfoDir, enUsDir, zhHansDir).forEach {
            if (!it.exists()) {
                if (it.mkdirs()) {
                    Console.success("Created directory: ${it.relativeTo(workspaceDir)}")
                }
            }
        }

        // Create template files
        createFile(File(workspaceDir, "storeray.json"), STORERAY_JSON_TEMPLATE)
        createFile(File(iapDir, "monthly.json"), IAP_TEMPLATE)
        createFile(File(releaseNotesDir, "1.0.0.json"), RELEASE_NOTES_TEMPLATE)
        createFile(File(enUsDir, "info.json"), APP_INFO_EN_US_JSON_TEMPLATE)
        createFile(File(enUsDir, "description.txt"), APP_INFO_EN_US_DESC_TEMPLATE)
        createFile(File(enUsDir, "promotional_text.txt"), APP_INFO_EN_US_PROMO_TEMPLATE)
        createFile(File(zhHansDir, "info.json"), APP_INFO_ZH_HANS_JSON_TEMPLATE)
        createFile(File(zhHansDir, "description.txt"), APP_INFO_ZH_HANS_DESC_TEMPLATE)
        createFile(File(zhHansDir, "promotional_text.txt"), APP_INFO_ZH_HANS_PROMO_TEMPLATE)

        Console.divider()
        Console.success("Initialization complete!")
        Console.info("Next steps:")
        Console.detail("1. Edit storeray.json with your App Store Connect and Google Play credentials.")
        Console.detail("2. Add your IAP products in metadata/iap/.")
        Console.detail("3. Add release notes in metadata/release_notes/.")
        Console.detail("4. Add app info localizations in metadata/app_info/{locale}/.")
    }

    private fun createFile(file: File, content: String) {
        if (file.exists()) {
            Console.warning("File already exists, skipping: ${file.path}")
        } else {
            file.writeText(content.trimIndent() + "\n")
            Console.success("Created template file: ${file.name}")
        }
    }

    companion object {
        private const val STORERAY_JSON_TEMPLATE = """{
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
}"""

        private const val IAP_TEMPLATE = """{
  "product_id": "com.example.app.monthly",
  "reference_name": "Monthly Subscription",
  "localizations": {
    "en-US": {
      "name": "Monthly Premium",
      "description": "Unlock all features for 1 month"
    },
    "zh-Hans": {
      "name": "按月订阅",
      "description": "解锁所有高级功能，有效期1个月"
    }
  }
}"""

        private const val RELEASE_NOTES_TEMPLATE = """{
  "en-US": "Bug fixes and performance improvements.",
  "zh-Hans": "修复已知问题，提升性能。"
}"""

        private const val APP_INFO_EN_US_JSON_TEMPLATE = """{
  "name": "My App",
  "subtitle": "App subtitle",
  "keywords": "keyword1,keyword2,keyword3",
  "supportUrl": "https://example.com/support",
  "marketingUrl": "https://example.com",
  "privacyPolicyUrl": "https://example.com/privacy"
}"""

        private const val APP_INFO_EN_US_DESC_TEMPLATE = """Describe your app here. This is the primary description shown on your app's store page.

Highlight the key features and benefits of your app. Explain what makes your app unique and why users should download it.

- Feature 1: What does it do?
- Feature 2: How does it help users?
- Feature 3: What problem does it solve?"""

        private const val APP_INFO_ZH_HANS_JSON_TEMPLATE = """{
  "name": "我的应用",
  "subtitle": "应用副标题",
  "keywords": "关键词1,关键词2,关键词3",
  "supportUrl": "https://example.com/support",
  "marketingUrl": "https://example.com",
  "privacyPolicyUrl": "https://example.com/privacy"
}"""

        private const val APP_INFO_ZH_HANS_DESC_TEMPLATE = """在此处描述您的应用。这是在应用商店页面上显示的主要描述。

突出应用的主要功能和优势。说明您的应用的独特之处以及用户为什么应该下载它。

- 功能 1：它有什么作用？
- 功能 2：它如何帮助用户？
- 功能 3：它解决了什么问题？"""

        private const val APP_INFO_EN_US_PROMO_TEMPLATE = """Promotional text is shown above your app description on the App Store on iOS 11 and later, and Apple Watch."""

        private const val APP_INFO_ZH_HANS_PROMO_TEMPLATE = """推广文本将显示在 App Store 应用描述的上方（iOS 11 及更高版本以及 Apple Watch）。"""
    }
}
