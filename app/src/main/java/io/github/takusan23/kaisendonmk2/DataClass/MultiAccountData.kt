package io.github.takusan23.kaisendonmk2.DataClass

import io.github.takusan23.kaisendonmk2.MisskeyDataClass.MisskeyUserData

data class MultiAccountData(
    val service: String = "mastodon",
    val accountData: AccountData? = null,        // Mastodonなら入れて 。Misskeyならnullで
    val misskeyUserData: MisskeyUserData? = null // Misskeyなら入れて。Mastodonならnullで
)