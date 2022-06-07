package com.websarva.wings.android.verysimplenote

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

class SoundManageService : Service() {
    companion object {
        // 通知チャンネルID文字列定数
        private const val CHANNEL_ID = "soundmanagerservice_notification_channel"
    }

    // メディアプレイヤープロパティ
    private var _player: MediaPlayer? = null

    override fun onBind(intent: Intent): IBinder {
        TODO("Return the communication channel to the service.")
    }

    override fun onCreate() {
        // プロパティのメディアプレイヤーオブジェクトを作成
        _player = MediaPlayer()

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

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // 音声ファイルのURI文字列を生成
        val mediaFileUriStr = "android.resource://${packageName}/${R.raw.karas}"
        // 音声ファイルのURI文字列を元にURIオブジェクトを生成
        val mediaFileUri = Uri.parse(mediaFileUriStr)
        // プロパティのプレーヤーがnullでない場合
        _player?.let {
            // メディアプレイヤーに音声ファイルを指定
            it.setDataSource(this@SoundManageService, mediaFileUri)
            // 非同期でのメディア再生準備が完了した際のリスナを設定
            it.setOnPreparedListener(PlayerPreparedListener())
            // メディア再生が終了した際のリスナを設定
            it.setOnCompletionListener(PlayerCompletionListener())
            // 非同期でメディア再生を準備
            it.prepareAsync()
        }
        // 定数を返す
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        // プロパティのプレーヤーがnullでない場合
        _player?.let {
            // プレーヤーが再生中の場合
            if(it.isPlaying) {
                // プレイヤーを停止
                it.stop()
            }
            // プレーヤーを開放
            it.release()
        }
        // プレーヤー用プロパティをnullに
        _player = null
    }

    // メディア再生準備が完了した際のリスナクラス
    private inner class PlayerPreparedListener: MediaPlayer.OnPreparedListener {
        override fun onPrepared(mp: MediaPlayer) {
            // メディアを再生
            mp.start()
            // Notificationを作成するBuilderクラスを生成
            val builder = NotificationCompat.Builder(this@SoundManageService, CHANNEL_ID)
            // 通知エリアに表示されるアイコンを設定
            builder.setSmallIcon(android.R.drawable.ic_dialog_info)
            // 通知ドロワーでの表示タイトルを設定
            builder.setContentTitle(getString(R.string.msg_notification_title_start))
            // 通知ドロワーでの表示メッセージを設定
            builder.setContentText(getString(R.string.msg_notification_text_start))
            // 起動先Activityクラスを指定したIntentオブジェクトを生成
            val intent = Intent(this@SoundManageService, MediaActivity::class.java)
            // 起動先Activityに引き継ぎデータを格納
            intent.putExtra("fromNotification", true)
            // PendingIntentオブジェクトを取得
            val stopServiceIntent = PendingIntent.getActivity(this@SoundManageService
                , 0, intent, PendingIntent.FLAG_CANCEL_CURRENT)
            // PendingIntentオブジェクトをビルダーに設定
            builder.setContentIntent(stopServiceIntent)
            // タップされた通知メッセージを自動的に消去するように設定
            builder.setAutoCancel(true)
            // BuilderからNotificationオブジェクトを生成
            val notification = builder.build()
            // Notificationオブジェクトを元にサービスをフォアグラウンド化
            startForeground(200, notification)
        }
    }

    // メディア再生が終了した際のリスナクラス
    private inner class PlayerCompletionListener: MediaPlayer.OnCompletionListener {
        override fun onCompletion(mp: MediaPlayer) {
            // Notificationを作成するBuilderクラスを生成
            val builder = NotificationCompat.Builder(this@SoundManageService, CHANNEL_ID)
            // 通知エリアに表示されるアイコンを設定
            builder.setSmallIcon(android.R.drawable.ic_dialog_info)
            // 通知ドロワーでの表示タイトルを設定
            builder.setContentTitle(getString(R.string.msg_notification_title_finish))
            // 通知ドロワーでの表示メッセージを設定
            builder.setContentText(getString(R.string.msg_notification_text_finish))
            // BuilderからNotificationオブジェクトを生成
            val notification = builder.build()
            // NotificationManagerCompatオブジェクトを取得
            val manager = NotificationManagerCompat.from(this@SoundManageService)
            // 通知
            manager.notify(100, notification)

            // 自分自身を終了
            stopSelf()
        }
    }
}