package com.rayject.storeray.cli.iap

import com.github.ajalt.clikt.core.CliktCommand

class IapCommand : CliktCommand(
    name = "iap"
) {
    override fun help(context: com.github.ajalt.clikt.core.Context) = "IAP (In-App Purchase) management tools"

    override fun run() {
        // 作为组命令，本身不执行逻辑
    }
}
