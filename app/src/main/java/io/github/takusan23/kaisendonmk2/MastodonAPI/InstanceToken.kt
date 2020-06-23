package io.github.takusan23.kaisendonmk2.MastodonAPI

import io.github.takusan23.kaisendonmk2.DetaBase.Entity.CustomTimeLineDBEntity
import java.io.Serializable

/**
 * インスタンス、アクセストークンを保持するクラス。将来的にマストドン以外も・・？
 * タイムライン叩く関数とかも引数を短縮する目的でまとめる
 * */
data class InstanceToken(val instance: String, val token: String, val service: String = "mastodon") : Serializable

/**
 * CustomTimeLineDBEntityからInstanceTokenを生成する拡張関数
 * */
internal fun CustomTimeLineDBEntity.createInstanceToken() = InstanceToken(this.instance, this.token, this.service)