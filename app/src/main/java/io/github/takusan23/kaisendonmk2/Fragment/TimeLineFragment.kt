package io.github.takusan23.kaisendonmk2.Fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import io.github.takusan23.kaisendonmk2.API.TimeLineAPI
import io.github.takusan23.kaisendonmk2.API.getInstanceToken
import io.github.takusan23.kaisendonmk2.Adapter.TimelineRecyclerViewAdapter
import io.github.takusan23.kaisendonmk2.DataClass.StatusData
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
    var statusList = arrayListOf<StatusData>()

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
                statusList.clear()
                timeLineAdapter.notifyDataSetChanged()
                fragment_timeline_swipe.isRefreshing = true
            }
            // 読み込む
            val allTimeLineJSON = AllTimeLineJSON(context)
            allTimeLineJSON.loadTimeLineSettingJSON().forEach {
                // 有効時のみ
                if (it.isEnable) {
                    // TL取得
                    val timeLineAPI = TimeLineAPI(it.instanceToken)
                    val timeLineParser = TimeLineParser()
                    val response = when (it.timeLineLoad) {
                        "home_notification" -> timeLineAPI.getHomeTimeLine().await().body?.string()
                        "local" -> timeLineAPI.getLocalTimeLine().await().body?.string()
                        else -> timeLineAPI.getHomeTimeLine().await().body?.string()
                    }
                    // 追加
                    timeLineParser.parseTL(response, it.instanceToken).forEach {
                        statusList.add(it)
                    }
                }
            }
            // 並び替え / 同じID排除 など
            statusList.sortByDescending { statusData -> toUnixTime(statusData.createdAt) }
            // UI反映
            withContext(Dispatchers.Main) {
                fragment_timeline_swipe.isRefreshing = false
                timeLineAdapter.notifyDataSetChanged()
                mainActivity.showSnackBar("取得数：${statusList.size}")
            }
        }
    }

    // 表示するタイムライン読み込み
    fun initAllTimeLineStreaming() {
        onDestroy()
        GlobalScope.launch(Dispatchers.IO) {
            val allTimeLineJSON = AllTimeLineJSON(context)
            allTimeLineJSON.loadTimeLineSettingJSON().forEach {
                // 有効時のみ
                if (it.isEnable) {
                    // TL取得
                    val streamingAPI = StreamingAPI(it.instanceToken)
                    val timeLineParser = TimeLineParser()
                    when (it.timeLineLoad) {
                        "home_notification" -> streamingAPI.streamingUser(::receiveMessage) {
                            // なにかする
                        }
                        "local" -> streamingAPI.streamingLocalTL(::receiveMessage)
                        else -> streamingAPI.streamingLocalTL(::receiveMessage)
                    }
                    streamingAPIList.add(streamingAPI)
                }
            }
        }
    }

    // ストリーミング受け取る拡張関数
    fun receiveMessage(statusData: StatusData) {
        GlobalScope.launch(Dispatchers.Main) {
            // 一番上にいれば一番上に追従する時に必要な値
            val intArray = IntArray(2)
            val pos =
                (fragment_timeline_recyclerview?.layoutManager as StaggeredGridLayoutManager).findFirstVisibleItemPositions(intArray)
            // タイムライン追加
            statusList.add(0, statusData)
            timeLineAdapter.notifyItemInserted(0)
            if (pos[0] == 0 || pos[1] == 0) {
                // 一番上にいれば一番上に追従する
                (fragment_timeline_recyclerview?.layoutManager as StaggeredGridLayoutManager).scrollToPosition(0)
            }
        }
    }

    private fun initRecyclerView() {
        fragment_timeline_recyclerview.apply {
            setHasFixedSize(true)
            // なんかかっこいいやつ
            layoutManager = StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
            timeLineAdapter = TimelineRecyclerViewAdapter(statusList)
            timeLineAdapter.mainActivity = mainActivity
            adapter = timeLineAdapter
        }
    }

    // タイムライン取得
    fun getTimeLine() {
        fragment_timeline_swipe.isRefreshing = true
        GlobalScope.launch {
            // APIまとめ
            val timeLineAPI = TimeLineAPI(getInstanceToken(context))
            val parseTL = TimeLineParser()
            // パース
            val localTL = timeLineAPI.getLocalTimeLine().await()
            val response = parseTL.parseTL(localTL.body?.string(), getInstanceToken(context))
            response.forEach {
                statusList.add(it)
            }
            activity?.runOnUiThread {
                fragment_timeline_swipe.isRefreshing = false
                timeLineAdapter.notifyDataSetChanged()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        streamingAPIList.forEach {
            it.destroy()
        }
    }

}