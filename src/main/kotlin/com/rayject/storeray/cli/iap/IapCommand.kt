package com.rayject.storeray.cli.iap

import com.github.ajalt.clikt.core.CliktCommand

class IapCommand : CliktCommand(
    name = "iap",
    help = "IAP (App 内购买) 管理工具"
) {
    override fun run() {
        // 作为组命令，本身不执行逻辑
    }
}
