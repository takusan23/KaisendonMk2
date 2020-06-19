package io.github.takusan23.kaisendonmk2.BottomFragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.room.Room
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import io.github.takusan23.kaisendonmk2.DetaBase.RoomDataBase.CustomTimeLineDB
import io.github.takusan23.kaisendonmk2.R
import kotlinx.android.synthetic.main.bottom_fragment_load_timeline_edit.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.json.JSONObject

/**
 * タイムラインの色設定とか。
 * argmentにname（タイムラインの名前）入れてね
 * */
class LoadTimeLineEditBottomSheet : BottomSheetDialogFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.bottom_fragment_load_timeline_edit, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 読み込む
        val timelineName = arguments?.getString("name", "") ?: return

        // データベース
        val customTimeLineDB = Room.databaseBuilder(requireContext(), CustomTimeLineDB::class.java, "CustomTimeLineDB").build()
        val dao = customTimeLineDB.customTimeLineDBDao()
        // RoomはUIスレッドでは呼べないので
        GlobalScope.launch {
            val item = dao.getAll().find { allTimeLineData -> allTimeLineData.name == timelineName } ?: return@launch
            val timeLineData = item.timeline ?: return@launch
            // もとの値セット
            bottom_fragment_load_timeline_edit_background.setText(JSONObject(timeLineData).getString("background_color"))
            // 保存ボタン
            bottom_fragment_load_timeline_edit_save.setOnClickListener {
                // JSON再構成
                val backgroundColor = bottom_fragment_load_timeline_edit_background.text.toString()
                val timeLineJSON = JSONObject(timeLineData).apply {
                    remove("background_color")
                    put("background_color", backgroundColor)
                }.toString()
                // 更新
                GlobalScope.launch {
                    item.timeline = timeLineJSON
                    dao.update(item)
                }
                dismiss()
            }
        }
    }
}