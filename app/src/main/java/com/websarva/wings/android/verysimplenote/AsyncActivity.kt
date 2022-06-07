package com.websarva.wings.android.verysimplenote

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ListView
import android.widget.SimpleAdapter
import android.widget.TextView
import androidx.annotation.UiThread
import androidx.annotation.WorkerThread
import androidx.core.os.HandlerCompat
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.lang.StringBuilder
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.util.concurrent.Executors

class AsyncActivity : AppCompatActivity() {
    // クラス内のprivate定数を宣言するためにcompanion objectブロックとする
    companion object {
        // ログに記載するタグ用の文字列
        private const val DEBUG_TAG = "AsyncSample"
        // お天気情報のURL
        private const val WEATHERINFO_URL = "https://api.openweathermap.org/data/2.5/weather?lang=ja"
        // お天気APIにアクセスするためのAPIキー
        private const val APP_ID = "764cc1394cb834790e67433faa15157f"
    }

    // リストビューに表示させるリストデータ
    private var _list: MutableList<MutableMap<String, String>> = mutableListOf()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_async)

        _list = createList()

        val lvCityList = findViewById<ListView>(R.id.lvCityList)
        val from = arrayOf("name")
        val to = intArrayOf(android.R.id.text1)
        val adapter = SimpleAdapter(this@AsyncActivity, _list,
            android.R.layout.simple_list_item_1, from, to)
        lvCityList.adapter = adapter
        lvCityList.onItemClickListener = ListItemClickListener()

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    // リストがタップされた時の処理が記述されたリスナクラス
    private inner class ListItemClickListener: AdapterView.OnItemClickListener {
        override fun onItemClick(parent: AdapterView<*>, view: View, position: Int, id: Long) {
            val item = _list[position]
            val q = item.get("q")
            q?.let {
                val urlFull = "$WEATHERINFO_URL&q=$q&appid=$APP_ID"
                receiveWeatherInfo(urlFull)
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // 戻り値用の変数を初期値trueで用意
        var returnVal = true
        // 選択されたメニューが「戻る」の場合、アクティビティを終了
        if (item.itemId == android.R.id.home) {
            finish()
        }
        else {
            returnVal = super.onOptionsItemSelected(item)
        }

        return returnVal
    }

    // リストビューに表示させる天気ポイントリストデータを生成するメソッド
    private fun createList(): MutableList<MutableMap<String, String>> {
        var list: MutableList<MutableMap<String, String>> = mutableListOf()

        var city = mutableMapOf("name" to "大阪", "q" to "Osaka")
        list.add(city)
        city = mutableMapOf("name" to "神戸", "q" to "Kobe")
        list.add(city)
        city = mutableMapOf("name" to "京都", "q" to "Kyoto")
        list.add(city)
        city = mutableMapOf("name" to "大津", "q" to "Otsu")
        list.add(city)
        city = mutableMapOf("name" to "奈良", "q" to "Nara")
        list.add(city)
        city = mutableMapOf("name" to "和歌山", "q" to "Wakayama")
        list.add(city)
        city = mutableMapOf("name" to "姫路", "q" to "Himeji")
        list.add(city)

        return list
    }

    // お天気情報の取得処理を行うメソッド
    @UiThread// UIスレッドであることを保証するアノテーション
    private fun receiveWeatherInfo(urlFull: String) {
        val handler = HandlerCompat.createAsync(mainLooper)
        val backgroundReciver = WeatherInfoBackgroundReciver(handler, urlFull)
        val executeService = Executors.newSingleThreadExecutor()
        // 引数のRunnable実装クラスのrun()メソッドが非同期で処理される
        executeService.submit(backgroundReciver)
    }

    // 非同期でお天気情報APIにアクセスするためのクラス
    private inner class WeatherInfoBackgroundReciver(handler: Handler, url:String): Runnable {
        // ハンドラオブジェクト
        private val _handler = handler
        // お天気情報を取得するURL
        private val _url = url

        @WorkerThread// ワーカースレッドであることを保証するアノテーション
        override fun run() {
            // 天気情報サービスから取得したJSON文字列。天気情報が格納されている
            var result = ""
            // URLオブジェクトを生成
            val url = URL(_url)
            // URLオブジェクトからHttpURLConnectionオブジェクトを取得
            val con = url.openConnection() as? HttpURLConnection
            // conがnullではない場合
            con?.let {
                try {
                    // 接続に使っても良い時間を指定
                    it.connectTimeout = 1000
                    // データ取得に使っても良い時間
                    it.readTimeout = 1000
                    // HTTP接続メソッドをGETに設定
                    it.requestMethod = "GET"
                    // 接続
                    it.connect()

                    // HttpURLConnectionオブジェクトからレスポンスデータを取得
                    val stream = it.inputStream
                    // レスポンスデータであるInputStreamを文字列に変換
                    result = is2String(stream)
                    // InputStreamオブジェクトを開放
                    stream.close()
                }
                catch (ex: SocketTimeoutException) {
                    Log.w(DEBUG_TAG, "通信タイムアウト", ex)
                }
                // HttpURLConnectionオブジェクトを開放
                it.disconnect()
            }
            val postExecuter = WeatherInfoPostExecutor(result)
            // Handlerオブジェクトを生成した元スレッドで引数のRunnable実装クラスのrun()メソッドが実行される
            _handler.post(postExecuter)
        }

        private fun is2String(stream: InputStream): String {
            val sb = StringBuilder()
            val reader = BufferedReader(InputStreamReader(stream, "UTF-8"))
            var line = reader.readLine()
            while(line != null) {
                sb.append(line)
                line = reader.readLine()
            }
            reader.close()
            return sb.toString()
        }
    }

    // 非同期でお天気情報を取得した後にUIスレッドでその情報を表示するためのクラス
    private inner class WeatherInfoPostExecutor(result: String): Runnable {
        // 取得したお天気情報JSON文字列
        private val _result = result

        @UiThread// UIスレッドであることを保証するアノテーション
        override fun run() {
            // ルートJSONオブジェクトを生成
            val rootJSON = JSONObject(_result)
            // 都市名文字列を取得
            val cityName = rootJSON.getString("name")
            // 緯度経度情報JSONオブジェクトを取得
            val coordJSON = rootJSON.getJSONObject(("coord"))
            // 緯度情報の文字列を取得
            val latitude = coordJSON.getString("lat")
            // 軽度情報の文字列を取得
            val longitude = coordJSON.getString("lon")
            // 天気情報JSON配列オブジェクトを取得
            val weatherJSONArray = rootJSON.getJSONArray("weather")
            // 現在の天気情報JSONオブジェクトを取得
            val weatherJSON = weatherJSONArray.getJSONObject(0)
            // 現在の天気情報文字列を取得
            val weather = weatherJSON.getString("description")

            // 画面に表示する「〇〇の天気」文字列を生成
            val telop = "${cityName}の天気"
            // 天気の詳細情報を表示する文字列を生成
            val desc = "現在は${weather}です。\n緯度は${latitude}で経度は${longitude}度です。"

            // 天気情報を表示するTextViewを取得
            val tvWeatherTelop = findViewById<TextView>(R.id.tvWeatherTelop)
            val tvWeatherDesc = findViewById<TextView>(R.id.tvWeatherDesc)
            // 天気情報を表示
            tvWeatherTelop.text = telop
            tvWeatherDesc.text = desc
        }
    }
}