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
import kotlinx.android.synthetic.main.fragment_timeline.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class TimeLineFragment : Fragment() {

    lateinit var mainActivity: MainActivity

    // Adapter
    lateinit var timeLineAdapter: TimelineRecyclerViewAdapter
    var statusList = arrayListOf<StatusData>()

    lateinit var streamingAPI: StreamingAPI

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_timeline, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // RecyclerView初期化
        initRecyclerView()

        // TL取得
        getTimeLine()

        // ストリーミング接続
        streamingAPI = StreamingAPI(getInstanceToken(context))
        setStreaming()

        // すわいぷ
        fragment_timeline_swipe.setOnRefreshListener {
            statusList.clear()
            getTimeLine()
        }

    }

    // ストリーミングAPIに接続する
    private fun setStreaming() {
        GlobalScope.launch {
            streamingAPI.streamingLocalTL {
                activity?.runOnUiThread {
                    // 一番上にいれば一番上に追従する時に必要な値
                    val intArray = IntArray(2)
                    val pos =
                        (fragment_timeline_recyclerview?.layoutManager as StaggeredGridLayoutManager).findFirstVisibleItemPositions(intArray)
                    // タイムライン追加
                    statusList.add(0, it)
                    timeLineAdapter.notifyItemInserted(0)
                    if (pos[0] == 0 || pos[1] == 0) {
                        // 一番上にいれば一番上に追従する
                        (fragment_timeline_recyclerview?.layoutManager as StaggeredGridLayoutManager).scrollToPosition(0)
                    }
                }
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
        streamingAPI.destroy()
    }

}