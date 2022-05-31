package com.websarva.wings.android.verysimplenote

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.os.Bundle
import android.view.ContextMenu
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ListView
import android.widget.SimpleAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

class MainActivity : AppCompatActivity() {
    companion object {
        // 通知チャンネルID文字列定数
        private const val CHANNEL_ID = "note_notification"
    }
    // SimpleAdapter 第4引数fromデータ用の用意
    private val _from = arrayOf("title", "content")
    // SimpleAdapter 第5引数toデータ用の用意
    private val _to = intArrayOf(android.R.id.text1, android.R.id.text2)
    // データベースヘルパーオブジェクト
    private val _helper = DatabaseHelper(this@MainActivity)
    private var _noteList: MutableList<MutableMap<String, String>> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        _noteList = getDbData()
        // SimpleAdapter を生成
        val adapter = SimpleAdapter(this@MainActivity, _noteList,
            android.R.layout.simple_list_item_2, _from, _to)

        // 画面部品ListViewを取得
        val lvMenu = findViewById<ListView>(R.id.lvMenu)
        // アダプタの登録
        lvMenu.adapter = adapter
        // リスナの登録
        lvMenu.onItemClickListener = ListItemClickListener()
        // コンテキストメニューの有効化
        registerForContextMenu(lvMenu)

