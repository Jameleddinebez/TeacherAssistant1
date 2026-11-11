package com.teacher.assistant

import android.media.*
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class AlarmActivity : ComponentActivity() {
    private var mp: MediaPlayer? = null
    override fun onCreate(s: Bundle?) {
        super.onCreate(s)
        val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
        mp = MediaPlayer().apply {
            setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
            setDataSource(this@AlarmActivity, uri); isLooping = true; prepare(); start()
        }
        setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                MaterialTheme {
                    Column(Modifier.fillMaxSize().padding(24.dp), Arrangement.Center, Alignment.CenterHorizontally) {
                        Text("حان وقت الاستيقاظ", fontSize = 32.sp)
                        Spacer(Modifier.height(32.dp))
                        Button(onClick = { stop() }, Modifier.fillMaxWidth().height(64.dp)) { Text("إيقاف المنبه", fontSize = 22.sp) }
                    }
                }
            }
        }
    }
    private fun stop() { mp?.stop(); mp?.release(); mp = null; finish() }
    override fun onDestroy() { mp?.release(); super.onDestroy() }
}
