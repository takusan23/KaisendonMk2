package io.github.takusan23.kaisendonmk2.BottomFragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.room.Room
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import io.github.takusan23.kaisendonmk2.Adapter.CustomTimeLineItemsListAdapter
import io.github.takusan23.kaisendonmk2.DetaBase.Entity.CustomTimeLineDBEntity
import io.github.takusan23.kaisendonmk2.DetaBase.RoomDataBase.CustomTimeLineDB
import io.github.takusan23.kaisendonmk2.Fragment.TabLayoutFragment
import io.github.takusan23.kaisendonmk2.Fragment.TabLayoutTimeLineFragment
import io.github.takusan23.kaisendonmk2.Fragment.TimeLineFragment
import io.github.takusan23.kaisendonmk2.MainActivity
import io.github.takusan23.kaisendonmk2.R
import kotlinx.android.synthetic.main.bottom_fragment_load_timeline_list.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.internal.UTC

/**
 * 読み込むTLの一覧
 * */
class LoadTimeLineListBottomSheet : BottomSheetDialogFragment() {

    lateinit var customTimeLineItemsListAdapter: CustomTimeLineItemsListAdapter
    lateinit var mainActivity: MainActivity
    lateinit var customTimeLineDB: CustomTimeLineDB

    // タイムラインの構成一覧
    val timeLineSettingDataList = arrayListOf<CustomTimeLineDBEntity>()

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
    private fun loadTimeLineSettingList() {
        GlobalScope.launch(Dispatchers.IO) {
            customTimeLineDB = Room.databaseBuilder(requireContext(), CustomTimeLineDB::class.java, "CustomTimeLineDB").build()
            customTimeLineDB.customTimeLineDBDao().getAll().forEach {
                timeLineSettingDataList.add(it)
            }
            withContext(Dispatchers.Main) {
                customTimeLineItemsListAdapter.notifyDataSetChanged()
            }
        }
    }

    private fun initRecyclerView() {
        bottom_fragment_timeline_setting_list_recyclerview.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(context)
            customTimeLineItemsListAdapter = CustomTimeLineItemsListAdapter(timeLineSettingDataList)
            customTimeLineItemsListAdapter.mainActivity = mainActivity
            customTimeLineItemsListAdapter.loadTimeLineListBottomSheet = this@LoadTimeLineListBottomSheet
            adapter = customTimeLineItemsListAdapter
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // TL再読み込み
        mainActivity.apply {
            if (isTabTLMode) {
                getTabLayoutAttachTimeLineFragmentList().forEach { fragment ->
                    fragment.onDestroy()
/*
                    val data = fragment.arguments?.getSerializable("timeline") as CustomTimeLineDBEntity
                    GlobalScope.launch {
                        fragment.loadTimeLine(data).await()
                        fragment.initStreaming(data)
                    }
*/
                }
                getTabLayoutFragment().initViewPager()
            } else {
                getTimeLineFragment().apply {
                    GlobalScope.launch {
                        initAllTimeLine().await()
                        initAllTimeLineStreaming()
                    }
                }
            }
        }
    }

}