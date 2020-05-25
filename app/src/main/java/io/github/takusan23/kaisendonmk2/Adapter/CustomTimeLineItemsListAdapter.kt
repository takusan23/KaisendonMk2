package io.github.takusan23.kaisendonmk2.Adapter

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Switch
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Room
import io.github.takusan23.kaisendonmk2.BottomFragment.LoadTimeLineEditBottomSheet
import io.github.takusan23.kaisendonmk2.BottomFragment.LoadTimeLineListBottomSheet
import io.github.takusan23.kaisendonmk2.DetaBase.Dao.CustomTimeLineDBDao
import io.github.takusan23.kaisendonmk2.DetaBase.Entity.CustomTimeLineDBEntity
import io.github.takusan23.kaisendonmk2.DetaBase.RoomDataBase.CustomTimeLineDB
import io.github.takusan23.kaisendonmk2.MainActivity
import io.github.takusan23.kaisendonmk2.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * タイムラインの構成一覧表示のAdapter
 * */
class CustomTimeLineItemsListAdapter(val customTimeLineList: ArrayList<CustomTimeLineDBEntity>) : RecyclerView.Adapter<CustomTimeLineItemsListAdapter.ViewHolder>() {

    lateinit var mainActivity: MainActivity
    lateinit var loadTimeLineListBottomSheet: LoadTimeLineListBottomSheet
    lateinit var customTimeLineDB: CustomTimeLineDB
    lateinit var customTimeLineDBDao: CustomTimeLineDBDao

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.adapter_timeline_setting, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return customTimeLineList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.apply {
            // Context
            val context = allTimeLineAdapterSwitch.context
            if (!::customTimeLineDB.isInitialized) {
                customTimeLineDB = Room.databaseBuilder(context, CustomTimeLineDB::class.java, "CustomTimeLineDB").build()
                customTimeLineDBDao = customTimeLineDB.customTimeLineDBDao()
            }

            // 値入れる
            val allTimeLineData = customTimeLineList[position]
            allTimeLineAdapterSwitch.isChecked = allTimeLineData.isEnable
            allTimeLineAdapterSwitch.text = allTimeLineData.name

            // スイッチ
            allTimeLineAdapterSwitch.setOnCheckedChangeListener { buttonView, isChecked ->
                // なんと isPressed を使うことでなにもしてないのにリスナーが動いた時でも大丈夫（リサイクラービューは使い回されるので）
                if (buttonView.isPressed) {
                    // 有効/無効を反転
                    customTimeLineList[position].isEnable = isChecked
                    GlobalScope.launch {
                        val db = Room.databaseBuilder(context, CustomTimeLineDB::class.java, "CustomTimeLineDB").build()
                        val item = db.customTimeLineDBDao().findById(allTimeLineData.id)
                        item.isEnable = isChecked
                        customTimeLineDBDao.update(item)
                    }
                }
            }

            // 見た目
            allTimeLineAdapterStyle.setOnClickListener {
                val loadTimeLineEditBottomSheet = LoadTimeLineEditBottomSheet()
                val bundle = Bundle().apply {
                    putString("name", allTimeLineData.name)
                }
                loadTimeLineEditBottomSheet.arguments = bundle
                loadTimeLineEditBottomSheet.show(mainActivity.supportFragmentManager, "style")
            }

        }
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val allTimeLineAdapterSwitch = itemView.findViewById<Switch>(R.id.adapter_all_tl_setting_switch)
        val allTimeLineAdapterStyle = itemView.findViewById<ImageView>(R.id.adapter_tl_setting_color)
    }

}
