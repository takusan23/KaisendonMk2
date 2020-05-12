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
import io.github.takusan23.kaisendonmk2.DataClass.StatusData
import io.github.takusan23.kaisendonmk2.MainActivity
import io.github.takusan23.kaisendonmk2.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// タイムライン表示RecyclerView
class TimelineRecyclerViewAdapter(val statusDataList: ArrayList<StatusData>) : RecyclerView.Adapter<TimelineRecyclerViewAdapter.ViewHolder>() {

    lateinit var mainActivity: MainActivity

    val customEmoji = CustomEmoji()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.adapter_timeline, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.apply {

            val context = nameTextView.context
            val status = statusDataList.get(position) ?: return

            // トゥート表示
            idTextView.text = "@${status.accountData.acct}"
            setCustomEmojiTextView(this, status)
            Glide.with(avatarImageView)
                .load(status.accountData.avatarStatic)
                .apply(RequestOptions.bitmapTransform(RoundedCorners(10)))
                .into(avatarImageView)

            // お気に入り、ブースト
            initFav(this, status)
            initBoost(this, status)
            setFavBoostIcon(this, status)

        }
    }

    // カスタム絵文字対応！？
    private fun setCustomEmojiTextView(viewHolder: ViewHolder, status: StatusData) {
        customEmoji.setCustomEmoji(viewHolder.nameTextView, status.accountData.displayName, status.accountData.allEmoji)
        customEmoji.setCustomEmoji(viewHolder.contentTextView, status.content, status.allEmoji)
    }

    // ふぁぼ/ブーストした後など値を反映させる
    private fun setFavBoostIcon(viewHolder: ViewHolder, status: StatusData) {
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

    private fun initBoost(viewHolder: ViewHolder, status: StatusData) {
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

    private fun initFav(viewHolder: ViewHolder, status: StatusData) {
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

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nameTextView = itemView.findViewById<TextView>(R.id.adapter_timeline_name)
        val idTextView = itemView.findViewById<TextView>(R.id.adapter_timeline_id)
        val contentTextView = itemView.findViewById<TextView>(R.id.adapter_timeline_content)
        val avatarImageView = itemView.findViewById<ImageView>(R.id.adapter_timeline_avatar)
        val favoutiteButton = itemView.findViewById<Button>(R.id.adapter_timeline_favourite)
        val boostButton = itemView.findViewById<Button>(R.id.adapter_timeline_boost)
    }

    override fun getItemCount(): Int {
        return statusDataList.size
    }

}