package com.example.demo.image

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.draw.clip
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import org.json.JSONObject
import android.util.Log
import androidx.compose.material3.AlertDialog
import android.widget.Toast
import java.util.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.TextField
import androidx.compose.foundation.clickable
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Check
import androidx.compose.ui.draw.shadow
import androidx.compose.material.icons.filled.Check
import android.content.Context
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.OutlinedTextField
import com.example.demo.utils.LogManager
import com.example.demo.MainActivity

enum class ScheduleType {
    NORMAL_WORK,    // CT/DR/体检/DR+检
    SHORT_WORK,     // 带*号的班次
    REST,           // 公休/休
    NIGHT_SHIFT,    // (值)
    MORNING_END,    // 出
    UNKNOWN;        // 未知类型

    fun getAlarmTime(): List<String> {
        return when (this) {
            NORMAL_WORK -> listOf("06:40", "12:59")
            SHORT_WORK -> listOf("06:40", "12:59")
            NIGHT_SHIFT -> listOf("14:30")
            MORNING_END -> listOf("07:10")
            REST -> listOf("休息")
            UNKNOWN -> listOf("未知")
        }
    }

    companion object {
        fun fromScheduleText(text: String): ScheduleType {
            if (text.isEmpty()) return UNKNOWN
            
            val upperText = text.uppercase()
            return when {
                upperText.contains("*") || upperText.contains("日") -> SHORT_WORK
                upperText.contains("CT") || upperText.contains("DR") || 
                upperText.contains("体检") || upperText.contains("检") -> NORMAL_WORK
                upperText.contains("公") || upperText.contains("休") -> REST
                upperText.contains("值") -> NIGHT_SHIFT
                upperText.contains("出") -> MORNING_END
                else -> UNKNOWN
            }
        }
    }
}

