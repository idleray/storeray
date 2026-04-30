package com.rayject.storeray.cli.releasenotes

import com.github.ajalt.clikt.core.CliktCommand

class ReleaseNotesCommand : CliktCommand(
    name = "release-notes"
) {
    override fun help(context: com.github.ajalt.clikt.core.Context) = "Release Notes (版本更新日志) 管理工具"

    override fun run() {
        // 作为组命令，本身不执行逻辑
    }
}
