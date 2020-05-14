package io.github.takusan23.kaisendonmk2.MastodonAPI

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response

/**
 * アカウント関係
 * */
class AccountAPI(val instanceToken: InstanceToken) {

    // 共通URL
    private val BASE_URL = "https://${instanceToken.instance}/api/v1/accounts"

    private val USER_AGENT = "KaisendonMk2;@takusan_23"


    /**
     * 自分のアカウントを取得する。コルーチンです。
     * */
    fun getVerifyCredentials(): Deferred<Response> = GlobalScope.async {
        val request = Request.Builder().apply {
            url("$BASE_URL/verify_credentials?access_token=${instanceToken.token}")
            header("User-Agent", USER_AGENT)
            get()
        }.build()
        val okHttpClient = OkHttpClient()
        val response = okHttpClient.newCall(request).execute()
        return@async response
    }

}