package io.github.takusan23.kaisendonmk2.Fragment

import android.content.SharedPreferences
import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import io.github.takusan23.kaisendonmk2.Adapter.TimelineRecyclerViewAdapter
import io.github.takusan23.kaisendonmk2.DataClass.CustomTimeLineData
import io.github.takusan23.kaisendonmk2.DataClass.TimeLineItemData
import io.github.takusan23.kaisendonmk2.DetaBase.Entity.CustomTimeLineDBEntity
import io.github.takusan23.kaisendonmk2.JSONParse.MisskeyParser
import io.github.takusan23.kaisendonmk2.JSONParse.TimeLineParser
import io.github.takusan23.kaisendonmk2.MastodonAPI.InstanceToken
import io.github.takusan23.kaisendonmk2.MastodonAPI.TimeLineAPI
import io.github.takusan23.kaisendonmk2.MisskeyAPI.MisskeyTimeLineAPI
import io.github.takusan23.kaisendonmk2.R
import io.github.takusan23.kaisendonmk2.StreamingAPI.MisskeyStreamingAPI
import io.github.takusan23.kaisendonmk2.StreamingAPI.StreamingAPI
import io.github.takusan23.kaisendonmk2.TimeLine.setNullTint
import io.github.takusan23.kaisendonmk2.Tool.CustomTimeLineDataJSON
import kotlinx.android.synthetic.main.fragment_tablayout_timeline.*
import kotlinx.android.synthetic.main.fragment_timeline.*
import kotlinx.coroutines.*
import org.json.JSONObject
import java.io.File

/**
 * ViewPagerに入れるFragment。
 * 入れてほしいもの↓
 * |timeline    | CustomTimeLineDBEntity (Serialize) | タイムラインの情報なんで。
 * */
class TabLayoutTimeLineFragment : Fragment() {

    lateinit var prefSetting: SharedPreferences

    // RecyclerView
    val timeLineItemDataList = arrayListOf<TimeLineItemData>()
    val timelineRecyclerViewAdapter = TimelineRecyclerViewAdapter(timeLineItemDataList)

