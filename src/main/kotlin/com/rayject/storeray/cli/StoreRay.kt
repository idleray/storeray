package com.rayject.storeray.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option

object GlobalState {
    var workspaceDir: String = "./storeray"
}

class StoreRay : CliktCommand() {
    override fun help(context: com.github.ajalt.clikt.core.Context) = "StoreRay — A lightweight CLI for App Store Connect & Google Play"
    
    val dir by option("-d", "--dir", help = "Workspace directory (default: ./storeray)").default("./storeray")
    
    override fun run() {
        GlobalState.workspaceDir = dir
    }
}
