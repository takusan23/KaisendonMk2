package io.github.takusan23.kaisendonmk2.BottomFragment

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.database.Cursor
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Switch
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import io.github.takusan23.kaisendonmk2.MainActivity
import io.github.takusan23.kaisendonmk2.R
import kotlinx.android.synthetic.main.bottom_fragment_timeline_setting.*
import java.io.File

/**
 * TL設定BottomFragment。画像非表示など
 * */
class TimeLineSettingBottomSheet : BottomSheetDialogFragment() {

    lateinit var prefSetting: SharedPreferences

    // 背景画像リクエストコード
    val REQURST_CODE = 816

    // フォントリクエストコード
    val FONT_REQUEST_CODE = 114

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

        // 背景画像
        bottom_fragment_timeline_setting_background_set.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                type = "image/*"
            }
            startActivityForResult(intent, REQURST_CODE)
        }
        bottom_fragment_timeline_setting_background_reset.setOnClickListener {
            deleteBackgroundImg()
        }

        // フォント設定
        bottom_fragment_timeline_setting_font_set.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                type = "font/*"
            }
            startActivityForResult(intent, FONT_REQUEST_CODE)
        }
        bottom_fragment_timeline_setting_font_reset.setOnClickListener {
            deleteFont()
        }

    }

    // フォント削除
    private fun deleteFont() {
        val file = File("${context?.getExternalFilesDir(null)}/font.ttf")
        file.delete()
        (activity as? MainActivity)?.getTimeLineFragment()?.timeLineAdapter?.font = Typeface.DEFAULT
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                REQURST_CODE -> {
                    // 先客は消す
                    deleteBackgroundImg()
                    // アプリ固有ディレクトリにコピー
                    val uri = data?.data ?: return
                    val file = File("${context?.getExternalFilesDir(null)}/background")
                    if (!file.exists()) {
                        file.mkdir()
                    }
                    // コピー
                    val imageFile = File("${file.path}/${getFileName(uri)}")
                    imageFile.createNewFile()
                    val byteArray = context?.contentResolver?.openInputStream(uri)?.readBytes()
                    if (byteArray != null) {
                        imageFile.writeBytes(byteArray)
                    }
                    // 適用
                    (activity as? MainActivity)?.getTimeLineFragment()?.setTimeLineBackgroundImage()
                }
                FONT_REQUEST_CODE -> {
                    // フォント設定
                    deleteFont()
                    // アプリ固有ディレクトリにコピー
                    val uri = data?.data ?: return
                    val file = File("${context?.getExternalFilesDir(null)}/font.ttf")
                    file.createNewFile()
                    // こぴー
                    val byteArray = context?.contentResolver?.openInputStream(uri)?.readBytes()
                    if (byteArray != null) {
                        file.writeBytes(byteArray)
                    }
                    // Adapterに適用
                    (activity as? MainActivity)?.getTimeLineFragment()?.setFont()
                }
            }
        }
    }

    /**
     * Uriからファイル名取得関数
     * */
    fun getFileName(uri: Uri): String {
        var fileName = ""
        val cursor: Cursor? = context?.contentResolver?.query(uri, null, null, null, null, null)
        cursor?.apply {
            moveToFirst()
            fileName = cursor.getString(this.getColumnIndex(OpenableColumns.DISPLAY_NAME))
            close()
        }
        return fileName
    }

    /**
     * 背景画像消す設定。
     * 背景設定のためにアプリ固有ディレクトリ内にコピーしたのでそれを消す
     * */
    fun deleteBackgroundImg() {
        val file = File("${context?.getExternalFilesDir(null)}/background")
        if (file.exists()) {
            file.listFiles()?.forEach {
                it.delete()
            }
        }
        // 適用
        (activity as? MainActivity)?.getTimeLineFragment()?.setTimeLineBackgroundImage()
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