package io.github.takusan23.kaisendonmk2.TimeLine

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build

/**
 * モバイルデータ回線で接続しいるか返す関数
 * */
internal fun isConnectionMobileData(context: Context?): Boolean {
    //今の接続状態を取得
    val connectivityManager =
        context?.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    //ろりぽっぷとましゅまろ以上で分岐
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        val networkCapabilities =
            connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
        if (networkCapabilities?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true) {
            return true
        }
    } else {
        if (connectivityManager.activeNetworkInfo.type == ConnectivityManager.TYPE_MOBILE) {
            return true
        }
    }
    return false
}