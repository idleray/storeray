package com.rayject.storeray.cli.releasenotes

import com.github.ajalt.clikt.core.CliktCommand

class ReleaseNotesCommand : CliktCommand(
    name = "release-notes"
) {
    override fun help(context: com.github.ajalt.clikt.core.Context) = "Release Notes management tools"

    override fun run() {
        // 作为组命令，本身不执行逻辑
    }
}
