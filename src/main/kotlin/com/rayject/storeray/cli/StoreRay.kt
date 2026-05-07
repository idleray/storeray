package com.rayject.storeray.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option

object GlobalState {
    var workspaceDir: String = "./storeray"
}

class StoreRay : CliktCommand() {
    override fun help(context: com.github.ajalt.clikt.core.Context) = "StoreRay — 轻量级 App Store Connect / Google Play CLI 工具"
    
    val dir by option("-d", "--dir", help = "工作区目录（默认: ./storeray）").default("./storeray")
    
    override fun run() {
        GlobalState.workspaceDir = dir
    }
}
