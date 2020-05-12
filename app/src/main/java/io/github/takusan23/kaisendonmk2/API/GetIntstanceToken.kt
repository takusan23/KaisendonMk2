package io.github.takusan23.kaisendonmk2.API

import android.content.Context
import androidx.preference.PreferenceManager

// ログイン情報取得関数
internal fun getInstanceToken(context: Context?): InstanceToken {
    val prefSetting = PreferenceManager.getDefaultSharedPreferences(context)
    val instance = prefSetting.getString("instance", "") ?: ""
    val token = prefSetting.getString("token", "") ?: ""
    return InstanceToken(instance, token)
}