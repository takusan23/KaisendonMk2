package io.github.takusan23.kaisendonmk2.Adapter

import android.content.Intent
import android.content.SharedPreferences
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Room
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.snackbar.Snackbar
import io.github.takusan23.kaisendonmk2.BottomFragment.AccountListBottomFragment
import io.github.takusan23.kaisendonmk2.BottomFragment.DialogBottomSheet
import io.github.takusan23.kaisendonmk2.DataClass.AccountListData
import io.github.takusan23.kaisendonmk2.DetaBase.Dao.CustomTimeLineDBDao
import io.github.takusan23.kaisendonmk2.DetaBase.RoomDataBase.CustomTimeLineDB
import io.github.takusan23.kaisendonmk2.Fragment.TimeLineFragment
import io.github.takusan23.kaisendonmk2.MainActivity
import io.github.takusan23.kaisendonmk2.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray

/**
 * アカウント一覧Adapter
 * @param accountDataList AccountDataの配列
 * */
class AccountListAdapter(val accountDataList: ArrayList<AccountListData>) : RecyclerView.Adapter<AccountListAdapter.AccountListAdapterViewHolder>() {

    lateinit var prefSetting: SharedPreferences
    lateinit var accountListBottomFragment: AccountListBottomFragment

    // カスタムタイムラインのデータベース
    lateinit var customTimeLineDB: CustomTimeLineDB
    lateinit var customTimeLineDBDao: CustomTimeLineDBDao

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AccountListAdapterViewHolder {
        val holder = AccountListAdapterViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.adapter_account_list, parent, false))
        return holder
    }

    override fun getItemCount(): Int = accountDataList.size

    override fun onBindViewHolder(holder: AccountListAdapterViewHolder, position: Int) {
        holder.apply {

            val context = nameTextView.context
            // しょきか
            if (!::prefSetting.isInitialized) {
                prefSetting = PreferenceManager.getDefaultSharedPreferences(context)
                customTimeLineDB = Room.databaseBuilder(context, CustomTimeLineDB::class.java, "CustomTimeLineDB").build()
                customTimeLineDBDao = customTimeLineDB.customTimeLineDBDao()
            }
            val accountJSONString = prefSetting.getString("account_json", null)

            // 名前
            val accountListData = accountDataList[position]
            if (accountListData.mastodonAccountData != null) {
                val account = accountListData.mastodonAccountData
                nameTextView.text = "${account.displayName}\n${account.instanceToken.instance}"
            } else if (accountListData.misskeyUserData != null) {
                val account = accountListData.misskeyUserData
                nameTextView.text = "${account.name}\n${account.instanceToken.instance}"
            }

            // Avatar
            val url = if (accountListData.service == "mastodon") {
                accountListData.mastodonAccountData!!.avatar
            } else {
                accountListData.misskeyUserData!!.avatarUrl
            }
            Glide.with(avatarImageView)
                .load(url)
                .apply(RequestOptions.bitmapTransform(RoundedCorners(10)))
                .into(avatarImageView)

            // 削除ボタン
            deleteButton.setOnClickListener {
                // 本当に消していい？
                val items = arrayListOf<DialogBottomSheet.DialogBottomSheetItem>().apply {
                    add(DialogBottomSheet.DialogBottomSheetItem(context.getString(R.string.delete)))
                    add(DialogBottomSheet.DialogBottomSheetItem(context.getString(R.string.cancel)))
                }
                DialogBottomSheet(context.getString(R.string.account_delete_message), items) { i, bottomSheetDialogFragment ->
                    val jsonArray = JSONArray(accountJSONString)
                    // 削除する
                    for (i in 0 until jsonArray.length()) {
                        val jsonObject = jsonArray.getJSONObject(i)
                        val token = jsonObject.getString("token")
                        if (token == accountListData.instanceToken.token) {
                            // 同じのあった
                            jsonArray.remove(i)
                        }
                    }
                    // 保存する
                    prefSetting.edit { putString("account_json", jsonArray.toString()) }
                    // DBからも消す
                    GlobalScope.launch(Dispatchers.Main) {
                        withContext(Dispatchers.IO) {
                            customTimeLineDBDao.accountDelete(accountListData.instanceToken.token)
                        }
                        // 再読み込み
                        accountListBottomFragment.loadAccount()
                        // 再起動
                        accountListBottomFragment.activity?.finish()
                        val intent = Intent(context, MainActivity::class.java)
                        context.startActivity(intent)
                    }
                }.show(accountListBottomFragment.parentFragmentManager, "delete")
            }

        }
    }

    // ViewHolder
    inner class AccountListAdapterViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nameTextView = itemView.findViewById<TextView>(R.id.adapter_account_list_account_name)
        val deleteButton = itemView.findViewById<Button>(R.id.adapter_account_list_delete)
        val avatarImageView = itemView.findViewById<ImageView>(R.id.adapter_account_list_account_imageview)
    }
}
