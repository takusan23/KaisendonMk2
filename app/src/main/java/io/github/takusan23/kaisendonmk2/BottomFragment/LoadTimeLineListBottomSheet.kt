package io.github.takusan23.kaisendonmk2.BottomFragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import io.github.takusan23.kaisendonmk2.Adapter.AllTimeLineAdapter
import io.github.takusan23.kaisendonmk2.DataClass.AllTimeLineData
import io.github.takusan23.kaisendonmk2.MainActivity
import io.github.takusan23.kaisendonmk2.R
import io.github.takusan23.kaisendonmk2.TimeLine.AllTimeLineJSON
import kotlinx.android.synthetic.main.bottom_fragment_load_timeline_list.*

/**
 * 読み込むTLの一覧
 * */
class LoadTimeLineListBottomSheet : BottomSheetDialogFragment() {

    lateinit var allTimeLineAdapter: AllTimeLineAdapter
    lateinit var mainActivity: MainActivity

    // タイムラインの構成一覧
    val timeLineSettingDataList = arrayListOf<AllTimeLineData>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.bottom_fragment_load_timeline_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        // RecyclerView初期化
        initRecyclerView()

        // 読み込む
        loadTimeLineSettingList()

    }

    // タイムラインの構成一覧読み込む
    fun loadTimeLineSettingList() {
        timeLineSettingDataList.clear()
        val timeLineSettingJSON = AllTimeLineJSON(context)
        timeLineSettingJSON.loadTimeLineSettingJSON().forEach {
            timeLineSettingDataList.add(it)
        }
        allTimeLineAdapter.notifyDataSetChanged()
    }

    private fun initRecyclerView() {
        bottom_fragment_timeline_setting_list_recyclerview.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(context)
            allTimeLineAdapter = AllTimeLineAdapter(timeLineSettingDataList)
            allTimeLineAdapter.mainActivity = mainActivity
            allTimeLineAdapter.loadTimeLineListBottomSheet = this@LoadTimeLineListBottomSheet
            adapter = allTimeLineAdapter
        }
    }
}