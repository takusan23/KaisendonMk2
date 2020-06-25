package io.github.takusan23.kaisendonmk2.Adapter

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Switch
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Room
import com.jaredrummler.android.colorpicker.ColorPickerDialog
import com.jaredrummler.android.colorpicker.ColorPickerDialogListener
import io.github.takusan23.kaisendonmk2.BottomFragment.LoadTimeLineEditBottomSheet
import io.github.takusan23.kaisendonmk2.DetaBase.Dao.CustomTimeLineDBDao
import io.github.takusan23.kaisendonmk2.DetaBase.Entity.CustomTimeLineDBEntity
import io.github.takusan23.kaisendonmk2.DetaBase.RoomDataBase.CustomTimeLineDB
import io.github.takusan23.kaisendonmk2.R
import io.github.takusan23.kaisendonmk2.TimeLine.setNullTint
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import okhttp3.internal.toHexString

/**
 * タイムラインの構成一覧表示のAdapter
 * */
class CustomTimeLineItemsListAdapter(val customTimeLineList: ArrayList<CustomTimeLineDBEntity>) : RecyclerView.Adapter<CustomTimeLineItemsListAdapter.ViewHolder>() {

    lateinit var customTimeLineDB: CustomTimeLineDB
    lateinit var customTimeLineDBDao: CustomTimeLineDBDao

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.adapter_timeline_setting, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return customTimeLineList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.apply {
            // Context
            val context = enableSwitch.context
            if (!::customTimeLineDB.isInitialized) {
                customTimeLineDB = Room.databaseBuilder(context, CustomTimeLineDB::class.java, "CustomTimeLineDB").build()
                customTimeLineDBDao = customTimeLineDB.customTimeLineDBDao()
            }

            // 値入れる
            val allTimeLineData = customTimeLineList[position]
            enableSwitch.isChecked = allTimeLineData.isEnable
            wifiSwitch.isChecked = allTimeLineData.isWiFiOnly
            nameTextView.text = allTimeLineData.name
            // いろ
            val labelColor = allTimeLineData.labelColor
            if (labelColor?.isNotEmpty() == true) {
                styleButton.imageTintList = ColorStateList.valueOf(Color.parseColor(labelColor))
            } else {
                styleButton.setNullTint()
            }

            // スイッチ
            enableSwitch.setOnCheckedChangeListener { buttonView, isChecked ->
                // なんと isPressed を使うことでなにもしてないのにリスナーが動いた時でも大丈夫（リサイクラービューは使い回されるので）
                if (buttonView.isPressed) {
                    // 有効/無効を反転
                    updateEnableDB(context, allTimeLineData.id, isEnable = isChecked, isWiFiOnly = allTimeLineData.isWiFiOnly)
                }
            }

            // Wi-Fiのみ
            wifiSwitch.setOnCheckedChangeListener { buttonView, isChecked ->
                if (buttonView.isPressed) {
                    // 有効/無効を反転
                    updateEnableDB(context, allTimeLineData.id, isEnable = allTimeLineData.isEnable, isWiFiOnly = isChecked)
                }
            }

            // 名前変更。未実装
            editButton.setOnClickListener {
/*
                val loadTimeLineEditBottomSheet = LoadTimeLineEditBottomSheet()
                val bundle = Bundle()
                bundle.putInt("id", allTimeLineData.id)
                loadTimeLineEditBottomSheet.arguments = bundle
                loadTimeLineEditBottomSheet.show((context as AppCompatActivity).supportFragmentManager, "style")
*/
            }

            // 色設定
            styleButton.setOnClickListener {
                // カラーピッカーライブラリを入れた。すげえ
                val dialog = ColorPickerDialog.newBuilder().setDialogType(ColorPickerDialog.TYPE_PRESETS).create()
                dialog.show((context as AppCompatActivity).supportFragmentManager, "color")
                dialog.setColorPickerDialogListener(object : ColorPickerDialogListener {
                    override fun onDialogDismissed(dialogId: Int) {

                    }

                    override fun onColorSelected(dialogId: Int, color: Int) {
                        updateColorLabel(context, allTimeLineData.id, "#${color.toHexString()}")
                        styleButton.imageTintList = ColorStateList.valueOf(Color.parseColor("#${color.toHexString()}"))
                    }
                })
            }

        }
    }

    /**
     * DBの有効、無効を更新する
     * */
    private fun updateEnableDB(context: Context, id: Int, isEnable: Boolean = true, isWiFiOnly: Boolean = false) {
        GlobalScope.launch {
            val db = Room.databaseBuilder(context, CustomTimeLineDB::class.java, "CustomTimeLineDB").build()
            val item = db.customTimeLineDBDao().findById(id)
            item.isEnable = isEnable
            item.isWiFiOnly = isWiFiOnly
            customTimeLineDBDao.update(item)
        }
    }

    /**
     * 色を更新する。
     * @param id データベースの主キー。
     * @param colorHexCode カラーコードの色。#ffffffなど
     * */
    private fun updateColorLabel(context: Context, id: Int, colorHexCode: String) {
        GlobalScope.launch {
            val db = Room.databaseBuilder(context, CustomTimeLineDB::class.java, "CustomTimeLineDB").build()
            val item = db.customTimeLineDBDao().findById(id)
            item.labelColor = colorHexCode
            customTimeLineDBDao.update(item)
        }
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val enableSwitch = itemView.findViewById<Switch>(R.id.adapter_tl_setting_switch_enable)
        val wifiSwitch = itemView.findViewById<Switch>(R.id.adapter_tl_setting_switch_wifi)
        val nameTextView = itemView.findViewById<TextView>(R.id.adapter_tl_setting_name)
        val editButton = itemView.findViewById<ImageView>(R.id.adapter_tl_setting_edit)
        val styleButton = itemView.findViewById<ImageView>(R.id.adapter_tl_setting_color)
    }

}
