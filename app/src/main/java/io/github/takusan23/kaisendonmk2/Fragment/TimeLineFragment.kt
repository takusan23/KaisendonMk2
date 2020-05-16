package io.github.takusan23.kaisendonmk2.Fragment

import android.content.SharedPreferences
import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.bumptech.glide.Glide
import io.github.takusan23.kaisendonmk2.MastodonAPI.TimeLineAPI
import io.github.takusan23.kaisendonmk2.Adapter.TimelineRecyclerViewAdapter
import io.github.takusan23.kaisendonmk2.DataClass.TimeLineItemData
import io.github.takusan23.kaisendonmk2.JSONParse.MisskeyParser
import io.github.takusan23.kaisendonmk2.JSONParse.TimeLineParser
import io.github.takusan23.kaisendonmk2.MainActivity
import io.github.takusan23.kaisendonmk2.MisskeyAPI.MisskeyTimeLineAPI
import io.github.takusan23.kaisendonmk2.R
import io.github.takusan23.kaisendonmk2.StreamingAPI.MisskeyStreamingAPI
import io.github.takusan23.kaisendonmk2.StreamingAPI.StreamingAPI
import io.github.takusan23.kaisendonmk2.TimeLine.AllTimeLineJSON
import io.github.takusan23.kaisendonmk2.TimeLine.setNullTint
import io.github.takusan23.kaisendonmk2.TimeLine.toUnixTime
import kotlinx.android.synthetic.main.fragment_timeline.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File

class TimeLineFragment : Fragment() {

    lateinit var mainActivity: MainActivity

    // 設定
    lateinit var prefSetting: SharedPreferences

    // Adapter
    lateinit var timeLineAdapter: TimelineRecyclerViewAdapter

    // RecyclerViewに渡す
    var timeLineItemDataList = arrayListOf<TimeLineItemData>()

    // リアルタイム更新
    val streamingAPIList = arrayListOf<StreamingAPI>()
    val misskeyStreamingAPIList = arrayListOf<MisskeyStreamingAPI>()

    // 追加済みID
    var addedIdList = arrayListOf<String>()

    // 横に表示する量
    var column = 2

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_timeline, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        prefSetting = PreferenceManager.getDefaultSharedPreferences(context)

