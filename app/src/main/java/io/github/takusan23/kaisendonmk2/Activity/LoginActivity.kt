package io.github.takusan23.kaisendonmk2.Activity

import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import io.github.takusan23.kaisendonmk2.API.AppsAPI
import io.github.takusan23.kaisendonmk2.API.InstanceToken
import io.github.takusan23.kaisendonmk2.DataClass.AllTimeLineData
import io.github.takusan23.kaisendonmk2.DataClass.AppData
import io.github.takusan23.kaisendonmk2.R
import io.github.takusan23.kaisendonmk2.TimeLine.AllTimeLineJSON
import kotlinx.android.synthetic.main.activity_login.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class LoginActivity : AppCompatActivity() {

    lateinit var prefSetting: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // タイトル
        supportActionBar?.title = getString(R.string.login)

        prefSetting = PreferenceManager.getDefaultSharedPreferences(this)

        // ログインする
        activity_login_login.setOnClickListener {
            GlobalScope.launch(Dispatchers.Main) {
                val instanceName = activity_login_instance.text.toString()
                val viaName = activity_login_via.text.toString()
                val appsAPI = AppsAPI(instanceName)
                // アプリ作成
                val data = withContext(Dispatchers.IO) {
                    appsAPI.createApp(viaName).await()
                } ?: return@launch
                // 保存
                prefSetting.edit {
                    putString("client_id", data.clientId)
                    putString("client_secret", data.clientSecret)
                    putString("redirect_url", data.redirectUrl)
                    putString("register_instance", instanceName)
                }
                // 認証画面出す
                val url =
                    Uri.parse("https://$instanceName/oauth/authorize?client_id=${data.clientId}&redirect_uri=${data.redirectUrl}&response_type=code&scope=read%20write%20follow")
                val intent = Intent(Intent.ACTION_VIEW, url)
                startActivity(intent)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // ユーザーの許可が降りたとき
        if (intent.data != null) {
            GlobalScope.launch(Dispatchers.Main) {
                // コード
                val code = intent.data!!.getQueryParameter("code") ?: return@launch
                // アプリ作成時のデータ
                val instanceName = prefSetting.getString("register_instance", "") ?: return@launch
                val clientId = prefSetting.getString("client_id", "") ?: return@launch
                val clientSecret = prefSetting.getString("client_secret", "") ?: return@launch
                val redirectUrl = prefSetting.getString("redirect_url", "") ?: return@launch
                val appData = AppData(clientId, clientSecret, redirectUrl)
                // アクセストークン取得
                val appsAPI = AppsAPI(instanceName)
                val token = withContext(Dispatchers.IO) {
                    appsAPI.getAccessToken(instanceName, appData, code).await()
                } ?: return@launch
                // 保存。
                saveAccount(instanceName, token)
                // AllTimeLine追加
                saveAllTimeLine(instanceName, token)
                finish()
            }
        }
    }

    // AllTimeLineの設定
    private fun saveAllTimeLine(instanceName: String, token: String, service: String = "mastodon") {
        // 今までのAllTimeLineの配列
        val allTimeLineJSON = AllTimeLineJSON(this)
        val list = allTimeLineJSON.loadTimeLineSettingJSON()
        // ログイン情報
        val instanceToken = InstanceToken(instanceName, token, service)
        // URLと名前
        val nameList =
            arrayListOf(getString(R.string.home_notification), getString(R.string.local_tl))
        val urlList = arrayListOf("home_notification", "local")
        repeat(2) {
            // AllTimeLineDataのオブジェクト
            val allTimeLineData = AllTimeLineData(
                instanceToken,
                service,
                "${nameList[it]} | $instanceName",
                urlList[it],
                "",
                "",
                false
            )
            list.add(allTimeLineData)
        }
        // 保存
        allTimeLineJSON.saveTimeLineSettingJSON(list)
    }

    private fun saveAccount(instanceName: String, token: String) {
        val accountJSONString = prefSetting.getString("account_json", null)
        if (accountJSONString != null) {
            // 追加
            val jsonArray = JSONArray(accountJSONString)
            jsonArray.put(JSONObject().apply {
                put("instance", instanceName)
                put("token", token)
                put("service", "mastodon")
            })
            // 保存
            prefSetting.edit { putString("account_json", jsonArray.toString()) }
        } else {
            // 初回
            val jsonArray = JSONArray()
            jsonArray.put(JSONObject().apply {
                put("instance", instanceName)
                put("token", token)
                put("service", "mastodon")
            })
            prefSetting.edit { putString("account_json", jsonArray.toString()) }
        }
    }

}
