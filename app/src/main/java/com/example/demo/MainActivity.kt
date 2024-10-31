package com.example.demo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.demo.ui.theme.DemoTheme
import com.example.demo.image.ImagePickerScreen
import androidx.compose.ui.tooling.preview.Preview
import android.Manifest
import android.os.Build
import androidx.core.app.ActivityCompat
import android.provider.AlarmClock
import android.widget.Toast
import android.content.Intent
import java.util.*


// MainActivity 类：应用程序的主入口点
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 请求存储权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_LOGS
                ),
                1001
            )
        }
        // 启用边缘到边缘显示，提供全屏体验
        enableEdgeToEdge()
        // 设置应用程序的内容
        setContent {
            // 应用主题
            DemoTheme {
                // Scaffold 提供基本的 Material Design 布局结构
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    // 调用主屏幕组件，传入内边距
                    MainScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }

    fun setAlarms(dates: List<Triple<String, String, String>>) {  // Triple<日期, 时间, 标签>
        try {
            val currentTime = System.currentTimeMillis()
            var hasValidDate = false
            
            // 检查是否所有日期都比当前时间早
            dates.forEach { (date, time, _) ->
                val (dateParts, timeParts) = parseDateTime(date, time) ?: return@forEach
                val targetTime = Calendar.getInstance().apply {
                    set(dateParts[0], dateParts[1] - 1, dateParts[2], timeParts[0], timeParts[1], 0)
                }
                if (targetTime.timeInMillis > currentTime) {
                    hasValidDate = true
                }
            }
            
            if (!hasValidDate) {
                Toast.makeText(this, "逗我呢？你日子倒着过呢嘛～📅", Toast.LENGTH_LONG).show()
                return
            }
            
            // 设置未来日期的闹钟
            dates.forEach { (date, time, label) ->
                val (dateParts, timeParts) = parseDateTime(date, time) ?: return@forEach
                val targetTime = Calendar.getInstance().apply {
                    set(dateParts[0], dateParts[1] - 1, dateParts[2], timeParts[0], timeParts[1], 0)
                }
                
                if (targetTime.timeInMillis > currentTime) {
                    // 获取星期几
                    val weekDays = arrayOf("周日", "周一", "周二", "周三", "周四", "周五", "周六")
                    val weekDay = weekDays[targetTime.get(Calendar.DAY_OF_WEEK) - 1]
                    
                    // 格式化日期为 "MM-dd"
                    val month = targetTime.get(Calendar.MONTH) + 1
                    val day = targetTime.get(Calendar.DAY_OF_MONTH)
                    val dateStr = String.format("%02d-%02d", month, day)
                    
                    val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                        putExtra(AlarmClock.EXTRA_HOUR, timeParts[0])
                        putExtra(AlarmClock.EXTRA_MINUTES, timeParts[1])
                        putExtra(AlarmClock.EXTRA_MESSAGE, "SSE: $dateStr $weekDay $label")
                        putExtra(AlarmClock.EXTRA_SKIP_UI, true)
                        // 添加日期设置
                        putExtra(AlarmClock.EXTRA_DAYS, arrayListOf(targetTime.get(Calendar.DAY_OF_WEEK)))
                        // 设置为一次性闹钟
                        putExtra(AlarmClock.EXTRA_VIBRATE, true)
                        putExtra(AlarmClock.EXTRA_RINGTONE, "content://settings/system/alarm_alert")
                    }
                    startActivity(intent)
                }
            }
        } catch (e: Exception) {
            Toast.makeText(this, "设置闹钟失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun parseDateTime(date: String, time: String): Pair<List<Int>, List<Int>>? {
        try {
            val dateParts = date.split(".").map { it.toInt() }
            val timeParts = time.split(":").map { it.toInt() }
            
            if (dateParts.size != 3 || timeParts.size != 2) {
                Toast.makeText(this, "日期或时间格式错误", Toast.LENGTH_SHORT).show()
                return null
            }
            
            return Pair(dateParts, timeParts)
        } catch (e: Exception) {
            Toast.makeText(this, "解析日期时间失败: ${e.message}", Toast.LENGTH_SHORT).show()
            return null
        }
    }

    fun clearSSEAlarms() {
        try {
            val intent = Intent(AlarmClock.ACTION_SHOW_ALARMS)
            startActivity(intent)
            
            Toast.makeText(
                this,
                "请手动删除 SSE 开头的闹钟",
                Toast.LENGTH_LONG
            ).show()
        } catch (e: Exception) {
            Toast.makeText(this, "打开闹钟设置失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}

// MainScreen 组合函数：应用的主要界面
@Composable
fun MainScreen(modifier: Modifier = Modifier) {
    // 使用 remember 和 mutableStateOf 管理分析结果状态
    var analysisResult by remember { mutableStateOf<String?>(null) }
    
    // 主要列布局，包含所有UI元素
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // 图片选择器界面
        ImagePickerScreen(
            onAnalysisResult = { result ->
                analysisResult = result
            }
        )
    }
}

@Preview(
    name = "主屏幕预览",
    showBackground = true,
    showSystemUi = true
)
@Composable
fun MainScreenPreview() {
    DemoTheme {
        MainScreen()
    }
}