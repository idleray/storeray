package com.rayject.storeray.cli.appinfo

import com.github.ajalt.clikt.core.CliktCommand

class AppInfoCommand : CliktCommand(
    name = "appinfo"
) {
    override fun help(context: com.github.ajalt.clikt.core.Context) = "App Info metadata management tools"

    override fun run() {
    }
}
