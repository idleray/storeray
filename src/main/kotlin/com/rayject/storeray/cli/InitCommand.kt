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

        // Create template files
        createFile(File(workspaceDir, "storeray.json"), STORERAY_JSON_TEMPLATE)
        createFile(File(iapDir, "monthly.json"), IAP_TEMPLATE)
        createFile(File(releaseNotesDir, "1.0.0.json"), RELEASE_NOTES_TEMPLATE)

        Console.divider()
        Console.success("Initialization complete!")
        Console.info("Next steps:")
        Console.detail("1. Edit storeray.json with your App Store Connect credentials.")
        Console.detail("2. Add your IAP products in metadata/iap/.")
        Console.detail("3. Add release notes in metadata/release_notes/.")
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
    }
}
