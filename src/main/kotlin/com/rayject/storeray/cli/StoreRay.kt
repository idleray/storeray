package com.rayject.storeray.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.rayject.storeray.cli.iap.IapCommand

class StoreRay : CliktCommand(
    help = "StoreRay — 轻量级 App Store Connect / Google Play CLI 工具"
) {
    override fun run() {
        // 根命令不需要具体行为
    }
}
