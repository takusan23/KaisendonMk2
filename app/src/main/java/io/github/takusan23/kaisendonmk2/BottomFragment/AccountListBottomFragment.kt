package io.github.takusan23.kaisendonmk2.BottomFragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import io.github.takusan23.kaisendonmk2.Adapter.AccountListAdapter
import io.github.takusan23.kaisendonmk2.DataClass.AccountListData
import io.github.takusan23.kaisendonmk2.JSONParse.MisskeyParser
import io.github.takusan23.kaisendonmk2.JSONParse.TimeLineParser
import io.github.takusan23.kaisendonmk2.MastodonAPI.AccountAPI
import io.github.takusan23.kaisendonmk2.MisskeyAPI.MisskeyAccountAPI
import io.github.takusan23.kaisendonmk2.R
import io.github.takusan23.kaisendonmk2.TimeLine.loadMultiAccount
import kotlinx.android.synthetic.main.bottom_fragment_account_list.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * アカウント一覧BottomFragment
 * */
class AccountListBottomFragment : BottomSheetDialogFragment() {

    val accountDataList = arrayListOf<AccountListData>()
    val accountListAdapter = AccountListAdapter(accountDataList)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.bottom_fragment_account_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // RecyclerView初期化
        initRecyclerView()

        // 読み込み
        loadAccount()

    }

    fun loadAccount() {
        accountDataList.clear()
        loadMultiAccount(context).forEach {
            if (it.service == "mastodon") {
                // Mastodon
                GlobalScope.launch {
                    val accountResponse = AccountAPI(it).getVerifyCredentials().await()
                    val timeLineParser = TimeLineParser()
                    if (accountResponse.isSuccessful) {
                        val accountData = timeLineParser.parseAccount(accountResponse.body?.string()!!, it)
                        val data = AccountListData(service = "mastodon", instanceToken = it, mastodonAccountData = accountData)
                        accountDataList.add(data)
                    }
                    withContext(Dispatchers.Main) { accountListAdapter.notifyDataSetChanged() }
                }
            } else {
                // Misskey
                GlobalScope.launch {
                    val accountResponse = MisskeyAccountAPI(it).getMyAccount().await()
                    val misskeyParser = MisskeyParser()
                    if (accountResponse.isSuccessful) {
                        val userData = misskeyParser.parseUser(accountResponse.body?.string()!!, it)
                        val data = AccountListData(service = "mastodon", instanceToken = it, misskeyUserData = userData)
                        accountDataList.add(data)
                    }
                    withContext(Dispatchers.Main) { accountListAdapter.notifyDataSetChanged() }
                }
            }
        }
    }

    private fun initRecyclerView() {
        bottom_fragment_account_list_recyclerview.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(context)
            accountListAdapter.accountListBottomFragment = this@AccountListBottomFragment
            adapter = accountListAdapter
        }
    }

}