package io.github.takusan23.kaisendonmk2.MastodonAPI

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response

/**
 * タイムライン取得関係。
 * コルーチンです
 * */
class TimeLineAPI(val instanceToken: InstanceToken) {

    private val BASE_URL = "https://${instanceToken.instance}/api/v1/timelines"

    /**
     * ローカルTLを取得する関数。コルーチンです。
     * */
    fun getLocalTimeLine(): Deferred<Response> =
        timeLine("$BASE_URL/public?access_token=${instanceToken.token}&limit=40&local=true")

    /**
     * ホームタイムラインを取得
     * */
    fun getHomeTimeLine(): Deferred<Response> =
        timeLine("$BASE_URL/home?access_token=${instanceToken.token}&limit=40")

    // 共通部分
    private fun timeLine(url: String): Deferred<Response> = GlobalScope.async {
        val request = Request.Builder().apply {
            url(url)
            header("User-Agent", "KaisendonMk2@takusan_23")
            get()
        }.build()
        val okHttpClient = OkHttpClient()
        val response = okHttpClient.newCall(request).execute()
        return@async response
    }

}