        if (isAdded) {

            // RecyclerView初期化
            initRecyclerView()
            // 背景画像とかフォントとか
            setTimeLineBackgroundImage()
            setFont()

            initAllTimeLine()
            initAllTimeLineStreaming()

            fragment_timeline_swipe.setOnRefreshListener {
                initAllTimeLine()
            }

        }

    }


    // 表示するタイムライン読み込み
    fun initAllTimeLine() {
        // くるくる
        GlobalScope.launch(Dispatchers.Main) {
            timeLineItemDataList.clear()
            timeLineAdapter.notifyDataSetChanged()
            fragment_timeline_swipe?.isRefreshing = true
        }
        // 読み込む
        val allTimeLineJSON = AllTimeLineJSON(context)
        GlobalScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.IO) {
                allTimeLineJSON.loadTimeLineSettingJSON().forEach { allTimeLineData ->
                    // 有効時 で 通知以外
                    if (allTimeLineData.isEnable && allTimeLineData.timeLineLoad != "notification") {
                        if (allTimeLineData.service == "mastodon") {
                            // 取得件数
                            val limit =
                                prefSetting.getString("setting_load_limit_mastodon", "40")?.toInt()
                                    ?: 40
                            // TL取得
                            val timeLineAPI = TimeLineAPI(allTimeLineData.instanceToken)
                            val timeLineParser = TimeLineParser()
                            val response = when (allTimeLineData.timeLineLoad) {
                                "home" -> timeLineAPI.getHomeTimeLine(limit).await().body?.string()
                                "local" -> timeLineAPI.getLocalTimeLine(limit).await().body?.string()
                                else -> timeLineAPI.getHomeTimeLine(limit).await().body?.string()
                            }
                            // 追加
                            timeLineParser.parseTL(response, allTimeLineData.instanceToken).forEach { statusData ->
                                val timeLineItemData = TimeLineItemData(allTimeLineData, statusData)
                                timeLineItemDataList.add(timeLineItemData)
                            }
                        } else {
                            // Misskey TL取得
                            // 取得件数
                            val limit =
                                prefSetting.getString("setting_load_limit_misskey", "100")?.toInt()
                                    ?: 100
                            val misskeyTimeLineAPI =
                                MisskeyTimeLineAPI(allTimeLineData.instanceToken)
                            val misskeyParser = MisskeyParser()
                            val response = when (allTimeLineData.timeLineLoad) {
                                "home" -> misskeyTimeLineAPI.getHomeNotesTimeLine(limit).await().body?.string()
                                "local" -> misskeyTimeLineAPI.getLocalNotesTimeLine(limit).await().body?.string()
                                else -> misskeyTimeLineAPI.getHomeNotesTimeLine(limit).await().body?.string()
                            }
                            // 追加
                            misskeyParser.parseTimeLine(response, allTimeLineData.instanceToken).forEach { misskeyNoteData ->
                                val timeLineItemData =
                                    TimeLineItemData(allTimeLineData, null, null, misskeyNoteData, null)
                                timeLineItemDataList.add(timeLineItemData)
                            }
                        }
                    }
                }
                // 同じID消す
                timeLineItemDataList.sortByDescending { timeLineItemData ->
                    when {
                        timeLineItemData.statusData != null -> timeLineItemData.statusData.createdAt.toUnixTime()
                        timeLineItemData.notificationData != null -> timeLineItemData.notificationData.createdAt.toUnixTime()
                        timeLineItemData.misskeyNoteData != null -> timeLineItemData.misskeyNoteData.createdAt.toUnixTime()
                        timeLineItemData.misskeyNotificationData != null -> timeLineItemData.misskeyNotificationData.createdAt.toUnixTime()
                        else -> 0 // ここ来ることはまずありえない
                    }
                }
            }
            withContext(Dispatchers.IO) {
                // 並び替え
                timeLineItemDataList = timeLineItemDataList.distinctBy { timeLineItemData ->
                    when {
                        timeLineItemData.statusData != null -> timeLineItemData.statusData.id
                        timeLineItemData.notificationData != null -> timeLineItemData.notificationData.notificationId
                        timeLineItemData.misskeyNoteData != null -> timeLineItemData.misskeyNoteData.noteId
                        timeLineItemData.misskeyNotificationData != null -> timeLineItemData.misskeyNotificationData.id
                        else -> 0 // ここ来ることはまずありえない
                    }
                } as ArrayList<TimeLineItemData>
                // 重複対策
                addedIdList = timeLineItemDataList.map { timeLineItemData ->
                    timeLineItemData.statusData?.id
                        ?: timeLineItemData.notificationData?.notificationId
                        ?: timeLineItemData.misskeyNoteData?.noteId
                        ?: timeLineItemData.misskeyNotificationData?.id
                } as ArrayList<String>
            }
            // UI反映
            withContext(Dispatchers.Main) {
                if (timeLineItemDataList.isNotEmpty()) {
                    fragment_timeline_swipe?.isRefreshing = false
                    initRecyclerView()
                    if (::mainActivity.isInitialized) {
                        mainActivity.showSnackBar("${getString(R.string.timeline_item_size)}：${timeLineItemDataList.size}")
                    }
                }
            }
        }
    }


    // 表示するタイムラインのストリーミングへ接続する
    fun initAllTimeLineStreaming() {
        onDestroy()
        // 設定で無効ならストリーミング繋がない
        if (prefSetting.getBoolean("timeline_setting_disable_streaming", false)) {
            return
        }
        GlobalScope.launch(Dispatchers.IO) {
            val allTimeLineJSON = AllTimeLineJSON(context)
            allTimeLineJSON.loadTimeLineSettingJSON().forEach {
                val allTimeLineData = it
                // 有効時のみ
                if (allTimeLineData.isEnable) {
                    if (allTimeLineData.service == "mastodon") {
                        // Mastodon
                        // TL取得
                        val streamingAPI = StreamingAPI(allTimeLineData.instanceToken)
                        val isHome = allTimeLineData.timeLineLoad.contains("home")
                        val isNotification = allTimeLineData.timeLineLoad.contains("notification")
                        val isLocal = allTimeLineData.timeLineLoad.contains("local")
                        when {
                            // ホームか通知なら
                            isHome || isNotification -> streamingAPI.streamingUser({ statusData ->
                                // タイムライン
                                if (isHome) {
                                    addStreamingTLItem(TimeLineItemData(allTimeLineData, statusData))
                                }
                            }) { notificationData ->
                                // 通知
                                if (isNotification) {
                                    addStreamingTLItem(TimeLineItemData(allTimeLineData, null, notificationData))
                                }
                            }
                            isLocal -> streamingAPI.streamingLocalTL { statusData ->
                                // タイムライン
                                addStreamingTLItem(TimeLineItemData(allTimeLineData, statusData))
                            }
                            else -> streamingAPI.streamingLocalTL { statusData ->
                                // タイムライン
                                addStreamingTLItem(TimeLineItemData(allTimeLineData, statusData))
                            }
                        }
                        streamingAPIList.add(streamingAPI)
                    } else {
                        // Misskey
                        val misskeyStreamingAPI = MisskeyStreamingAPI(allTimeLineData.instanceToken)
                        val misskeyParser = MisskeyParser()
                        val isHome = allTimeLineData.timeLineLoad.contains("home")
                        val isNotification = allTimeLineData.timeLineLoad.contains("notification")
                        val isLocal = allTimeLineData.timeLineLoad.contains("local")
                        val streamingChannel = arrayListOf<String>()
                        when {
                            isHome -> streamingChannel.add(MisskeyStreamingAPI.CHANNEL_HOME)
                            isNotification -> streamingChannel.add(MisskeyStreamingAPI.CHANNEL_MAIN)
                            isLocal -> streamingChannel.add(MisskeyStreamingAPI.CHANNEL_LOCAL)
                        }
                        // ストリーミング接続。
                        misskeyStreamingAPI.initStreaming(streamingChannel) {
                            val jsonObject = JSONObject(it)
                            when (jsonObject.getJSONObject("body").getString("type")) {
                                "note" -> {
                                    // 投稿
                                    val noteData =
                                        misskeyParser.parseNote(jsonObject.getJSONObject("body").getJSONObject("body").toString(), allTimeLineData.instanceToken)
                                    addStreamingTLItem(TimeLineItemData(allTimeLineData, null, null, noteData, null))
                                }
                                "notification" -> {
                                    // 通知
                                    val notificationData =
                                        misskeyParser.parseNotification(jsonObject.getJSONObject("body").getJSONObject("body").toString(), allTimeLineData.instanceToken)
                                    addStreamingTLItem(TimeLineItemData(allTimeLineData, null, null, null, notificationData))
                                }
                            }
                        }
                        misskeyStreamingAPIList.add(misskeyStreamingAPI)
                    }
                }
            }
        }
    }

    // ストリーミングで受け取ったトゥートを表示
    fun addStreamingTLItem(timeLineItemData: TimeLineItemData) {
        // 追加済みかどうか判断する
        val isAdded = if (timeLineItemData.allTimeLineData.service == "mastodon") {
            addedIdList.contains(timeLineItemData.statusData?.id)
        } else {
            addedIdList.contains(timeLineItemData.misskeyNoteData?.noteId)
        }
        if (!isAdded) {
            GlobalScope.launch(Dispatchers.Main) {
                if (fragment_timeline_recyclerview?.layoutManager is StaggeredGridLayoutManager) {
                    // 一番上にいれば一番上に追従する時に必要な値
                    val intArray = IntArray(column)
                    val pos =
                        (fragment_timeline_recyclerview?.layoutManager as StaggeredGridLayoutManager).findFirstVisibleItemPositions(intArray)
                    // タイムライン追加
                    timeLineItemDataList.add(0, timeLineItemData)
                    timeLineAdapter.notifyItemInserted(0)
                    if (pos[0] == 0) {
                        // 一番上にいれば一番上に追従する
                        (fragment_timeline_recyclerview?.layoutManager as StaggeredGridLayoutManager).scrollToPosition(0)
                    }
                }
            }
        }
    }

    // 背景画像セット
    fun setTimeLineBackgroundImage() {
        // 画像パス
        val file = File("${context?.getExternalFilesDir(null)}/background")
        if (file.exists() && file.listFiles()?.isNotEmpty() == true) {
            val imageFile = file.listFiles()?.get(0)
            fragment_timeline_background.setNullTint()
            Glide.with(fragment_timeline_background)
                .load(imageFile)
                .into(fragment_timeline_background)
        } else {
            fragment_timeline_background.setImageDrawable(null)
        }
    }

    // フォントセット
    fun setFont() {
        val file = File("${context?.getExternalFilesDir(null)}/font.ttf")
        if (file.exists()) {
            (activity as? MainActivity)?.getTimeLineFragment()?.timeLineAdapter?.font =
                Typeface.createFromFile(file)
        }
    }

    private fun initRecyclerView() {
        fragment_timeline_recyclerview?.apply {
            setHasFixedSize(true)
            // なんかかっこいいやつ
            column = prefSetting.getString("setting_tl_column", "2")?.toInt() ?: 2
            layoutManager = StaggeredGridLayoutManager(column, StaggeredGridLayoutManager.VERTICAL)
            timeLineAdapter = TimelineRecyclerViewAdapter(timeLineItemDataList)
            if (::mainActivity.isInitialized) {
                timeLineAdapter.mainActivity = mainActivity
            }
            adapter = timeLineAdapter
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        streamingAPIList.forEach {
            it.destroy()
        }
        misskeyStreamingAPIList.forEach {
            it.destroy()
        }
    }

}