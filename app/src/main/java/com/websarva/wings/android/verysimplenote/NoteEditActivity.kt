package com.websarva.wings.android.verysimplenote

import android.database.sqlite.SQLiteStatement
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class NoteEditActivity : AppCompatActivity() {
    // データベースヘルパーオブジェクト
    private val _helper = DatabaseHelper(this@NoteEditActivity)
    private var _noteId = -1
    // 編集フォーム findViewIdを同じ対象に2度使用するとnullが返る
    private var etTitle: EditText? = null
    private var etContent: EditText? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_note_edit)

        // リスト画面から渡されたデータを取得
        _noteId = intent.getIntExtra("noteId", -1)
        val noteTitle = intent.getStringExtra("noteTitle")
        val noteContent = intent.getStringExtra("noteContent")

        // タイトルと内容を表示させるEditTextを取得
        etTitle = findViewById<EditText>(R.id.etTitle)
        etContent = findViewById<EditText>(R.id.etContent)

        etTitle?.setText(noteTitle)
        etContent?.setText(noteContent)

        // 戻るボタンの有効化
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    // 戻るボタンの処理実装
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

    // 保存ボタンがタップされた時の処理メソッド
    fun onSaveButtonClick(view: View) {

        var title = etTitle?.text.toString()
        if (title == "") {
            title = "タイトルなし"
        }
        var content = etContent?.text.toString()

        // データベースヘルパーオブジェクトからデータベース接続オブジェクトを取得
        val db = _helper.writableDatabase
        var stmt: SQLiteStatement

        // 更新時のみ既存のデータを削除
        if (_noteId != -1) {
            // 削除用SQL文字列を用意
            val sqlDelete = "DELETE FROM verysimplenote WHERE _id = ?"
            // SQL文字列を元にプリペアドステートメントを取得
            stmt = db.compileStatement(sqlDelete)
            // 変数のバインド
            stmt.bindLong(1, _noteId.toLong())
            // 削除SQLの実行
            stmt.executeUpdateDelete()
        }
        // インサート用SQL文字列を用意
        val sqlInsert = "INSERT INTO verysimplenote (title, content) VALUES (?, ?)"
        // SQL文字列を元にプリペアドステートメントを取得
        stmt = db.compileStatement(sqlInsert)
        // 変数のバインド
        stmt.bindString(1, title)
        stmt.bindString(2, content)
        // インサートSQLの実行
        stmt.executeInsert()

        finish()
    }
}