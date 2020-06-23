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
        val timelineColumnId = arguments?.getInt("id", 0) ?: return

        // データベース
        val customTimeLineDB = Room.databaseBuilder(requireContext(), CustomTimeLineDB::class.java, "CustomTimeLineDB").build()
        val dao = customTimeLineDB.customTimeLineDBDao()
        // RoomはUIスレッドでは呼べないので
        GlobalScope.launch {
            // DB取り出し
            val data = dao.findById(timelineColumnId)
            bottom_fragment_load_timeline_edit_background.setText(data.labelColor)
            // 保存ボタン押したとき
            bottom_fragment_load_timeline_edit_save.setOnClickListener {
                // DB更新
                GlobalScope.launch {
                    val db = Room.databaseBuilder(requireContext(), CustomTimeLineDB::class.java, "CustomTimeLineDB").build()
                    db.customTimeLineDBDao().update(data)
                }
            }
        }
    }
}