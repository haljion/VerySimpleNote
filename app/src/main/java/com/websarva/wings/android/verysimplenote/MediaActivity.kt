package com.websarva.wings.android.verysimplenote

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Button

class MediaActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_media)

        // Intentから、通知のタップからの引き継ぎデータを取得
        val fromNotification = intent.getBooleanExtra("fromNotification", false)
        // 引き継ぎデータが存在する場合
        if (fromNotification) {
            // 再生ボタンをタップ不可に、停止ボタンをタップ可に変更
            val btPlay = findViewById<Button>(R.id.btPlay)
            val btStop = findViewById<Button>(R.id.btStop)
            btPlay.isEnabled = false
            btStop.isEnabled = true
        }

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    fun onPlayButtonClick(view: View) {
        // インテントオブジェクトを生成
        val intent = Intent(this@MediaActivity, SoundManageService::class.java)
        // サービスを起動
        startService(intent)
        // 再生ボタンをタップ不可に、停止ボタンをタップ可に変更
        val btPlay = findViewById<Button>(R.id.btPlay)
        val btStop = findViewById<Button>(R.id.btStop)
        btPlay.isEnabled = false
        btStop.isEnabled = true
    }

    fun onStopButtonClick(view: View) {
        // インテントオブジェクトを生成
        val intent = Intent(this@MediaActivity, SoundManageService::class.java)
        // サービスを起動
        stopService(intent)
        // 再生ボタンをタップ不可に、停止ボタンをタップ可に変更
        val btPlay = findViewById<Button>(R.id.btPlay)
        val btStop = findViewById<Button>(R.id.btStop)
        btPlay.isEnabled = true
        btStop.isEnabled = false
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
}