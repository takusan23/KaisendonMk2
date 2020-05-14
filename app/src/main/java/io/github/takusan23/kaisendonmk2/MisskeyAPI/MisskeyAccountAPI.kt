package io.github.takusan23.kaisendonmk2.MisskeyAPI

import io.github.takusan23.kaisendonmk2.MastodonAPI.InstanceToken
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject

/**
 * MisskeyのアカウントAPI叩く関数があるクラス
 * */
class MisskeyAccountAPI(val instanceToken: InstanceToken) {

    // ユーザーエージェント
    private val USER_AGENT = "KaisendonMk2;@takusan_23"

    // application/json
    private val APPLICATON_JSON = "application/json".toMediaType()

    private val BASE_URL = "https://${instanceToken.instance}/api"

    /**
     * Misskeyで自分のアカウント情報を取得する。
     * パースはMisskeyParse()#parseAccount()でパースできます。
     * */
    fun getMyAccount(): Deferred<Response> = GlobalScope.async {
        val postData = JSONObject().apply {
            put("i", instanceToken.token)
        }.toString().toRequestBody(APPLICATON_JSON)
        val request = Request.Builder().apply {
            url("$BASE_URL/i")
            header("User-Agent", USER_AGENT)
            post(postData)
        }.build()
        val okHttpClient = OkHttpClient()
        val response = okHttpClient.newCall(request).execute()
        return@async response
    }

}