package io.github.takusan23.kaisendonmk2.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.core.text.HtmlCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.button.MaterialButton
import io.github.takusan23.kaisendonmk2.API.StatusAPI
import io.github.takusan23.kaisendonmk2.CustomEmoji.CustomEmoji
import io.github.takusan23.kaisendonmk2.DataClass.EmojiData
import io.github.takusan23.kaisendonmk2.DataClass.StatusData
import io.github.takusan23.kaisendonmk2.DataClass.TimeLineItemData
import io.github.takusan23.kaisendonmk2.MainActivity
import io.github.takusan23.kaisendonmk2.R
import io.github.takusan23.kaisendonmk2.TimeLine.toTimeFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// タイムライン表示RecyclerView
class TimelineRecyclerViewAdapter(val timeLineItemDataList: ArrayList<TimeLineItemData>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    lateinit var mainActivity: MainActivity

    val customEmoji = CustomEmoji()

    // 詳細表示してるCardViewのトゥートID配列
    private val infoVISIBLEList = arrayListOf<String>()

    // レイアウトの定数（onCreateViewHolder()で使う）
    companion object {
        val TOOT_LAYOUT = 0
        val NOTIFICATION_LAYOUT = 1
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        // レイアウト分岐
        val view = when (viewType) {
            TOOT_LAYOUT -> TootViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.adapter_timeline, parent, false))
            NOTIFICATION_LAYOUT -> NotificationViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.adapter_notification, parent, false))
            else -> TootViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.adapter_timeline, parent, false))
        }
        return view
    }

    // 通知と投稿で分岐させる
    override fun getItemViewType(position: Int): Int {
        return when {
            timeLineItemDataList[position].statusData != null -> TOOT_LAYOUT
            timeLineItemDataList[position].notificationData != null -> NOTIFICATION_LAYOUT
            else -> TOOT_LAYOUT
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is TootViewHolder) {
            holder.apply {
                // トゥート
                val context = nameTextView.context
                val status = timeLineItemDataList.get(position).statusData ?: return
                // TL名
                timeLineName.text = timeLineItemDataList.get(position).allTimeLineData.timeLineName
                // トゥート表示
                idTextView.text = "@${status.accountData.acct}"
                customEmoji.setCustomEmoji(nameTextView, status.accountData.displayName, status.accountData.allEmoji)
                customEmoji.setCustomEmoji(contentTextView, status.content, status.allEmoji)
                Glide.with(avatarImageView)
                    .load(status.accountData.avatarStatic)
                    .apply(RequestOptions.bitmapTransform(RoundedCorners(10)))
                    .into(avatarImageView)

                // お気に入り、ブースト
                initFav(this, status)
                initBoost(this, status)
                setFavBoostIcon(this, status)
                // 詳細表示
                initInfo(this, status)
            }
        } else if (holder is NotificationViewHolder) {
            holder.apply {
                // 通知
                val context = nameTextView.context
                val notificationData = timeLineItemDataList.get(position).notificationData ?: return
                // TL名
                timeLineName.text = timeLineItemDataList.get(position).allTimeLineData.timeLineName
                // 通知タイプ
                notificationTextView.text = when (notificationData.type) {
                    "favourite" -> context.getText(R.string.notification_favourite)
                    "reblog" -> context.getText(R.string.notification_reblog)
                    "mention" -> context.getText(R.string.notification_mention)
                    "follow" -> context.getText(R.string.notification_follow)
                    else -> context.getText(R.string.notification_favourite)
                }
                // アカウント情報
                idTextView.text = "@${notificationData.accountData.acct}"
                customEmoji.setCustomEmoji(nameTextView, notificationData.accountData.displayName, notificationData.accountData.allEmoji)
                Glide.with(avatarImageView)
                    .load(notificationData.accountData.avatarStatic)
                    .apply(RequestOptions.bitmapTransform(RoundedCorners(10)))
                    .into(avatarImageView)

                // statusあればトゥート表示
                if (notificationData.status != null) {
                    customEmoji.setCustomEmoji(contentTextView, notificationData.status.content, notificationData.status.allEmoji)
                }
            }
        }
    }

    private fun initInfo(tootViewHolder: TootViewHolder, status: StatusData) {
        tootViewHolder.apply {
            // 表示・非表示
            moreButton.setOnClickListener {
                if (infoTextView.visibility == View.GONE) {
                    infoTextView.visibility = View.VISIBLE
                    (moreButton as MaterialButton).icon =
                        infoTextView.context.getDrawable(R.drawable.ic_expand_less_black_24dp)
                    infoVISIBLEList.add(status.id)
                } else {
                    infoTextView.visibility = View.GONE
                    (moreButton as MaterialButton).icon =
                        infoTextView.context.getDrawable(R.drawable.ic_expand_more_black_24dp)
                    infoVISIBLEList.remove(status.id)
                }
            }
            // リサイクルされるので
            if (infoVISIBLEList.contains(status.id)) {
                infoTextView.visibility = View.VISIBLE
                (moreButton as MaterialButton).icon =
                    infoTextView.context.getDrawable(R.drawable.ic_expand_less_black_24dp)
            } else {
                infoTextView.visibility = View.GONE
                (moreButton as MaterialButton).icon =
                    infoTextView.context.getDrawable(R.drawable.ic_expand_more_black_24dp)
            }

            val text = """
                投稿日時：${status.createdAt.toTimeFormat()}
                トゥートID：${status.id}
            """.trimIndent()
            infoTextView.text = text
        }
    }

    // ふぁぼ/ブーストした後など値を反映させる
    private fun setFavBoostIcon(viewHolder: TootViewHolder, status: StatusData) {
        viewHolder.apply {
            val context = favoutiteButton.context
            // ファボ数
            favoutiteButton.text = status.favouritesCount.toString()
            boostButton.text = status.boostCount.toString()
            // ファボ済みならチェックマークに
            (favoutiteButton as MaterialButton).icon = if (status.isFavourited) {
                context.getDrawable(R.drawable.ic_done_black_24dp)
            } else {
                context.getDrawable(R.drawable.ic_star_border_black_24dp)
            }
            (boostButton as MaterialButton).icon = if (status.isBoosted) {
                context.getDrawable(R.drawable.ic_done_black_24dp)
            } else {
                context.getDrawable(R.drawable.ic_repeat_black_24dp)
            }
        }
    }

    private fun initBoost(viewHolder: TootViewHolder, status: StatusData) {
        val context = viewHolder.boostButton.context
        // ブーストAPI叩く
        viewHolder.boostButton.setOnClickListener {
            mainActivity.showSnackBar(context.getString(R.string.boost_message), context.getString(R.string.boost)) {
                GlobalScope.launch(Dispatchers.Main) {
                    val statusAPI = StatusAPI(status.instanceToken)
                    val response = withContext(Dispatchers.IO) {
                        if (!status.isBoosted) {
                            statusAPI.postStatusBoost(status.id, status.instanceToken).await()
                        } else {
                            statusAPI.postDeleteStatusBoost(status.id, status.instanceToken).await()
                        }
                    }
                    if (!response.isSuccessful) {
                        mainActivity.showSnackBar("${context.getString(R.string.boost_ok)}：${status.id}")
                        // 反転
                        status.isBoosted = !status.isBoosted
                        // UI反映
                        setFavBoostIcon(viewHolder, status)
                    } else {
                        mainActivity.showSnackBar("${context.getString(R.string.error)}：${response.code}")
                    }
                }
            }
        }
    }

    private fun initFav(viewHolder: TootViewHolder, status: StatusData) {
        val context = viewHolder.favoutiteButton.context
        viewHolder.favoutiteButton.setOnClickListener {
            // 本当に叩いていいか聞く
            mainActivity.showSnackBar(context.getString(R.string.favourite_message), context.getString(R.string.favourite)) {
                GlobalScope.launch(Dispatchers.Main) {
                    // ふぁぼAPI叩く
                    val statusAPI = StatusAPI(status.instanceToken)
                    // コルーチンガチ有能
                    val response = withContext(Dispatchers.IO) {
                        if (!status.isFavourited) {
                            statusAPI.postStatusFav(status.id, status.instanceToken).await()// ふぁぼ
                        } else {
                            statusAPI.postDeleteStatusFav(status.id, status.instanceToken).await()// ふぁぼ取り消し
                        }
                    }
                    if (response.isSuccessful) {
                        mainActivity.showSnackBar("${context.getString(R.string.favourite_ok)}：${status.id}")
                        // 反転
                        status.isFavourited = !status.isFavourited
                        // UI反映
                        setFavBoostIcon(viewHolder, status)
                    } else {
                        mainActivity.showSnackBar("${context.getString(R.string.error)}：${response.code}")
                    }
                }
            }
        }
    }

    // トゥートViewHolder
    inner class TootViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val timeLineName = itemView.findViewById<TextView>(R.id.adapter_timeline_name)
        val nameTextView = itemView.findViewById<TextView>(R.id.adapter_timeline_user_name)
        val idTextView = itemView.findViewById<TextView>(R.id.adapter_timeline_id)
        val contentTextView = itemView.findViewById<TextView>(R.id.adapter_timeline_content)
        val avatarImageView = itemView.findViewById<ImageView>(R.id.adapter_timeline_avatar)
        val favoutiteButton = itemView.findViewById<Button>(R.id.adapter_timeline_favourite)
        val boostButton = itemView.findViewById<Button>(R.id.adapter_timeline_boost)

        // 詳細表示
        val moreButton = itemView.findViewById<Button>(R.id.adapter_timeline_more)
        val infoTextView = itemView.findViewById<TextView>(R.id.adapter_timeline_info_textview)
    }

    // 通知ViewHolder
    inner class NotificationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val notificationTextView =
            itemView.findViewById<TextView>(R.id.adapter_notification_type)
        val timeLineName = itemView.findViewById<TextView>(R.id.adapter_timeline_name)
        val nameTextView = itemView.findViewById<TextView>(R.id.adapter_timeline_user_name)
        val idTextView = itemView.findViewById<TextView>(R.id.adapter_timeline_id)
        val contentTextView = itemView.findViewById<TextView>(R.id.adapter_timeline_content)
        val avatarImageView = itemView.findViewById<ImageView>(R.id.adapter_timeline_avatar)
    }

    override fun getItemCount(): Int {
        return timeLineItemDataList.size
    }

}