package io.github.takusan23.kaisendonmk2.Tool

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException

/**
 * レスポンス結果が入るデータクラス。
 * @param isSuccessful 成功時true（trueならResponse入ってる。200の時は確実true）
 * @param response レスポンス（isSuccessfulがfalseでも入ってる可能性あり。）
 * @param ioException 失敗時
 * */
data class ResultResponse(val isSuccessful: Boolean, val response: Response? = null, val ioException: IOException? = null)

/**
 * OkHttpでリクエストする関数。
 * @param request OkHttpのリクエスト
 * @return 成功/失敗どっちかが入ってるデータクラス
 * */
internal fun okhttpExecute(request: Request): ResultResponse {
    return try {
        // 成功時
        val response = OkHttpClient().newCall(request).execute()
        ResultResponse(isSuccessful = response.isSuccessful, response = response)
    } catch (e: IOException) {
        // 失敗時
        ResultResponse(isSuccessful = false, ioException = e)
    }
}

