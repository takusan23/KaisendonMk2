package io.github.takusan23.kaisendonmk2.BottomFragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import io.github.takusan23.kaisendonmk2.R
import io.github.takusan23.kaisendonmk2.TimeLine.AllTimeLineJSON
import kotlinx.android.synthetic.main.bottom_fragment_load_timeline_edit.*

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

        val timeLineList = AllTimeLineJSON(context).loadTimeLineSettingJSON()
        val item =
            timeLineList.find { allTimeLineData -> allTimeLineData.timeLineName == timelineName }
                ?: return
        bottom_fragment_load_timeline_edit_background.setText(item.timeLineBackground)
        bottom_fragment_load_timeline_edit_text_color.setText(item.timeLineTextColor)

        // 保存ボタン
        bottom_fragment_load_timeline_edit_save.setOnClickListener {
            val backgroundColor = bottom_fragment_load_timeline_edit_background.text.toString()
            val textColor = bottom_fragment_load_timeline_edit_text_color.text.toString()
            AllTimeLineJSON(context).setAllTimeLineColor(timelineName, backgroundColor, textColor)
            dismiss()
        }

    }

}