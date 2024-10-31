package com.example.demo.image

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
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
import androidx.compose.foundation.background
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

enum class ScheduleType {
    NORMAL_WORK,    // CT/DR/体检/DR+检
    SHORT_WORK,     // 带*号的班次
    REST,           // 公休/休
    NIGHT_SHIFT,    // (值)
    MORNING_END,    // 出
    UNKNOWN;        // 未知类型

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
    val imageRepository = remember { ImageRepository(context) }
    
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
        selectedImageUri = uri
        scale = 1f
        offset = Offset.Zero
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
                onClick = { launcher.launch("image/*") },
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
                
                // 在这里显示表格（分隔栏下方）
                if (scheduleTableState.isNotEmpty()) {
                    ScheduleTable(scheduleTableState)
                }
                
                // ���钮区域
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
                                                                        ScheduleType.NIGHT_SHIFT -> "值夜班🌙"
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
                                                                dialogMessage = "未找到排班信息"
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
                                        dialogMessage = "图片解析失败～请重试\n多次重试失败联系🍓草莓园"
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

@Composable
fun ScheduleTable(markdownTable: String) {
    if (markdownTable.isNotEmpty()) {
        val lines = markdownTable.lines()
        if (lines.size >= 4) {
            val dates = lines[0].trim('|').split('|').map { it.trim() }
            val values = lines[2].trim('|').split('|').map { it.trim() }
            val emojis = lines[3].trim('|').split('|').map { it.trim() }

            // 添加外限制框
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surface,
                        shape = MaterialTheme.shapes.medium
                    )
                    .clip(MaterialTheme.shapes.medium)
                    .padding(1.dp)
            ) {
                // 添加水平滚动
                Box(
                    modifier = Modifier
                        .horizontalScroll(rememberScrollState())
                        .padding(8.dp)
                ) {
                    Row(
                        modifier = Modifier.width(IntrinsicSize.Max),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        dates.forEachIndexed { index, date ->
                            Box(
                                modifier = Modifier.padding(
                                    start = if (index == 0) 0.dp else 4.dp,
                                    end = if (index == dates.size - 1) 0.dp else 4.dp
                                )
                            ) {
                                // 添加左边的分隔线
                                if (index > 0) {
                                    Box(
                                        modifier = Modifier
                                            .width(1.dp)
                                            .fillMaxHeight()
                                            .background(
                                                MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                            )
                                            .align(Alignment.CenterStart)
                                    )
                                }
                                
                                ScheduleCell(
                                    date = date,
                                    value = values.getOrNull(index) ?: "",
                                    emoji = emojis.getOrNull(index) ?: "",
                                    modifier = Modifier.width(85.dp) // 固定每个单元格的宽度
                                )
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
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.small
            )
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = date.split(".").takeLast(2).joinToString("."),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1 // 限制为单行
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
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(vertical = 0.2.dp),
            maxLines = 1 // 限制为单行
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
        Text(
            text = emoji,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1 // 限制为单行
        )
    }
}

@Preview(
    name = "显示表格后预览",
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
        值夜班🌙 | 早班结束🌞 | 睡大觉喽😴 | 正常上班😊 | 没有午休😭 | 睡大觉喽😴 | 睡大觉喽😴
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
                ScheduleTable(scheduleTableState)
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