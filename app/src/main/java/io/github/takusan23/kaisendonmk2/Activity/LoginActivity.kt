package io.github.takusan23.kaisendonmk2.Activity

import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.preference.PreferenceManager
import androidx.room.Room
import io.github.takusan23.kaisendonmk2.MastodonAPI.AppsAPI
import io.github.takusan23.kaisendonmk2.DataClass.AppData
import io.github.takusan23.kaisendonmk2.DetaBase.Entity.CustomTimeLineDBEntity
import io.github.takusan23.kaisendonmk2.DetaBase.RoomDataBase.CustomTimeLineDB
import io.github.takusan23.kaisendonmk2.MainActivity
import io.github.takusan23.kaisendonmk2.MisskeyAPI.MisskeyLoginAPI
import io.github.takusan23.kaisendonmk2.R
import kotlinx.android.synthetic.main.activity_login.*
import kotlinx.coroutines.*
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
            val instanceName = activity_login_instance.text.toString()
            val viaName = activity_login_via.text.toString()
            GlobalScope.launch(Dispatchers.Main) {
                // Misskeyと分岐
                if (activity_login_misskey.isChecked) {
                    // Misskey
                    val misskeyLoginAPI = MisskeyLoginAPI(instanceName)
                    // アプリ作成
                    val secret = withContext(Dispatchers.IO) {
                        misskeyLoginAPI.createMisskeyApp(viaName).await()
                    } ?: return@launch

                    // 保存
                    prefSetting.edit {
                        putString("misskey_instance", instanceName)
                        putString("misskey_secret", secret)
                    }
                    // 認証画面表示
                    val data = withContext(Dispatchers.IO) {
                        misskeyLoginAPI.sessionGenerate(secret).await()
                    } ?: return@launch
                    // 保存
                    prefSetting.edit {
                        putString("misskey_token", data.token)
                    }
                    // ブラウザ起動
                    val intent = Intent(Intent.ACTION_VIEW, data.url.toUri())
                    startActivity(intent)

                } else {
                    // Mastodon
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
    }

    override fun onResume() {
        super.onResume()
        // ユーザーの許可が降りたとき
        if (intent.data != null) {
            GlobalScope.launch(Dispatchers.Main) {
                // Misskeyと分岐
                if (intent.data!!.getQueryParameter("token") != null) {
                    // Misskey
                    val token = intent.data!!.getQueryParameter("token") ?: return@launch
                    // インスタンス、シークレット取得
                    val instance = prefSetting.getString("misskey_instance", "")!!
                    val misskeyLoginAPI = MisskeyLoginAPI(instance)
                    val secret = prefSetting.getString("misskey_secret", "")!!
                    // アクセストークン生成に使う値取得
                    val tmpToken = withContext(Dispatchers.IO) {
                        misskeyLoginAPI.sessionUserkey(secret, token).await()
                    } ?: return@launch
                    // SHA-256してアクセストークン作成
                    val accessToken = misskeyLoginAPI.generateAccessToken(secret, tmpToken)
                    // 保存。
                    saveAccount(instance, accessToken, "misskey")
                    // AllTimeLine追加
                    saveCustomMenuTimeLine(instance, accessToken, "misskey").await()
                    // MainActivity
                    val intent = Intent(this@LoginActivity, MainActivity::class.java)
                    startActivity(intent)
                } else {
                    // Mastodon
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
                    saveCustomMenuTimeLine(instanceName, token).await()
                    // MainActivity
                    val intent = Intent(this@LoginActivity, MainActivity::class.java)
                    startActivity(intent)
                }
            }
        }
    }

    // カスタムTLに追加
    private fun saveCustomMenuTimeLine(instanceName: String, token: String, service: String = "mastodon") = GlobalScope.async {
        GlobalScope.launch {
            val db = Room.databaseBuilder(applicationContext, CustomTimeLineDB::class.java, "CustomTimeLineDB").build()
            val customTimeLineDBDao = db.customTimeLineDBDao()
            // URLと名前
            val nameList = arrayListOf(getString(R.string.home), getString(R.string.notification), getString(R.string.local_tl))
            val urlList = arrayListOf("home", "notification", "local")
            // TL追加
            repeat(3) {
                // timelineのJSON
                val jsonObject = JSONObject().apply {
                    put("instance", instanceName)
                    put("token", token)
                    put("service", service)
                    put("load_tl", urlList[it])
                    put("background_color", "")
                    put("text_color", "")
                    put("is_enable", false)
                    put("name", "${nameList[it]} | $instanceName")
                }
                // DBに追加
                val customTimeLineDBEntity = CustomTimeLineDBEntity(
                    name = "${nameList[it]} | $instanceName",
                    instance = instanceName,
                    token = token,
                    timeline = jsonObject.toString(),
                    isEnable = false,
                    service = service
                )
                customTimeLineDBDao.insert(customTimeLineDBEntity)
            }
        }
    }

    private fun saveAccount(instanceName: String, token: String, service: String = "mastodon") {
        val accountJSONString = prefSetting.getString("account_json", null)
        if (accountJSONString != null) {
            // 追加
            val jsonArray = JSONArray(accountJSONString)
            jsonArray.put(JSONObject().apply {
                put("instance", instanceName)
                put("token", token)
                put("service", service)
            })
            // 保存
            prefSetting.edit { putString("account_json", jsonArray.toString()) }
        } else {
            // 初回
            val jsonArray = JSONArray()
            jsonArray.put(JSONObject().apply {
                put("instance", instanceName)
                put("token", token)
                put("service", service)
            })
            prefSetting.edit { putString("account_json", jsonArray.toString()) }
        }
    }

}
