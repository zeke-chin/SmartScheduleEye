package com.example.demo.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter

object LogManager {
    private const val TAG = "LogManager"
    private val logBuffer = mutableListOf<String>()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    private var lastLogFile: File? = null

    fun addLog(tag: String, message: String) {
        val timeStamp = dateFormat.format(Date())
        val logMessage = "[$timeStamp] $tag: $message"
        logBuffer.add(logMessage)
        Log.d(tag, message)
    }

    fun exportLogs(context: Context): Result<File> {
        return try {
            val fileName = "app_logs_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.txt"
            val downloadsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            val logFile = File(downloadsDir, fileName)

            FileWriter(logFile).use { writer ->
                // 写入自定义日志
                writer.append("=== 自定义日志 ===\n\n")
                logBuffer.forEach { log ->
                    writer.append(log)
                    writer.append("\n")
                }
                
                writer.append("\n=== 系统日志 ===\n\n")
                
                // 获取系统日志
                try {
                    val process = Runtime.getRuntime().exec("logcat -d")
                    val bufferedReader = BufferedReader(
                        InputStreamReader(process.inputStream)
                    )
                    
                    var line: String?
                    while (bufferedReader.readLine().also { line = it } != null) {
                        // 只保存应用相关的日志
                        if (line?.contains(context.packageName) == true) {
                            writer.append(line)
                            writer.append("\n")
                        }
                    }
                } catch (e: Exception) {
                    writer.append("获取系统日志失败: ${e.message}\n")
                    e.printStackTrace(PrintWriter(writer))
                }
                
                writer.append("\n=== 设备信息 ===\n\n")
                writer.append("设备型号: ${android.os.Build.MODEL}\n")
                writer.append("Android 版本: ${android.os.Build.VERSION.RELEASE}\n")
                writer.append("应用版本: ${context.packageManager.getPackageInfo(context.packageName, 0).versionName}\n")
                writer.append("系统时间: ${dateFormat.format(Date())}\n")
            }

            lastLogFile = logFile
            Result.success(logFile)
        } catch (e: Exception) {
            Log.e(TAG, "Error exporting logs: ${e.message}")
            Result.failure(e)
        }
    }

    fun openLogFolder(context: Context) {
        try {
            val downloadsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            if (downloadsDir?.exists() == true) {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    setDataAndType(Uri.parse(downloadsDir.absolutePath), "*/*")
                }
                
                try {
                    context.startActivity(Intent.createChooser(intent, "打开日志文件夹"))
                } catch (e: Exception) {
                    val alternativeIntent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                        addCategory(Intent.CATEGORY_OPENABLE)
                        type = "*/*"
                    }
                    context.startActivity(Intent.createChooser(alternativeIntent, "打开日志文件夹"))
                }
            } else {
                Log.e(TAG, "Downloads directory does not exist")
                Toast.makeText(context, "日志文件夹不存在", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error opening log folder: ${e.message}")
            Toast.makeText(
                context, 
                "无法打开日志文件夹，请手动前往: ${context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)?.absolutePath}", 
                Toast.LENGTH_LONG
            ).show()
            
            // 记录详细错误信息
            addLog(TAG, "打开日志文件夹失败: ${e.message}\n${e.stackTraceToString()}")
        }
    }

    fun getLastLogFile(): File? = lastLogFile
} 