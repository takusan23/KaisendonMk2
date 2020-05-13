package io.github.takusan23.kaisendonmk2.Fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import io.github.takusan23.kaisendonmk2.API.InstanceToken
import io.github.takusan23.kaisendonmk2.API.TimeLineAPI
import io.github.takusan23.kaisendonmk2.API.getInstanceToken
import io.github.takusan23.kaisendonmk2.Adapter.TimelineRecyclerViewAdapter
import io.github.takusan23.kaisendonmk2.DataClass.AllTimeLineData
import io.github.takusan23.kaisendonmk2.DataClass.StatusData
import io.github.takusan23.kaisendonmk2.DataClass.TimeLineItemData
import io.github.takusan23.kaisendonmk2.JSONParse.TimeLineParser
import io.github.takusan23.kaisendonmk2.MainActivity
import io.github.takusan23.kaisendonmk2.R
import io.github.takusan23.kaisendonmk2.StreamingAPI.StreamingAPI
import io.github.takusan23.kaisendonmk2.TimeLine.AllTimeLineJSON
import io.github.takusan23.kaisendonmk2.TimeLine.toUnixTime
import kotlinx.android.synthetic.main.fragment_timeline.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TimeLineFragment : Fragment() {

    lateinit var mainActivity: MainActivity

    // Adapter
    lateinit var timeLineAdapter: TimelineRecyclerViewAdapter

    // RecyclerViewに渡す
    var timeLineItemDataList = arrayListOf<TimeLineItemData>()

    // リアルタイム更新
    val streamingAPIList = arrayListOf<StreamingAPI>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_timeline, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // RecyclerView初期化
        initRecyclerView()

        initAllTimeLine()
        initAllTimeLineStreaming()

        fragment_timeline_swipe.setOnRefreshListener {
            initAllTimeLine()
        }

    }


    // 表示するタイムライン読み込み
    fun initAllTimeLine() {
        // UIスレッドではない
        GlobalScope.launch(Dispatchers.IO) {
            // くるくる
            withContext(Dispatchers.Main) {
                timeLineItemDataList.clear()
                timeLineAdapter.notifyDataSetChanged()
                fragment_timeline_swipe.isRefreshing = true
            }
            // 読み込む
            val allTimeLineJSON = AllTimeLineJSON(context)
            allTimeLineJSON.loadTimeLineSettingJSON().forEach { allTimeLineData ->
                // 有効時のみ
                if (allTimeLineData.isEnable) {
                    // TL取得
                    val timeLineAPI = TimeLineAPI(allTimeLineData.instanceToken)
                    val timeLineParser = TimeLineParser()
                    val response = when (allTimeLineData.timeLineLoad) {
                        "home_notification" -> timeLineAPI.getHomeTimeLine().await().body?.string()
                        "local" -> timeLineAPI.getLocalTimeLine().await().body?.string()
                        else -> timeLineAPI.getHomeTimeLine().await().body?.string()
                    }
                    // 追加
                    timeLineParser.parseTL(response, allTimeLineData.instanceToken).forEach { statusData ->
                        val timeLineItemData = TimeLineItemData(allTimeLineData, statusData)
                        timeLineItemDataList.add(timeLineItemData)
                    }
                }
            }
            // 並び替え / 同じID排除 など
            timeLineItemDataList.sortByDescending { timeLineItemData ->
                if (timeLineItemData.statusData != null) {
                    timeLineItemData.statusData.createdAt
                } else {
                    timeLineItemData.notificationData!!.createdAt
                }
            }
            timeLineItemDataList = timeLineItemDataList.distinctBy { timeLineItemData ->
                timeLineItemData.statusData?.id
            } as ArrayList<TimeLineItemData>
            // UI反映
            withContext(Dispatchers.Main) {
                fragment_timeline_swipe.isRefreshing = false
                initRecyclerView()
                mainActivity.showSnackBar("取得数：${timeLineItemDataList.size}")
            }
        }
    }

    // 表示するタイムライン読み込み
    fun initAllTimeLineStreaming() {
        onDestroy()
        GlobalScope.launch(Dispatchers.IO) {
            val allTimeLineJSON = AllTimeLineJSON(context)
            allTimeLineJSON.loadTimeLineSettingJSON().forEach {
                val allTimeLineData = it
                // 有効時のみ
                if (allTimeLineData.isEnable) {
                    // TL取得
                    val streamingAPI = StreamingAPI(allTimeLineData.instanceToken)
                    when (allTimeLineData.timeLineLoad) {
                        "home_notification" -> streamingAPI.streamingUser({ statusData ->
                            // タイムライン
                            addStreamingTLItem(TimeLineItemData(allTimeLineData, statusData))
                        }) { notificationData ->
                            // 通知
                            addStreamingTLItem(TimeLineItemData(allTimeLineData, null, notificationData))
                        }
                        "local" -> streamingAPI.streamingLocalTL { statusData ->
                            // タイムライン
                            addStreamingTLItem(TimeLineItemData(allTimeLineData, statusData))
                        }
                        else -> streamingAPI.streamingLocalTL { statusData ->
                            // タイムライン
                            addStreamingTLItem(TimeLineItemData(allTimeLineData, statusData))
                        }
                    }
                    streamingAPIList.add(streamingAPI)
                }
            }
        }
    }

    // ストリーミングで受け取ったトゥートを表示
    fun addStreamingTLItem(timeLineItemData: TimeLineItemData) {
        if (timeLineItemDataList.find { it.statusData?.id == timeLineItemData.statusData?.id } == null) {
            // すでに追加済みなら
            GlobalScope.launch(Dispatchers.Main) {
                // 一番上にいれば一番上に追従する時に必要な値
                val intArray = IntArray(2)
                val pos =
                    (fragment_timeline_recyclerview?.layoutManager as StaggeredGridLayoutManager).findFirstVisibleItemPositions(intArray)
                // タイムライン追加
                timeLineItemDataList.add(0, timeLineItemData)
                timeLineAdapter.notifyItemInserted(0)
                if (pos[0] == 0 || pos[1] == 0) {
                    // 一番上にいれば一番上に追従する
                    (fragment_timeline_recyclerview?.layoutManager as StaggeredGridLayoutManager).scrollToPosition(0)
                }
            }
        }
    }

    private fun initRecyclerView() {
        fragment_timeline_recyclerview.apply {
            setHasFixedSize(true)
            // なんかかっこいいやつ
            layoutManager = StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
            timeLineAdapter = TimelineRecyclerViewAdapter(timeLineItemDataList)
            timeLineAdapter.mainActivity = mainActivity
            adapter = timeLineAdapter
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        streamingAPIList.forEach {
            it.destroy()
        }
    }

}