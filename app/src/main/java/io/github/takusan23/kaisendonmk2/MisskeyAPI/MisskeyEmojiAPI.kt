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
 * Misskeyのカスタム絵文字
 * */
class MisskeyEmojiAPI(val instanceToken: InstanceToken) {

    // ユーザーエージェント
    private val USER_AGENT = "KaisendonMk2;@takusan_23"

    // application/json
    private val APPLICATON_JSON = "application/json".toMediaType()

    private val BASE_URL = "https://${instanceToken.instance}/api"

    /**
     * Misskeyのカスタム絵文字一覧取得APIを叩く。
     * */
    fun getMisskeyEmoji(): Deferred<Response> = GlobalScope.async {
        val postData = JSONObject().toString().toRequestBody(APPLICATON_JSON)
        val request = Request.Builder().apply {
            url("$BASE_URL/emojis")
            header("User-Agent", USER_AGENT)
            post(postData)
        }.build()
        val okHttpClient = OkHttpClient()
        val response = okHttpClient.newCall(request).execute()
        return@async response
    }

}