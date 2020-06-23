package io.github.takusan23.kaisendonmk2.Fragment

import android.content.SharedPreferences
import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import androidx.room.Room
import com.bumptech.glide.Glide
import io.github.takusan23.kaisendonmk2.MastodonAPI.TimeLineAPI
import io.github.takusan23.kaisendonmk2.Adapter.TimelineRecyclerViewAdapter
import io.github.takusan23.kaisendonmk2.DataClass.TimeLineItemData
import io.github.takusan23.kaisendonmk2.DetaBase.RoomDataBase.CustomTimeLineDB
import io.github.takusan23.kaisendonmk2.JSONParse.MisskeyParser
import io.github.takusan23.kaisendonmk2.JSONParse.TimeLineParser
import io.github.takusan23.kaisendonmk2.MainActivity
import io.github.takusan23.kaisendonmk2.MastodonAPI.createInstanceToken
import io.github.takusan23.kaisendonmk2.MisskeyAPI.MisskeyTimeLineAPI
import io.github.takusan23.kaisendonmk2.R
import io.github.takusan23.kaisendonmk2.StreamingAPI.MisskeyStreamingAPI
import io.github.takusan23.kaisendonmk2.StreamingAPI.StreamingAPI
import io.github.takusan23.kaisendonmk2.TimeLine.isConnectionMobileData
import io.github.takusan23.kaisendonmk2.TimeLine.setNullTint
import io.github.takusan23.kaisendonmk2.TimeLine.toUnixTime
import kotlinx.android.synthetic.main.fragment_timeline.*
import kotlinx.coroutines.*
import org.json.JSONObject
import java.io.File

