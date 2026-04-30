package com.rayject.storeray

import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.core.subcommands
import com.rayject.storeray.cli.StoreRay
import com.rayject.storeray.cli.iap.IapCommand
import com.rayject.storeray.cli.iap.IapInspectCommand
import com.rayject.storeray.cli.iap.IapSyncCommand

fun main(args: Array<String>) {
    StoreRay()
        .subcommands(
            IapCommand().subcommands(
                IapSyncCommand(),
                IapInspectCommand()
            )
        )
        .main(args)
}
