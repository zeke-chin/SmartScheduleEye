package com.example.demo

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.widget.Toast

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // 播放默认闹钟声音
        val ringtone = RingtoneManager.getRingtone(
            context,
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        )
        ringtone.play()
        
        // 显示提示
        Toast.makeText(context, "闹钟时间到！", Toast.LENGTH_LONG).show()
    }
} 