@Composable
fun ImagePickerScreen(
    onAnalysisResult: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showSettingsDialog by remember { mutableStateOf(false) }
    
    // 从 SharedPreferences 读取配置
    val prefs = remember { context.getSharedPreferences("app_config", Context.MODE_PRIVATE) }
    var apiHost by remember { 
        mutableStateOf(prefs.getString("api_host", "https://open.bigmodel.cn/api/paas/v4/") ?: "https://open.bigmodel.cn/api/paas/v4/")
    }
    var apiKey by remember { 
        mutableStateOf(prefs.getString("api_key", "d8cf7e81dc97fd21e176b783b4704101.fcmydfO7fCiSKzyN") ?: "d8cf7e81dc97fd21e176b783b4704101.fcmydfO7fCiSKzyN")
    }
    
    // 添加 modelId 的状态
    var modelId by remember { 
        mutableStateOf(prefs.getString("model_id", "glm-4v") ?: "glm-4v")
    }
    
    // 修改 ImageRepository 实例化
    var imageRepository by remember { 
        mutableStateOf(ImageRepository(context, apiHost, apiKey, modelId))
    }
    
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var isAnalyzing by remember { mutableStateOf(false) }
    var scheduleTableState by remember { mutableStateOf("") }
    
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    
    var targetSplitRatio by remember { mutableStateOf(0.5f) }
    val splitRatio by animateFloatAsState(
        targetValue = targetSplitRatio,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "splitRatio"
    )

    var imageContainerSize by remember { mutableStateOf(IntSize.Zero) }

    var showDialog by remember { mutableStateOf(false) }
    var dialogMessage by remember { mutableStateOf("") }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        try {
            selectedImageUri = uri
            scale = 1f
            offset = Offset.Zero
        } catch (e: Exception) {
            Log.e("ImagePicker", "Error selecting image: ${e.message}")
            Toast.makeText(
                context,
                "选择图片失败，请重试",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned { coordinates ->
                imageContainerSize = coordinates.size
            },
        contentAlignment = if (selectedImageUri == null) Alignment.Center else Alignment.TopCenter
    ) {
        if (selectedImageUri == null) {
            Button(
                onClick = { 
                    try {
                        launcher.launch("image/*")
                    } catch (e: Exception) {
                        Log.e("ImagePicker", "Error launching picker: ${e.message}")
                        Toast.makeText(
                            context,
                            "启动图片选择器失败，请重试",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                },
                modifier = Modifier
                    .height(56.dp)
                    .width(200.dp)
            ) {
                Text("选择图片")
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 图片显示区域
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(splitRatio)
                        .padding(16.dp)
                        .background(
                            color = MaterialTheme.colorScheme.surface,
                            shape = MaterialTheme.shapes.medium
                        )
                        .clip(MaterialTheme.shapes.medium)
                        .padding(1.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .onGloballyPositioned { coordinates ->
                                imageContainerSize = coordinates.size
                            }
                    ) {
                        Image(
                            painter = rememberAsyncImagePainter(selectedImageUri),
                            contentDescription = "Selected image",
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer(
                                    scaleX = scale,
                                    scaleY = scale,
                                    translationX = offset.x.coerceIn(
                                        -imageContainerSize.width * (scale - 1) / 2,
                                        imageContainerSize.width * (scale - 1) / 2
                                    ),
                                    translationY = offset.y.coerceIn(
                                        -imageContainerSize.height * (scale - 1) / 2,
                                        imageContainerSize.height * (scale - 1) / 2
                                    )
                                )
                                .pointerInput(Unit) {
                                    detectTransformGestures { _, pan, zoom, _ ->
                                        val newScale = (scale * zoom).coerceIn(1f, 3f)
                                        
                                        val newOffset = Offset(
                                            x = (offset.x + pan.x),
                                            y = (offset.y + pan.y)
                                        )
                                        
                                        scale = newScale
                                        
                                        offset = Offset(
                                            x = newOffset.x.coerceIn(
                                                -imageContainerSize.width * (newScale - 1) / 2,
                                                imageContainerSize.width * (newScale - 1) / 2
                                            ),
                                            y = newOffset.y.coerceIn(
                                                -imageContainerSize.height * (newScale - 1) / 2,
                                                imageContainerSize.height * (newScale - 1) / 2
                                            )
                                        )
                                    }
                                },
                            contentScale = ContentScale.Fit
                        )
                    }
                }
                
                // 分隔栏
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(32.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .pointerInput(Unit) {
                            detectVerticalDragGestures { change: PointerInputChange, dragAmount: Float ->
                                change.consume()
                                val delta = dragAmount / size.height
                                targetSplitRatio = (targetSplitRatio + delta * 0.05f)
                                    .coerceIn(0.2f, 0.5f)
                            }
                        }
                ) {
                    Box(
                        modifier = Modifier
                            .width(50.dp)
                            .height(4.dp)
                            .background(
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                shape = MaterialTheme.shapes.small
                            )
                            .align(Alignment.Center)
                    )
                }
                
                // 在这里显示表格（分隔方）
                if (scheduleTableState.isNotEmpty()) {
                    ScheduleTable(
                        markdownTable = scheduleTableState,
                        onTableUpdated = { newTable ->
                            scheduleTableState = newTable
                        }
                    )
                }
                
                // 钮区域
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Button(
                            onClick = { launcher.launch("image/*") },
                            modifier = Modifier
                                .height(56.dp)
                                .width(200.dp)
                        ) {
                            Text("选择图片")
                        }

                        Button(
                            onClick = {
                                scope.launch {
                                    isAnalyzing = true
                                    try {
                                        selectedImageUri?.let { uri ->
                                            val inputStream = context.contentResolver.openInputStream(uri)
                                            inputStream?.use { stream ->
                                                val bitmap = BitmapFactory.decodeStream(stream)
                                                val outputStream = ByteArrayOutputStream()
                                                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                                                val imageBytes = outputStream.toByteArray()
                                                val base64String = Base64.encodeToString(imageBytes, Base64.DEFAULT)

                                                val result = imageRepository.analyzeImage(base64String)
                                                
                                                result.fold(
                                                    onSuccess = { content ->
                                                        Toast.makeText(context, "图片解析成功！逻辑处理中", Toast.LENGTH_SHORT).show()
                                                        try {
                                                            val jsonContent = content.substringAfter("```json").substringBefore("```")
                                                            val jsonObject = JSONObject(jsonContent)
                                                            
                                                            val qinShuYangObj = jsonObject.optJSONObject("钱淑阳")
                                                            if (qinShuYangObj != null) {
                                                                Log.d("排班信息", "钱淑阳的排班信息：")
                                                                val currentYear = Calendar.getInstance().get(Calendar.YEAR)
                                                                val scheduleData = mutableListOf<Triple<String, String, ScheduleType>>()
                                                                
                                                                val keys = qinShuYangObj.keys()
                                                                while (keys.hasNext()) {
                                                                    val key = keys.next()
                                                                    val value = qinShuYangObj.getString(key)
                                                                    val type = ScheduleType.fromScheduleText(value)
                                                                    
                                                                    // 解析日期 (格式: "9-23" -> "2024.9.23")
                                                                    val (month, day) = key.split("-").map { it.toInt() }
                                                                    val fullDate = "$currentYear.$month.$day"
                                                                    
                                                                    scheduleData.add(Triple(fullDate, value, type))
                                                                }
                                                                
                                                                // 生成 Markdown 表格
                                                                val tableHeader = scheduleData.joinToString(" | ", "| ", " |") { it.first }
                                                                val tableDivider = scheduleData.joinToString(" | ", "| ", " |") { ":---:" }
                                                                val tableValues = scheduleData.joinToString(" | ", "| ", " |") { it.second }
                                                                val tableEmojis = scheduleData.joinToString(" | ", "| ", " |") { 
                                                                    when (it.third) {
                                                                        ScheduleType.NIGHT_SHIFT -> "值夜班了🌙"
                                                                        ScheduleType.MORNING_END -> "早班结束🌞"
                                                                        ScheduleType.REST -> "睡大觉喽😴"
                                                                        ScheduleType.NORMAL_WORK -> "正常上班😊"
                                                                        ScheduleType.SHORT_WORK -> "没有午休😭"
                                                                        ScheduleType.UNKNOWN -> "未知班次❓"
                                                                    }
                                                                }
                                                                
                                                                scheduleTableState = """
                                                                    $tableHeader
                                                                    $tableDivider
                                                                    $tableValues
                                                                    $tableEmojis
                                                                """.trimIndent()
                                                                
                                                                onAnalysisResult("解析成功！请查看排班表")
                                                            } else {
                                                                dialogMessage = "未找到排班信"
                                                                showDialog = true
                                                            }
                                                        } catch (e: Exception) {
                                                            Log.e("JSON解析错误", "解析失败: ${e.message}")
                                                            dialogMessage = "图片解析结果失败～请重试\n多次重试失败联系🍓草莓园"
                                                            showDialog = true
                                                        }
                                                    },
                                                    onFailure = { exception ->
                                                        Toast.makeText(
                                                            context,
                                                            "图片解析失败～请重试\n多次重试失败联系🍓草莓园",
                                                            Toast.LENGTH_LONG
                                                        ).show()
                                                    }
                                                )
                                            }
                                        }
                                    } catch (e: Exception) {
                                        dialogMessage = "图片解析失败～请重试\n多次重试败联系🍓草莓园"
                                        showDialog = true
                                    } finally {
                                        isAnalyzing = false
                                    }
                                }
                            },
                            enabled = !isAnalyzing,
                            modifier = Modifier
                                .height(56.dp)
                                .width(200.dp)
                        ) {
                            Text(if (isAnalyzing) "分析中..." else "分析图片")
                        }

                        // 添加设置闹钟按钮
                        if (scheduleTableState.isNotEmpty()) {
                            Button(
                                onClick = {
                                    val lines = scheduleTableState.lines()
                                    if (lines.size >= 4) {
                                        val dates = lines[0].trim('|').split('|').map { it.trim() }
                                        val values = lines[2].trim('|').split('|').map { it.trim() }
                                        val emojis = lines[3].trim('|').split('|').map { it.trim() }
                                        
                                        val alarmList = mutableListOf<Triple<String, String, String>>()
                                        
                                        dates.forEachIndexed { index, date ->
                                            val value = values.getOrNull(index) ?: ""
                                            val emoji = emojis.getOrNull(index) ?: ""
                                            val scheduleType = ScheduleType.fromScheduleText(value)
                                            val alarmTimes = scheduleType.getAlarmTime()
                                            
                                            // 如果有闹钟时间，则添加到列表
                                            if (alarmTimes.isNotEmpty() && alarmTimes[0] != "休息" && alarmTimes[0] != "未知") {
                                                alarmTimes.forEach { time ->
                                                    alarmList.add(Triple(date, time, emoji))
                                                }
                                            }
                                        }
                                        
                                        // 调用新的设置闹钟方法
                                        (context as? MainActivity)?.setAlarms(alarmList)
                                    }
                                },
                                modifier = Modifier
                                    .height(56.dp)
                                    .width(200.dp)
                            ) {
                                Text("确认闹钟")
                            }
                        }
                    }
                }
            }
        }

        if (showDialog) {
            AlertDialog(
                onDismissRequest = { showDialog = false },
                title = { Text("提示") },
                text = { Text(dialogMessage) },
                confirmButton = {
                    Button(onClick = { showDialog = false }) {
                        Text("确定")
                    }
                }
            )
        }

        // 添加设置按钮
        FloatingActionButton(
            onClick = { showSettingsDialog = true },
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Settings,
                contentDescription = "设置",
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
        
        // 设置对话框
        if (showSettingsDialog) {
            AlertDialog(
                onDismissRequest = { showSettingsDialog = false },
                title = { 
                    Text(
                        "API 配置",
                        style = MaterialTheme.typography.headlineSmall
                    )
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        OutlinedTextField(
                            value = apiHost,
                            onValueChange = { apiHost = it },
                            label = { Text("API Host") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        OutlinedTextField(
                            value = apiKey,
                            onValueChange = { apiKey = it },
                            label = { Text("API Key") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        OutlinedTextField(
                            value = modelId,
                            onValueChange = { modelId = it },
                            label = { Text("Model ID") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // 恢复默认按钮
                            TextButton(
                                onClick = {
                                    apiHost = "https://open.bigmodel.cn/api/paas/v4/"
                                    apiKey = "d8cf7e81dc97fd21e176b783b4704101.fcmydfO7fCiSKzyN"
                                    modelId = "glm-4v"
                                }
                            ) {
                                Text(
                                    "恢复默认设置",
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            
                            // 日志操作按钮
                            Column(
                                horizontalAlignment = Alignment.End
                            ) {
                                TextButton(
                                    onClick = {
                                        scope.launch {
                                            LogManager.exportLogs(context).fold(
                                                onSuccess = { file ->
                                                    Toast.makeText(
                                                        context,
                                                        "日志已保存到: ${file.path}",
                                                        Toast.LENGTH_LONG
                                                    ).show()
                                                },
                                                onFailure = { e ->
                                                    Toast.makeText(
                                                        context,
                                                        "导出日志失败: ${e.message}",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                            )
                                        }
                                    }
                                ) {
                                    Text(
                                        "下载运行日志",
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                                
                                TextButton(
                                    onClick = {
                                        LogManager.openLogFolder(context)
                                    }
                                ) {
                                    Text(
                                        "打开日志文件夹",
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TextButton(onClick = { showSettingsDialog = false }) {
                            Text("取消")
                        }
                        Button(
                            onClick = {
                                scope.launch {
                                    imageRepository.cleanup()
                                    prefs.edit().apply {
                                        putString("api_host", apiHost)
                                        putString("api_key", apiKey)
                                        putString("model_id", modelId)
                                        apply()
                                    }
                                    imageRepository = ImageRepository(context, apiHost, apiKey, modelId)
                                    showSettingsDialog = false
                                }
                            }
                        ) {
                            Text("保存")
                        }
                    }
                }
            )
        }
    }
}

private suspend fun PointerInputScope.detectVerticalDragGestures(
    onVerticalDrag: (change: PointerInputChange, dragAmount: Float) -> Unit
) {
    detectDragGestures { change: PointerInputChange, dragAmount: Offset ->
        change.consume()
        onVerticalDrag(change, dragAmount.y)
    }
}

@Preview(
    name = "空状态预览",
    showBackground = true,
    showSystemUi = true
)
@Composable
fun ImagePickerScreenEmptyPreview() {
    ImagePickerScreen(
        onAnalysisResult = {}
    )
}

@Preview(
    name = "选择图片后预览",
    showBackground = true,
    showSystemUi = true
)
@Composable
fun ImagePickerScreenWithImagePreview() {
    val previewUri = Uri.parse("https://example.com/preview.jpg")
    
    CompositionLocalProvider {
        ImagePickerScreen(
            onAnalysisResult = {}
        )
    }
}

@Preview(
    name = "分析中状态预览",
    showBackground = true,
    showSystemUi = true
)
@Composable
fun ImagePickerScreenAnalyzingPreview() {
    ImagePickerScreen(
        onAnalysisResult = {}
    )
}

// 添加日期处理具函数
private fun adjustDate(date: String, dayOffset: Int): String {
    try {
        val parts = date.split(".")
        if (parts.size >= 3) {
            val calendar = Calendar.getInstance().apply {
                set(Calendar.YEAR, parts[0].toInt())
                set(Calendar.MONTH, parts[1].toInt() - 1)  // Calendar月份从0开始
                set(Calendar.DAY_OF_MONTH, parts[2].toInt())
                add(Calendar.DAY_OF_MONTH, dayOffset)
            }
            return "${calendar.get(Calendar.YEAR)}.${calendar.get(Calendar.MONTH) + 1}.${calendar.get(Calendar.DAY_OF_MONTH)}"
        }
    } catch (e: Exception) {
        Log.e("日期调整", "日期格式错误: $date", e)
    }
    return date
}

@Composable
fun ScheduleTable(
    markdownTable: String,
    onTableUpdated: (String) -> Unit
) {
    if (markdownTable.isNotEmpty()) {
        val lines = markdownTable.lines()
        if (lines.size >= 4) {
            val dates = lines[0].trim('|').split('|').map { it.trim() }.toMutableList()
            val values = lines[2].trim('|').split('|').map { it.trim() }.toMutableList()
            val emojis = lines[3].trim('|').split('|').map { it.trim() }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min)
                    .padding(16.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surface,
                        shape = MaterialTheme.shapes.medium
                    )
                    .clip(MaterialTheme.shapes.medium)
                    .padding(1.dp)
            ) {
                Box(
                    modifier = Modifier
                        .horizontalScroll(rememberScrollState())
                        .padding(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .width(IntrinsicSize.Max)
                            .height(IntrinsicSize.Max),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        dates.forEachIndexed { index, date ->
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .padding(
                                        start = if (index == 0) 0.dp else 4.dp,
                                        end = if (index == dates.lastIndex) 0.dp else 4.dp
                                    )
                            ) {
                                ScheduleCell(
                                    date = date,
                                    value = values.getOrNull(index) ?: "",
                                    emoji = emojis.getOrNull(index) ?: "",
                                    modifier = Modifier
                                        .width(85.dp)
                                        .fillMaxHeight(),
                                    onDateChange = { newDate ->
                                        dates[index] = newDate
                                        
                                        if (index >= 1) {
                                            dates[index - 1] = adjustDate(newDate, -1)
                                        }
                                        if (index >= 2) {
                                            dates[index - 2] = adjustDate(newDate, -2)
                                        }
                                        
                                        if (index < dates.size - 1) {
                                            dates[index + 1] = adjustDate(newDate, 1)
                                        }
                                        if (index < dates.size - 2) {
                                            dates[index + 2] = adjustDate(newDate, 2)
                                        }
                                        
                                        val newTable = buildMarkdownTable(dates, values)
                                        onTableUpdated(newTable)
                                    },
                                    onValueChange = { newValue ->
                                        values[index] = newValue
                                        val newTable = buildMarkdownTable(dates, values)
                                        onTableUpdated(newTable)
                                    }
                                )
                                
                                if (index < dates.lastIndex) {
                                    Box(
                                        modifier = Modifier
                                            .width(0.dp)
                                            .fillMaxHeight()
                                            .background(
                                                MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                            )
                                            .align(Alignment.CenterEnd)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ScheduleCell(
    date: String,
    value: String,
    emoji: String,
    modifier: Modifier = Modifier,
    onDateChange: (String) -> Unit = {},
    onValueChange: (String) -> Unit = {}
) {
    var isEditing by remember { mutableStateOf(false) }
    var isEditingValue by remember { mutableStateOf(false) }
    var editedDate by remember { mutableStateOf(date) }
    var editedValue by remember { mutableStateOf(value) }
    
    // 解析日期字符串为 Calendar
    val calendar = remember(date) {
        Calendar.getInstance().apply {
            date.split(".").let {
                if (it.size == 3) {
                    set(Calendar.YEAR, it[0].toInt())
                    set(Calendar.MONTH, it[1].toInt() - 1)
                    set(Calendar.DAY_OF_MONTH, it[2].toInt())
                }
            }
        }
    }
    
    var selectedYear by remember { mutableStateOf(calendar.get(Calendar.YEAR)) }
    var selectedMonth by remember { mutableStateOf(calendar.get(Calendar.MONTH)) }
    var selectedDay by remember { mutableStateOf(calendar.get(Calendar.DAY_OF_MONTH)) }
    
    LaunchedEffect(date) {
        editedDate = date
        date.split(".").let {
            if (it.size == 3) {
                selectedYear = it[0].toInt()
                selectedMonth = it[1].toInt() - 1
                selectedDay = it[2].toInt()
            }
        }
    }
    
    Column(
        modifier = modifier
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                shape = MaterialTheme.shapes.medium
            )
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 日期显示
        Text(
            text = date.split(".").drop(1).joinToString("."),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .clip(MaterialTheme.shapes.small)
                .clickable { isEditing = true }
                .padding(vertical = 4.dp, horizontal = 8.dp)
        )
        
        // 分隔线
        Box(
            modifier = Modifier
                .padding(vertical = 4.dp)
                .fillMaxWidth()
                .height(1.dp)
                .background(
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                )
        )
        
        // 班次显示
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .clip(MaterialTheme.shapes.small)
                .clickable { isEditingValue = true }
                .padding(vertical = 4.dp, horizontal = 8.dp),
            maxLines = 1
        )
        
        Box(
            modifier = Modifier
                .padding(vertical = 4.dp)
                .fillMaxWidth()
                .height(1.dp)
                .background(
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                )
        )
        
        // Emoji 显示
        val scheduleType = ScheduleType.fromScheduleText(value)
        val (currentEmoji, emojiColor) = when (scheduleType) {
            ScheduleType.NIGHT_SHIFT -> "值夜班了🌙" to MaterialTheme.colorScheme.tertiary
            ScheduleType.MORNING_END -> "早班结束🌞" to MaterialTheme.colorScheme.primary
            ScheduleType.REST -> "睡大觉喽😴" to MaterialTheme.colorScheme.secondary
            ScheduleType.NORMAL_WORK -> "正常上班😊" to MaterialTheme.colorScheme.primary
            ScheduleType.SHORT_WORK -> "没有午休😭" to MaterialTheme.colorScheme.error
            ScheduleType.UNKNOWN -> "未知班次❓" to MaterialTheme.colorScheme.outline
        }
        Text(
            text = currentEmoji,
            style = MaterialTheme.typography.labelMedium,
            color = emojiColor,
            maxLines = 1,
            modifier = Modifier.padding(vertical = 2.dp)
        )
        
        // 闹钟时间显示
        Box(
            modifier = Modifier
                .padding(vertical = 4.dp)
                .fillMaxWidth()
                .height(1.dp)
                .background(
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                )
        )
        
        // 闹钟图标
        Text(
            text = "⏰",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(vertical = 2.dp)
        )
        
        // 闹钟时间列表
        val alarmTimes = ScheduleType.fromScheduleText(value).getAlarmTime()
        alarmTimes.forEach { time ->
            Text(
                text = time,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(vertical = 1.dp)
            )
        }
    }

    if (isEditing) {
        AlertDialog(
            onDismissRequest = { isEditing = false },
            title = { 
                Text(
                    "修改日期",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            },
            text = {
                Column(
                    modifier = Modifier.padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 年份选择
                    DatePickerRow(
                        label = "年份：",
                        value = selectedYear,
                        onValueChange = { selectedYear = it },
                        range = selectedYear - 1..selectedYear + 1
                    )
                    
                    // 月份选择
                    DatePickerRow(
                        label = "月份：",
                        value = selectedMonth + 1,
                        onValueChange = { selectedMonth = it - 1 },
                        range = 1..12
                    )
                    
                    // 日期选择
                    val maxDays = Calendar.getInstance().apply {
                        set(selectedYear, selectedMonth, 1)
                    }.getActualMaximum(Calendar.DAY_OF_MONTH)
                    
                    DatePickerRow(
                        label = "日期：",
                        value = selectedDay,
                        onValueChange = { selectedDay = it },
                        range = 1..maxDays
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val newDate = String.format("%d.%d.%d", selectedYear, selectedMonth + 1, selectedDay)
                        onDateChange(newDate)
                        isEditing = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { isEditing = false }) {
                    Text("取消")
                }
            }
        )
    }

    if (isEditingValue) {
        AlertDialog(
            onDismissRequest = { isEditingValue = false },
            title = { 
                Text(
                    "修改班次",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 班次选项
                    ScheduleTypeOption(
                        title = "正常班次",
                        description = "CT/DR/体检/DR+检",
                        currentValue = editedValue,
                        onClick = { 
                            editedValue = "CT"  // 默认选择CT
                            onValueChange(editedValue)
                            isEditingValue = false
                        }
                    )
                    
                    ScheduleTypeOption(
                        title = "无午休班",
                        description = "日/*",
                        currentValue = editedValue,
                        onClick = { 
                            editedValue = "CT*"  // 添加*号表示短班
                            onValueChange(editedValue)
                            isEditingValue = false
                        }
                    )
                    
                    ScheduleTypeOption(
                        title = "休息",
                        description = "公休/休",
                        currentValue = editedValue,
                        onClick = { 
                            editedValue = "休"
                            onValueChange(editedValue)
                            isEditingValue = false
                        }
                    )
                    
                    ScheduleTypeOption(
                        title = "夜班",
                        description = "值",
                        currentValue = editedValue,
                        onClick = { 
                            editedValue = "(值)"
                            onValueChange(editedValue)
                            isEditingValue = false
                        }
                    )
                    
                    ScheduleTypeOption(
                        title = "出夜班",
                        description = "出",
                        currentValue = editedValue,
                        onClick = { 
                            editedValue = "出"
                            onValueChange(editedValue)
                            isEditingValue = false
                        }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { isEditingValue = false }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
private fun ScheduleTypeOption(
    title: String,
    description: String,
    currentValue: String,
    onClick: () -> Unit
) {
    val scheduleType = ScheduleType.fromScheduleText(currentValue)
    val isSelected = when (title) {
        "正常班次" -> scheduleType == ScheduleType.NORMAL_WORK
        "短班" -> scheduleType == ScheduleType.SHORT_WORK
        "休息" -> scheduleType == ScheduleType.REST
        "夜班" -> scheduleType == ScheduleType.NIGHT_SHIFT
        "早班结束" -> scheduleType == ScheduleType.MORNING_END
        else -> false
    }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(
                if (isSelected) 
                    MaterialTheme.colorScheme.primaryContainer 
                else 
                    MaterialTheme.colorScheme.surface
            )
            .clickable(onClick = onClick)
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = if (isSelected) 
                    MaterialTheme.colorScheme.onPrimaryContainer 
                else 
                    MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = if (isSelected) 
                    MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f) 
                else 
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (isSelected) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = "已选择",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun DatePickerRow(
    label: String,
    value: Int,
    onValueChange: (Int) -> Unit,
    range: IntRange
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(
                onClick = { 
                    if (value > range.first) onValueChange(value - 1)
                },
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Remove,
                    contentDescription = "减少",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                text = value.toString(),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            IconButton(
                onClick = { 
                    if (value < range.last) onValueChange(value + 1)
                },
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "增加",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

// 添加辅助函数来生成 markdown 表格
private fun buildMarkdownTable(dates: List<String>, values: List<String>): String {
    val tableHeader = dates.joinToString(" | ", "| ", " |")
    val tableDivider = dates.joinToString(" | ", "| ", " |") { ":---:" }
    val tableValues = values.joinToString(" | ", "| ", " |")
    val tableEmojis = values.joinToString(" | ", "| ", " |") { value ->
        when (ScheduleType.fromScheduleText(value)) {
            ScheduleType.NIGHT_SHIFT -> "值夜班了🌙"
            ScheduleType.MORNING_END -> "早班结束🌞"
            ScheduleType.REST -> "睡大觉喽😴"
            ScheduleType.NORMAL_WORK -> "正常上班😊"
            ScheduleType.SHORT_WORK -> "没有午休😭"
            ScheduleType.UNKNOWN -> "未知班次❓"
        }
    }
    
    return """
        $tableHeader
        $tableDivider
        $tableValues
        $tableEmojis
    """.trimIndent()
}

@Preview(
    name = "显示表格后预",
    showBackground = true,
    showSystemUi = true
)
@Composable
fun ImagePickerScreenWithTablePreview() {
    var scheduleTableState by remember { mutableStateOf(
        """
        2024.9.23 | 2024.9.24 | 2024.9.25 | 2024.9.26 | 2024.9.27 | 2024.9.28 | 2024.9.29
        :---: | :---: | :---: | :---: | :---: | :---: | :---:
        (值) | 出 | 休 | CT | DR+检* | 公 | 休
        值夜班了🌙 | 早班结束🌞 | 睡大觉喽😴 | 正常上班😊 | 没有午休😭 | 睡大觉喽😴 | 睡大觉喽😴
        """.trimIndent()
    )}
    
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 图片显示区域
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.5f)
                    .padding(16.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surface,
                        shape = MaterialTheme.shapes.medium
                    )
                    .clip(MaterialTheme.shapes.medium)
                    .padding(1.dp),
                contentAlignment = Alignment.Center
            ) {
                // 模拟图片
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )
            }
            
            // 分隔栏
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(32.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Box(
                    modifier = Modifier
                        .width(50.dp)
                        .height(4.dp)
                        .background(
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            shape = MaterialTheme.shapes.small
                        )
                        .align(Alignment.Center)
                )
            }
            
            // 显示表格
            if (scheduleTableState.isNotEmpty()) {
                ScheduleTable(
                    markdownTable = scheduleTableState,
                    onTableUpdated = { newTable ->
                        scheduleTableState = newTable
                    }
                )
            }
            
            // 按钮区域
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Button(
                        onClick = { },
                        modifier = Modifier
                            .height(56.dp)
                            .width(200.dp)
                    ) {
                        Text("选择图片")
                    }

                    Button(
                        onClick = { },
                        modifier = Modifier
                            .height(56.dp)
                            .width(200.dp)
                    ) {
                        Text("分析图片")
                    }
                }
            }
        }
    }
}