package io.github.takusan23.kaisendonmk2.TimeLine

import android.content.Context
import androidx.preference.PreferenceManager
import io.github.takusan23.kaisendonmk2.API.InstanceToken
import org.json.JSONArray

/**
 * マルチアカウントを楽に扱いたい。
 * アカウント情報を配列で返す関数。
 * */
internal fun loadMultiAccount(context: Context?): ArrayList<InstanceToken> {
    val accountList = arrayListOf<InstanceToken>()
    val prefSetting = PreferenceManager.getDefaultSharedPreferences(context)
    val accountJSON = prefSetting.getString("account_json", null)
    // 無いとき
    if (accountJSON == null) {
        return accountList
    } else {
        val jsonArray = JSONArray(accountJSON)
        for (i in 0 until jsonArray.length()) {
            val accountObject = jsonArray.getJSONObject(i)
            val instance = accountObject.getString("instance")
            val token = accountObject.getString("token")
            val service = accountObject.getString("service")
            val instanceToken = InstanceToken(instance, token, service)
            accountList.add(instanceToken)
        }
        return accountList
    }
}