    // リアルタイム更新
    val streamingAPIList = arrayListOf<StreamingAPI>()
    val misskeyStreamingAPIList = arrayListOf<MisskeyStreamingAPI>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_tablayout_timeline, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (isAdded) {
            prefSetting = PreferenceManager.getDefaultSharedPreferences(context)

            // RecyclerView初期化
            initRecyclerView()
            // 背景画像とかフォントとか
            setTimeLineBackgroundImage()
            setFont()

            // データ受け取る
            val data = arguments?.getSerializable("timeline") as CustomTimeLineDBEntity

            // 画面回転時
            if (savedInstanceState != null) {
                ArrayList(savedInstanceState.getSerializable("list") as ArrayList<TimeLineItemData>).forEach {
                    timeLineItemDataList.add(it)
                }
                timelineRecyclerViewAdapter.notifyDataSetChanged()
                initStreaming(data)
            } else {
                // 初回実行時
                if (timeLineItemDataList.isEmpty()) {
                    // TL読み込み
                    GlobalScope.launch(Dispatchers.Main) {
                        loadTimeLine(data).await()
                        initStreaming(data)
                    }
                }
            }

            fragment_tablayout_timeline_swipe.setOnRefreshListener {
                GlobalScope.launch(Dispatchers.Main) {
                    loadTimeLine(data).await()
                }
            }
        }

    }

    fun setFont() {
        val file = File("${context?.getExternalFilesDir(null)}/font.ttf")
        if (file.exists()) {
            timelineRecyclerViewAdapter.font = Typeface.createFromFile(file)
        }
    }

    fun initStreaming(data: CustomTimeLineDBEntity) {
        onDestroy()
        // 設定で無効ならストリーミング繋がない
        if (prefSetting.getBoolean("timeline_setting_disable_streaming", false)) {
            return
        }
        GlobalScope.launch {
            // ログイン情報
            val timeLineData = CustomTimeLineDataJSON().parse(data.timeline) ?: return@launch
            if (timeLineData.service == "mastodon") {
                // TL取得
                val streamingAPI = StreamingAPI(timeLineData.instanceToken)
                val isHome = timeLineData.timeLineLoad.contains("home")
                val isNotification = timeLineData.timeLineLoad.contains("notification")
                val isLocal = timeLineData.timeLineLoad.contains("local")
                when {
                    // ホームか通知なら
                    isHome || isNotification -> streamingAPI.streamingUser({ statusData ->
                        // タイムライン
                        if (isHome) {
                            addStreamingTLItem(TimeLineItemData(timeLineData, statusData))
                        }
                    }) { notificationData ->
                        // 通知
                        if (isNotification) {
                            addStreamingTLItem(TimeLineItemData(timeLineData, null, notificationData))
                        }
                    }
                    isLocal -> streamingAPI.streamingLocalTL { statusData ->
                        // タイムライン
                        addStreamingTLItem(TimeLineItemData(timeLineData, statusData))
                    }
                    else -> streamingAPI.streamingLocalTL { statusData ->
                        // タイムライン
                        addStreamingTLItem(TimeLineItemData(timeLineData, statusData))
                    }
                }
                streamingAPIList.add(streamingAPI)
            } else {
                // Misskey
                val misskeyStreamingAPI = MisskeyStreamingAPI(timeLineData.instanceToken)
                val misskeyParser = MisskeyParser()
                val isHome = timeLineData.timeLineLoad.contains("home")
                val isNotification = timeLineData.timeLineLoad.contains("notification")
                val isLocal = timeLineData.timeLineLoad.contains("local")
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
                            val noteData = misskeyParser.parseNote(jsonObject.getJSONObject("body").getJSONObject("body").toString(), timeLineData.instanceToken)
                            addStreamingTLItem(TimeLineItemData(timeLineData, null, null, noteData, null))
                        }
                        "notification" -> {
                            // 通知
                            val notificationData = misskeyParser.parseNotification(jsonObject.getJSONObject("body").getJSONObject("body").toString(), timeLineData.instanceToken)
                            addStreamingTLItem(TimeLineItemData(timeLineData, null, null, null, notificationData))
                        }
                    }
                }
                misskeyStreamingAPIList.add(misskeyStreamingAPI)
            }
        }
    }

    // ストリーミングで受け取ったトゥートを表示
    fun addStreamingTLItem(timeLineItemData: TimeLineItemData) {
        GlobalScope.launch(Dispatchers.Main) {
            if (fragment_tablayout_timeline_recyclerview?.layoutManager is LinearLayoutManager) {
                // 一番上にいれば一番上に追従する
                val pos = (fragment_tablayout_timeline_recyclerview?.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()
                // タイムライン追加
                timeLineItemDataList.add(0, timeLineItemData)
                timelineRecyclerViewAdapter.notifyItemInserted(0)
                if (pos == 0 || pos == 1) {
                    // 一番上にいれば一番上に追従する
                    (fragment_tablayout_timeline_recyclerview?.layoutManager as LinearLayoutManager).scrollToPosition(0)
                }
            }
        }
    }


    // タイムライン読み込む
    fun loadTimeLine(data: CustomTimeLineDBEntity) = GlobalScope.async(Dispatchers.IO) {
        // くるくる
        withContext(Dispatchers.Main) {
            timeLineItemDataList.clear()
            fragment_tablayout_timeline_swipe.isRefreshing = true
            timelineRecyclerViewAdapter.notifyDataSetChanged()
        }
        // ログイン情報
        val customTimeLineData = CustomTimeLineDataJSON().parse(data.timeline) ?: return@async
        val instanceToken = customTimeLineData.instanceToken
        // タイムライン取得
        withContext(Dispatchers.IO) {
            if (customTimeLineData.service == "mastodon") {
                // Mastodon
                // 取得件数
                val limit = prefSetting.getString("setting_load_limit_mastodon", "40")?.toInt() ?: 40
                // TL取得
                val timeLineAPI = TimeLineAPI(instanceToken)
                val timeLineParser = TimeLineParser()
                val response = when (customTimeLineData.timeLineLoad) {
                    "home" -> timeLineAPI.getHomeTimeLine(limit).await().body?.string()
                    "local" -> timeLineAPI.getLocalTimeLine(limit).await().body?.string()
                    else -> timeLineAPI.getHomeTimeLine(limit).await().body?.string()
                }
                // 追加
                timeLineParser.parseTL(response, instanceToken).forEach { statusData ->
                    val timeLineItemData = TimeLineItemData(customTimeLineData, statusData)
                    timeLineItemDataList.add(timeLineItemData)
                }
            } else {
                // Misskey
                // Misskey TL取得
                // 取得件数
                val limit = prefSetting.getString("setting_load_limit_misskey", "100")?.toInt() ?: 100
                val misskeyTimeLineAPI = MisskeyTimeLineAPI(instanceToken)
                val misskeyParser = MisskeyParser()
                val response = when (customTimeLineData.timeLineLoad) {
                    "home" -> misskeyTimeLineAPI.getHomeNotesTimeLine(limit).await().body?.string()
                    "local" -> misskeyTimeLineAPI.getLocalNotesTimeLine(limit).await().body?.string()
                    else -> misskeyTimeLineAPI.getHomeNotesTimeLine(limit).await().body?.string()
                }
                // 追加
                misskeyParser.parseTimeLine(response, instanceToken).forEach { misskeyNoteData ->
                    val timeLineItemData = TimeLineItemData(customTimeLineData, null, null, misskeyNoteData, null)
                    timeLineItemDataList.add(timeLineItemData)
                }
            }
        }
        // UI反映
        withContext(Dispatchers.Main) {
            if (timeLineItemDataList.isNotEmpty()) {
                fragment_tablayout_timeline_swipe?.isRefreshing = false
                timelineRecyclerViewAdapter.notifyDataSetChanged()
            }
        }
    }

    // 背景画像セット
    fun setTimeLineBackgroundImage() {
        // 画像パス
        val file = File("${context?.getExternalFilesDir(null)}/background")
        if (file.exists() && file.listFiles()?.isNotEmpty() == true) {
            val imageFile = file.listFiles()?.get(0)
            fragment_tablayout_timeline_background.setNullTint()
            Glide.with(fragment_tablayout_timeline_background)
                .load(imageFile)
                .into(fragment_tablayout_timeline_background)
        } else {
            fragment_tablayout_timeline_background.setImageDrawable(null)
        }
    }

    private fun initRecyclerView() {
        fragment_tablayout_timeline_recyclerview.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(context)
            adapter = timelineRecyclerViewAdapter
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        // 保存する
        outState.apply {
            putSerializable("list", timeLineItemDataList)
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