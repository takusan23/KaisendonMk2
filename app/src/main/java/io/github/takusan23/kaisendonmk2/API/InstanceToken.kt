package io.github.takusan23.kaisendonmk2.API

/**
 * インスタンス、アクセストークンを保持するクラス。将来的にマストドン以外も・・？
 * タイムライン叩く関数とかも引数を短縮する目的でまとめる
 * */
data class InstanceToken(val instance: String, val token: String, val service: String = "mastodon")