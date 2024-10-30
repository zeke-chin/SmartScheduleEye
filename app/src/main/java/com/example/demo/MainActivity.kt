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


// MainActivity 类：应用程序的主入口点
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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