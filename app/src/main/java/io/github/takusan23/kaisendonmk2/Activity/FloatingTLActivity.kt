package io.github.takusan23.kaisendonmk2.Activity

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.widget.Toast
import io.github.takusan23.kaisendonmk2.MainActivity
import io.github.takusan23.kaisendonmk2.R

class FloatingTLActivity : MainActivity() {

    companion object {
        fun showBubbles(context: Context?) {
            // Android Q以降で利用可能
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val intent = Intent(context, FloatingTLActivity::class.java)
                val icon = Icon.createWithResource(context, R.drawable.ic_library_books_black_24dp)
                val bubbleIntent = PendingIntent.getActivity(context, 25, intent, PendingIntent.FLAG_UPDATE_CURRENT)
                // 通知作成？
                val bubbleData = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    Notification.BubbleMetadata.Builder(bubbleIntent, icon)
                        .setDesiredHeight(1200)
                        .setIntent(bubbleIntent)
                        .build()
                } else {
                    Notification.BubbleMetadata.Builder()
                        .setDesiredHeight(1200)
                        .setIcon(icon)
                        .setIntent(bubbleIntent)
                        .build()
                }
                val supplierPerson = Person.Builder().setName(context?.getString(R.string.floating_tl)).setIcon(icon).build()
                // 通知送信
                val notificationManager = context?.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                // 通知チャンネル作成
                val notificationId = "floating_comment_viewer"
                if (notificationManager.getNotificationChannel(notificationId) == null) {
                    // 作成
                    val notificationChannel = NotificationChannel(notificationId, context?.getString(R.string.floating_tl), NotificationManager.IMPORTANCE_DEFAULT)
                    notificationManager.createNotificationChannel(notificationChannel)
                }
                // 通知作成
                val notification = Notification.Builder(context, notificationId)
                    .setContentText(context.getString(R.string.floating_tl))
                    .setContentTitle(context.getString(R.string.floating_tl))
                    .setSmallIcon(R.drawable.ic_library_books_black_24dp)
                    .setBubbleMetadata(bubbleData)
                    .addPerson(supplierPerson)
                    .setStyle(Notification.MessagingStyle(supplierPerson).apply {
                        conversationTitle = context.getString(R.string.floating_tl)
                    })
                    .build()
                // 送信
                notificationManager.notify(5, notification)
            } else {
                // Android Pieなので..
                Toast.makeText(context, "Android 10以降で利用可能です", Toast.LENGTH_SHORT).show()
            }
        }
    }

}