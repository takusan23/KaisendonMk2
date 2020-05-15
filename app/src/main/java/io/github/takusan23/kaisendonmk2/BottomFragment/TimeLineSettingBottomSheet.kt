package io.github.takusan23.kaisendonmk2.BottomFragment

import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Switch
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import io.github.takusan23.kaisendonmk2.R
import kotlinx.android.synthetic.main.bottom_fragment_timeline_setting.*

class TimeLineSettingBottomSheet : BottomSheetDialogFragment() {

    lateinit var prefSetting: SharedPreferences

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.bottom_fragment_timeline_setting, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        prefSetting = PreferenceManager.getDefaultSharedPreferences(context)

        // 設定適用・読み込み
        initSettingSwitch(bottom_fragment_timeline_setting_streaming, "timeline_setting_disable_streaming") // ストリーミング
        initSettingSwitch(bottom_fragment_timeline_setting_image, "timeline_setting_image_hide") // 画像を非表示
        initSettingSwitch(bottom_fragment_timeline_setting_mobile_image, "timeline_setting_image_hide_mobile") // モバイルデータ回線なら非表示
        initSettingSwitch(bottom_fragment_timeline_setting_gif, "timeline_setting_image_gif_stop") // GIFアニメ止める

    }

    /**
     * Switchに設定の値を読み込み・書き込みできる関数。
     * @param key 設定の保存されるキー
     * @param switch スイッチ
     * */
    private fun initSettingSwitch(switch: Switch, key: String) {
        switch.setOnCheckedChangeListener { buttonView, isChecked ->
            prefSetting.edit { putBoolean(key, isChecked) }
        }
        switch.isChecked = prefSetting.getBoolean(key, false)
    }

}