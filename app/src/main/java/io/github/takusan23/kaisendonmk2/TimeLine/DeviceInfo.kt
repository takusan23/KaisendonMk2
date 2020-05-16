package io.github.takusan23.kaisendonmk2.TimeLine

import android.content.Context
import android.content.Context.BATTERY_SERVICE
import android.os.BatteryManager
import android.os.Build
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat.getSystemService


/**
 * かいせんどん（回線）というわけで端末情報を正規ルートで取得できる値を取得してみる
 * */
class DeviceInfo(context: Context?) {

    /** 回線 */
    private val telephonyManager =
        context?.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

    /** 携帯回線会社に割り当てられる番号 MMCとNNCをくっつけた値。 NTT DOCOMOは44010 */
    val mobilePLMN = telephonyManager.networkOperator

    /** 携帯回線会社の名前。NTT DOCOMO とか */
    val carrierName = telephonyManager.networkOperatorName

    /** 携帯回線会社の国コード(ISO-3166) */
    val countryISO = telephonyManager.networkCountryIso

    /** 信号強度 */
    val signalLevel = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        telephonyManager.signalStrength?.level
    } else {
        -1
    }

    /** 電池残量 */
    private var batteryManager = context?.getSystemService(BATTERY_SERVICE) as BatteryManager
    var batteryLevel = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)

    /** Androidバージョン */
    val version = Build.VERSION.SDK_INT

    /** 名前 */
    val name = Build.MODEL

    /** メーカー */
    val maker = Build.BRAND

    /** SDK */
    val sdk = Build.VERSION.SDK_INT

}