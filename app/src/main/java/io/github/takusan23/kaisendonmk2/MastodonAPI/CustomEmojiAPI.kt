package io.github.takusan23.kaisendonmk2.MastodonAPI

import io.github.takusan23.kaisendonmk2.DataClass.EmojiData
import io.github.takusan23.kaisendonmk2.JSONParse.TimeLineParser
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONArray

/**
 * インスタンスにあるカスタム絵文字取得関数など
 * */
class CustomEmojiAPI(val instanceToken: InstanceToken) {

    /**
     * カスタム絵文字取得する関数。
     * */
    fun getCustomEmoji(): Deferred<Response> = GlobalScope.async {
        val request = Request.Builder().apply {
            url("https://${instanceToken.instance}/api/v1/custom_emojis")
            header("User-Agent", "KaisendonMk2;@takusan_23")
            get()
        }.build()
        val okHttpClient = OkHttpClient()
        val response = okHttpClient.newCall(request).execute()
        return@async response
    }

    /**
     * カスタム絵文字のレスポンスをパースする関数
     * @param getCustomEmoji()のレスポンス
     * */
    fun parseCustomEmoji(responseString: String): ArrayList<EmojiData> {
        val emojiDataList = arrayListOf<EmojiData>()
        val timeLineParser = TimeLineParser()
        return timeLineParser.parseEmoji(JSONArray((responseString)))
    }

}