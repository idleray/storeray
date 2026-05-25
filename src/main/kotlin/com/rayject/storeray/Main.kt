package com.rayject.storeray

import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.core.subcommands
import com.rayject.storeray.cli.InitCommand
import com.rayject.storeray.cli.StoreRay
import com.rayject.storeray.cli.appinfo.AppInfoCommand
import com.rayject.storeray.cli.appinfo.AppInfoFetchCommand
import com.rayject.storeray.cli.iap.IapCommand
import com.rayject.storeray.cli.iap.IapInspectCommand
import com.rayject.storeray.cli.iap.IapSyncCommand
import com.rayject.storeray.cli.releasenotes.ReleaseNotesCommand
import com.rayject.storeray.cli.releasenotes.ReleaseNotesUpdateCommand
import com.rayject.storeray.cli.releasenotes.PlayStoreTracksCommand

fun main(args: Array<String>) {
    StoreRay()
        .subcommands(
            InitCommand(),
            IapCommand().subcommands(
                IapSyncCommand(),
                IapInspectCommand()
            ),
            ReleaseNotesCommand().subcommands(
                ReleaseNotesUpdateCommand(),
                PlayStoreTracksCommand()
            ),
            AppInfoCommand().subcommands(
                AppInfoFetchCommand()
            )
        )
        .main(args)
}