/**
 * タイムラインFragment。あけおめ
 * */
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

    // データベース
    val customTimeLineDB by lazy { Room.databaseBuilder(requireContext(), CustomTimeLineDB::class.java, "CustomTimeLineDB").build() }

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

            // 画面回転時
            if (savedInstanceState != null) {
                ArrayList(savedInstanceState.getSerializable("list") as ArrayList<TimeLineItemData>).forEach {
                    timeLineItemDataList.add(it)
                }
                timeLineAdapter.notifyDataSetChanged()
                GlobalScope.launch {
                    initAllTimeLine().await()
                    initAllTimeLineStreaming()
                }
            } else {
                // 初回実行時
                if (timeLineItemDataList.isEmpty()) {
                    GlobalScope.launch {
                        initAllTimeLine().await()
                        initAllTimeLineStreaming()
                    }
                }
            }

            fragment_timeline_swipe.setOnRefreshListener {
                GlobalScope.launch {
                    initAllTimeLine().await()
                }
            }

        }

    }


    // 表示するタイムライン読み込み
    fun initAllTimeLine() = GlobalScope.async(Dispatchers.Main) {
        // println("あれ")
        // くるくる
        timeLineItemDataList.clear()
        timeLineAdapter.notifyDataSetChanged()
        // val allTimeLineJSON = AllTimeLineJSON(context)
        withContext(Dispatchers.IO) {
            // 読み込む
            val dao = customTimeLineDB.customTimeLineDBDao()
            dao.getAll().forEach { customTimeLineEntity ->
                // タイムライン構成JSON
                // val timeLineData = CustomTimeLineDataJSON().parse(customTimeLineEntity.timeline)
                // 有効時 か Wi-Fi利用時のみはモバイルデータ以外かどうかを確認して それでなお 通知以外
                if (customTimeLineEntity.isEnable || customTimeLineEntity.isWiFiOnly == !isConnectionMobileData(context) && customTimeLineEntity.timeline != "notification") {
                    // 認証情報
                    val instanceToken = customTimeLineEntity.createInstanceToken()
                    // くるくるなかったら表示
                    withContext(Dispatchers.Main) {
                        if (fragment_timeline_swipe?.isRefreshing == false) {
                            fragment_timeline_swipe?.isRefreshing = true
                        }
                    }
                    if (customTimeLineEntity.service == "mastodon") {
                        // 取得件数
                        val limit = prefSetting.getString("setting_load_limit_mastodon", "40")?.toInt() ?: 40
                        // TL取得
                        val timeLineAPI = TimeLineAPI(instanceToken)
                        val timeLineParser = TimeLineParser()
                        val response = when (customTimeLineEntity.timeline) {
                            "home" -> timeLineAPI.getHomeTimeLine(limit)
                            "local" -> timeLineAPI.getLocalTimeLine(limit)
                            else -> timeLineAPI.getHomeTimeLine(limit)
                        }
                        // 成功したか
                        response.apply {
                            if (isSuccessful) {
                                // 追加
                                timeLineParser.parseTL(this.response?.body?.string(), instanceToken).forEach { statusData ->
                                    val timeLineItemData = TimeLineItemData(customTimeLineEntity, statusData)
                                    timeLineItemDataList.add(timeLineItemData)
                                }
                            } else {
                                // 失敗時
                                mainActivity.showSnackBar("${getString(R.string.error)}/ Mastodon Timeline API\n${ioException?.message}")
                            }
                        }
                    } else {
                        // Misskey TL取得
                        // 取得件数
                        val limit = prefSetting.getString("setting_load_limit_misskey", "100")?.toInt() ?: 100
                        val misskeyTimeLineAPI = MisskeyTimeLineAPI(instanceToken)
                        val misskeyParser = MisskeyParser()
                        val response = when (customTimeLineEntity.timeline) {
                            "home" -> misskeyTimeLineAPI.getHomeNotesTimeLine(limit).await()
                            "local" -> misskeyTimeLineAPI.getLocalNotesTimeLine(limit).await()
                            else -> misskeyTimeLineAPI.getHomeNotesTimeLine(limit).await()
                        }
                        // 成功したか
                        response.apply {
                            if (isSuccessful) {
                                // 追加
                                misskeyParser.parseTimeLine(this.response?.body?.string(), instanceToken).forEach { misskeyNoteData ->
                                    val timeLineItemData = TimeLineItemData(customTimeLineEntity, null, null, misskeyNoteData, null)
                                    timeLineItemDataList.add(timeLineItemData)
                                }
                            } else {
                                // 失敗時
                                mainActivity.showSnackBar("${getString(R.string.error)}/ Misskey Timeline API\n${ioException?.message}")
                            }
                        }
                    }
                }
            }
            withContext(Dispatchers.IO) {
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
            // 読み込む
            val dao = customTimeLineDB.customTimeLineDBDao()
            dao.getAll().forEach { customTimeLineEntity ->
                // タイムライン構成JSON
                // 有効時
                if (customTimeLineEntity.isEnable && customTimeLineEntity.isWiFiOnly == !isConnectionMobileData(context)) {
                    // 認証情報
                    val instanceToken = customTimeLineEntity.createInstanceToken()
                    if (customTimeLineEntity.service == "mastodon") {
                        // Mastodon
                        // TL取得
                        val streamingAPI = StreamingAPI(instanceToken)
                        val isHome = customTimeLineEntity.timeline.contains("home")
                        val isNotification = customTimeLineEntity.timeline.contains("notification")
                        val isLocal = customTimeLineEntity.timeline.contains("local")
                        when {
                            // ホームか通知なら
                            isHome || isNotification -> streamingAPI.streamingUser({ statusData ->
                                // タイムライン
                                if (isHome) {
                                    addStreamingTLItem(TimeLineItemData(customTimeLineEntity, statusData))
                                }
                            }) { notificationData ->
                                // 通知
                                if (isNotification) {
                                    addStreamingTLItem(TimeLineItemData(customTimeLineEntity, null, notificationData))
                                }
                            }
                            isLocal -> streamingAPI.streamingLocalTL { statusData ->
                                // タイムライン
                                addStreamingTLItem(TimeLineItemData(customTimeLineEntity, statusData))
                            }
                            else -> streamingAPI.streamingLocalTL { statusData ->
                                // タイムライン
                                addStreamingTLItem(TimeLineItemData(customTimeLineEntity, statusData))
                            }
                        }
                        streamingAPIList.add(streamingAPI)
                    } else {
                        // Misskey
                        val misskeyStreamingAPI = MisskeyStreamingAPI(instanceToken)
                        val misskeyParser = MisskeyParser()
                        val isHome = customTimeLineEntity.timeline.contains("home")
                        val isNotification = customTimeLineEntity.timeline.contains("notification")
                        val isLocal = customTimeLineEntity.timeline.contains("local")
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
                                    val noteData = misskeyParser.parseNote(jsonObject.getJSONObject("body").getJSONObject("body").toString(), instanceToken)
                                    addStreamingTLItem(TimeLineItemData(customTimeLineEntity, null, null, noteData, null))
                                }
                                "notification" -> {
                                    // 通知
                                    val notificationData = misskeyParser.parseNotification(jsonObject.getJSONObject("body").getJSONObject("body").toString(), instanceToken)
                                    addStreamingTLItem(TimeLineItemData(customTimeLineEntity, null, null, null, notificationData))
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
        val isAdded = addedIdList.any { id -> id == timeLineItemData.statusData?.id ?: timeLineItemData.misskeyNoteData!!.noteId }
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
                    // 追加済み
                    addedIdList.add(timeLineItemData.statusData?.id ?: timeLineItemData.misskeyNoteData!!.noteId)
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
            Glide.with(fragment_timeline_background).load(imageFile).into(fragment_timeline_background)
        } else {
            fragment_timeline_background.setImageDrawable(null)
        }
    }

    // フォントセット
    fun setFont() {
        val file = File("${context?.getExternalFilesDir(null)}/font.ttf")
        if (file.exists()) {
            timeLineAdapter.font = Typeface.createFromFile(file)
        }
    }

    private fun initRecyclerView() {
        fragment_timeline_recyclerview?.apply {
            setHasFixedSize(true)
            // なんかかっこいいやつ
            column = prefSetting.getString("setting_tl_column", "1")?.toInt() ?: 1
            layoutManager = StaggeredGridLayoutManager(column, StaggeredGridLayoutManager.VERTICAL)
            timeLineAdapter = TimelineRecyclerViewAdapter(timeLineItemDataList)
            if (::mainActivity.isInitialized) {
                timeLineAdapter.mainActivity = mainActivity
            }
            adapter = timeLineAdapter
            val itemDecoration = DividerItemDecoration(context, DividerItemDecoration.VERTICAL)
            addItemDecoration(itemDecoration)
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