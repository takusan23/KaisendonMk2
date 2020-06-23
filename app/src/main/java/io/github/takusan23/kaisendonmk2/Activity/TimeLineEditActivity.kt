package io.github.takusan23.kaisendonmk2.Activity

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.room.Room
import io.github.takusan23.kaisendonmk2.Adapter.CustomTimeLineItemsListAdapter
import io.github.takusan23.kaisendonmk2.DetaBase.Entity.CustomTimeLineDBEntity
import io.github.takusan23.kaisendonmk2.DetaBase.RoomDataBase.CustomTimeLineDB
import io.github.takusan23.kaisendonmk2.MainActivity
import io.github.takusan23.kaisendonmk2.R
import kotlinx.android.synthetic.main.activity_time_line_edit.*
import kotlinx.android.synthetic.main.bottom_fragment_load_timeline_list.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TimeLineEditActivity : AppCompatActivity() {

    lateinit var customTimeLineItemsListAdapter: CustomTimeLineItemsListAdapter
    lateinit var customTimeLineDB: CustomTimeLineDB

    // タイムラインの構成一覧
    val timeLineSettingDataList = arrayListOf<CustomTimeLineDBEntity>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_time_line_edit)

        // タイトル
        setTitleBar()

        // RecyclerView初期化
        initRecyclerView()

        // 読み込む
        loadTimeLineSettingList()
    }

    private fun setTitleBar() {
        supportActionBar?.apply {
            title = "読み込むタイムラインの設定"
            subtitle = "再読み込み時に適用されます。"
        }
    }


    // タイムラインの構成一覧読み込む
    private fun loadTimeLineSettingList() {
        GlobalScope.launch(Dispatchers.IO) {
            customTimeLineDB = Room.databaseBuilder(this@TimeLineEditActivity, CustomTimeLineDB::class.java, "CustomTimeLineDB").build()
            customTimeLineDB.customTimeLineDBDao().getAll().forEach {
                timeLineSettingDataList.add(it)
            }
            withContext(Dispatchers.Main) {
                customTimeLineItemsListAdapter.notifyDataSetChanged()
            }
        }
    }

    private fun initRecyclerView() {
        activity_tl_edit_recyclerview.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(context)
            customTimeLineItemsListAdapter = CustomTimeLineItemsListAdapter(timeLineSettingDataList)
            adapter = customTimeLineItemsListAdapter
        }
    }

}