        // 通知チャネル名をstring.xmlから取得
        val name = getString(R.string.notification_channel_name)
        // 通知チャネルの重要度を標準に設定
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        // 通知チャネルを生成
        val channel = NotificationChannel(CHANNEL_ID, name, importance)
        // NotificationManagerオブジェクトを取得
        val manager = getSystemService(NotificationManager::class.java)
        // 通知チャネルを設定
        manager.createNotificationChannel(channel)
    }

    override fun onRestart() {
        // DBデータの再取得・画面への反映
        _noteList = getDbData()
        // SimpleAdapter を生成
        val adapter = SimpleAdapter(this@MainActivity, _noteList,
            android.R.layout.simple_list_item_2, _from, _to)
        // 画面部品ListViewを取得
        val lvMenu = findViewById<ListView>(R.id.lvMenu)
        // アダプタの登録
        lvMenu.adapter = adapter

        super.onRestart()
    }

    override fun onDestroy() {
        _helper.close()
        super.onDestroy()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // オプションメニュー用xmlファイルをインフレイト
        menuInflater.inflate(R.menu.menu_options_menu_list, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // 戻り値用の変数を初期値trueで用意
        var returnVal = true
        // 選択されたメニューが「新しいメモを登録」の場合
        when (item.itemId) {
            R.id.menuListOptionCreate -> {
                val intent = Intent(this@MainActivity, NoteEditActivity::class.java)
                intent.putExtra("noteTitle", "")
                intent.putExtra("noteContent", "")
                startActivity(intent)
            }
            R.id.menuListOptionNotification -> {
                val builder = NotificationCompat.Builder(this@MainActivity, CHANNEL_ID)
                // 通知エリアに表示されるアイコンを設定
                builder.setSmallIcon(android.R.drawable.ic_dialog_info)
                // 通知ドロワーでの表示タイトルを設定
                builder.setContentTitle(getString(R.string.notification_title_finish))
                // 通知ドロワーでの表示メッセージを設定
                builder.setContentText(getString(R.string.notification_text_finish))
                // BuilderからNotificationオブジェクトを生成
                val notification = builder.build()
                // NotificationManagerCompatオブジェクトを取得
                val manager = NotificationManagerCompat.from(this@MainActivity)
                // 通知
                manager.notify(100, notification)

                Toast.makeText(this@MainActivity, "通知を送信しました。", Toast.LENGTH_LONG).show()
            }
            else ->
                returnVal = super.onOptionsItemSelected(item)
        }
        return returnVal
    }

    override fun onCreateContextMenu(menu: ContextMenu, view: View, menuInfo: ContextMenu.ContextMenuInfo) {
        super.onCreateContextMenu(menu, view, menuInfo)
        // コンテキストメニュー用xmlファイルをインフレイト
        menuInflater.inflate(R.menu.menu_context_menu_list, menu)
        // コンテキストメニューのヘッダタイトルを設定
        menu.setHeaderTitle(R.string.menu_list_context_header)
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        var returnVal = true
        // 長押しされたビューに関する情報が格納されたオブジェクトを取得
        val info = item.menuInfo as AdapterView.AdapterContextMenuInfo
        // 長押しされたリストのポジションを取得
        val listPosition = info.position
        // ポジションから長押しされたメニュー情報Mapオブジェクトを取得
        val menu = _noteList[listPosition]

        when(item.itemId){
            R.id.menuListContextDel -> {
                val id = menu["id"] as String
                // データベースヘルパーオブジェクトからデータベース接続オブジェクトを取得
                val db = _helper.writableDatabase
                // 削除用SQL文字列を用意
                val sqlDelete = "DELETE FROM verysimplenote WHERE _id = ?"
                // SQL文字列を元にプリペアドステートメントを取得
                var stmt = db.compileStatement(sqlDelete)
                // 変数のバインド
                stmt.bindLong(1, id.toLong())
                // 削除SQLの実行
                stmt.executeUpdateDelete()

                // DBデータの再取得・画面への反映
                _noteList = getDbData()
                // SimpleAdapter を生成
                val adapter = SimpleAdapter(this@MainActivity, _noteList,
                    android.R.layout.simple_list_item_2, _from, _to)
                // 画面部品ListViewを取得
                val lvMenu = findViewById<ListView>(R.id.lvMenu)
                // アダプタの登録
                lvMenu.adapter = adapter
            }
            else ->
                returnVal = super.onContextItemSelected(item)
        }
        return returnVal
    }

    private inner class ListItemClickListener: AdapterView.OnItemClickListener {
        override fun onItemClick(parent: AdapterView<*>, view: View, position: Int, id: Long) {
            // タップされた行のデータを取得 SimpleAdapterでは1行分のデータは MutableMap型！
            val item = parent.getItemAtPosition(position) as MutableMap<String,String>

            // タイトルと内容を取得
            val noteId = item["id"]?.toInt()
            val noteTitle = item["title"]
            val noteContent = item["content"]

            // インテントオブジェクトを生成
            val intentEdit = Intent(this@MainActivity, NoteEditActivity::class.java)

            // 第2画面に送るデータを格納
            intentEdit.putExtra("noteId", noteId)
            intentEdit.putExtra("noteTitle", noteTitle)
            intentEdit.putExtra("noteContent", noteContent)

            // 第2画面の起動
            startActivity(intentEdit)
        }
    }

    private fun getDbData(): MutableList<MutableMap<String, String>>{
        val noteList: MutableList<MutableMap<String, String>> = mutableListOf()

        // データベースヘルパーオブジェクトからデータベース接続オブジェクトを取得
        val db = _helper.writableDatabase

        // 主キーによる検索SQL文字列を用意
        val sql = "SELECT * FROM verysimplenote"
        // SQLの実行
        val cursor = db.rawQuery(sql, null)

        while (cursor.moveToNext()){
            // カラムのインデックス値を取得
            val idxId = cursor.getColumnIndex("_id")
            val idxTitle = cursor.getColumnIndex("title")
            val idxContent = cursor.getColumnIndex("content")
//            val content = cursor.getString(idxContent)

            // カラムのインデックス値を元に実際のデータを取得
            noteList.add(mutableMapOf(
                "id" to cursor.getString(idxId),
                "title" to cursor.getString(idxTitle),
                "content" to cursor.getString(idxContent)
//                "content" to if(content.length <= 5) content else "${content.substring(0,5)}..."
            ))
        }

        return noteList
    }

}