package io.github.takusan23.kaisendonmk2.DataClass

import io.github.takusan23.kaisendonmk2.MastodonAPI.InstanceToken
import io.github.takusan23.kaisendonmk2.MisskeyDataClass.MisskeyUserData

/**
 * AccountListAdapterに渡すデータクラス。
 * @param instanceToken ログイン情報
 * @param mastodonAccountData Mastodonの場合はデータクラスを入れてね。Misskeyの場合はnull
 * @param misskeyUserData Misskeyの場合はデータクラス入れてね。Mastodonの場合はnull
 * @param service mastodon か misskey
 * */
data class AccountListData(
    val instanceToken: InstanceToken,
    val mastodonAccountData: AccountData? = null,
    val misskeyUserData: MisskeyUserData? = null,
    val service: String = "mastodon"
)