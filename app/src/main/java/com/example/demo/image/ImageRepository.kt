package com.example.demo.image

import com.aallam.openai.api.BetaOpenAI
import com.aallam.openai.api.chat.ChatCompletionRequest
import com.aallam.openai.api.chat.ChatMessage
import com.aallam.openai.api.chat.ChatRole
import com.aallam.openai.api.model.ModelId
import com.aallam.openai.client.OpenAI
import com.aallam.openai.api.chat.ChatCompletion
import com.aallam.openai.api.chat.ContentPart
import com.aallam.openai.api.chat.ImagePart
import com.aallam.openai.api.chat.TextPart
import com.aallam.openai.client.OpenAIHost
import android.util.Base64
import com.aallam.openai.api.http.Timeout
import android.content.Context
import android.util.Log
import kotlin.time.Duration.Companion.seconds

class ImageRepository(
    private val context: Context
) {

    private val host = "https://open.bigmodel.cn/api/paas/v4/"
    private val apiKey = "d8cf7e81dc97fd21e176b783b4704101.fcmydfO7fCiSKzyN"

    private val openAI = OpenAI(
        token = apiKey,
        timeout = Timeout(socket = 90.seconds),
        host = OpenAIHost(baseUrl = host)

    )
    private val modelId = "glm-4v"
    private val prompt = """
读取图片
在回答问题之前,请先执行以下步骤:
1. 复述问题: 用你自己的话重新表述用户的问题或要求。确保你完全理解了问题的本质和目标。
2. 理解确认: 简要说明你对问题的理解,并指出任何可能的歧义或需要澄清的地方。
3. 思考过程: 简述你打算如何回答这个问题,包括你将采用的方法或步骤。
4. 自我评估: 快速评估你对该主题的知识水平和回答能力。
完成这些步骤后,请继续给出你的详细回答。请确保你的回答直接针对用户的原始问题。

先告诉我图片中的内容，，请务必保证你输出的是正确的信息 这很重要，如果表格内容是空白json内容是空白即可，不要看错行！！
最后按照json格式输出 钱淑阳的 每个日期的排班情况. 例如'{"钱淑阳": {"7-1": "(值)", "7-2": "出"}}'
原始问题:
"""

    private lateinit var messages: MutableList<ChatMessage>

    @OptIn(BetaOpenAI::class)
    suspend fun analyzeImage(imageBase64: String): Result<String> {
        return try {
            val reqList: ArrayList<ContentPart> = ArrayList()
            reqList.add(TextPart(prompt))
            reqList.add(ImagePart("data:image/jpeg;base64,$imageBase64"))

            messages.add(
                ChatMessage(
                    role = ChatRole.User,
                    content = reqList
                )
            )

            val chatCompletionRequest = ChatCompletionRequest(
                model = ModelId(modelId),
                messages = messages,
            )

            val completion: ChatCompletion = openAI.chatCompletion(chatCompletionRequest)
            val content = completion.choices.first().message.content

            if (content.isNullOrEmpty()) {
                Result.failure(Exception("图片解析失败"))
            } else {
                Log.d("ImageRepository", "content: $content")
                Result.success(content)
            }
        } catch (e: Exception) {
            Log.e("ImageRepository", "error: ${e.message}")
            Result.failure(e)
        }
    }

    fun cleanup() {
        openAI.close()
    }


    init {
        val shot_1_user_req = ArrayList<ContentPart>().apply {
            add(TextPart(prompt))
            add(ImagePart("data:image/jpeg;base64,${readImageFromAssets("shot_1.jpg")}"))
        }
        
        val shot_2_user_req = ArrayList<ContentPart>().apply {
            add(TextPart(prompt))
            add(ImagePart("data:image/jpeg;base64,${readImageFromAssets("shot_2.jpg")}"))
        }
        
        val shot_3_user_req = ArrayList<ContentPart>().apply {
            add(TextPart(prompt))
            add(ImagePart("data:image/jpeg;base64,${readImageFromAssets("shot_3.jpg")}"))
        }
        
        val shot_1_user_resp = """
### 1. 复述问题：
用户需要从图片内容中找到“钱淑阳”在每个日期的排班情况，并按照JSON格式输出。

### 2. 理解确认：
用户希望我提取图片中“钱淑阳”对应的排班信息，并返回JSON格式的结果。输入内容中的表格列出了从8月26日到9月1日的排班安排。

### 3. 思考过程：
1. 首先，我需要查看图片中的表格，专注于“钱淑阳”这一行。
2. 然后，提取每个日期对应的排班信息。
3. 最后，将这些信息整理成JSON格式。

### 4. 自我评估：
我对处理和解析图像中的表格信息有充分的知识和技能，可以准确提取所需的数据并按要求进行格式化。

### 详细回答：

根据图中表格提供的信息，钱淑阳的排班情况如下：

```json
{
  "钱淑阳": {
    "8-26": "CT",
    "8-27": "DR",
    "8-28": "CT",
    "8-29": "公",
    "8-30": "休",
    "8-31": "",
    "9-1": "(值)"
  }
}
```
"""

        val shot_2_user_resp = """
### 1. 复述问题：
用户需要从图片内容中找到“钱淑阳”在每个日期的排班情况，并按照JSON格式输出。

### 2. 理解确认：
用户希望我提取图片中“钱淑阳”对应的排班信息，并返回JSON格式的结果。输入内容中的表格列出了从9月2日到9月8日的排班安排。

### 3. 思考过程：
1. 首先，我需要查看图片中的表格，专注于“钱淑阳”这一行。
2. 然后，提取每个日期对应的排班信息。
3. 最后，将这些信息整理成JSON格式。

### 4. 自我评估：
我对处理和解析图像中的表格信息有充分的知识和技能，可以准确提取所需的数据并按要求进行格式化。

### 详细回答：

根据图中表格提供的信息，钱淑阳的排班情况如下：

```json
{
  "钱淑阳": {
    "9-2": "出",
    "9-3": "休",
    "9-4": "CT*",
    "9-5": "DR+检",
    "9-6": "休",
    "9-7": "(值)",
    "9-8": "出"
  }
}
```
"""

        val shot_3_user_resp = """
### 1. 复述问题：
用户需要从图片内容中找到“钱淑阳”在每个日期的排班情况，并按照JSON格式输出。

### 2. 理解确认：
用户希望我提取图片中“钱淑阳”对应的排班信息，并返回JSON格式的结果。输入内容中的表格列出了从9月9日到9月15日的排班安排。

### 3. 思考过程：
1. 首先，我需要查看图片中的表格，专注于“钱淑阳”这一行。
2. 然后，提取每个日期对应的排班信息。
3. 最后，将这些信息整理成JSON格式。

### 4. 自我评估：
我对处理和解析图像中的表格信息有充分的知识和技能，可以准确提取所需的数据并按要求进行格式化。

### 详细回答：

根据图中表格提供的信息，钱淑阳的排班情况如下：

```json
{
  "钱淑阳": {
    "9-9": "休",
    "9-10": "CT",
    "9-11": "DR+检",
    "9-12": "CT",
    "9-13": "(值)",
    "9-14": "出",
    "9-15": ""
  }
}
```
"""

        messages = listOf(
            ChatMessage(role = ChatRole.User, content = shot_1_user_req),
            ChatMessage(role = ChatRole.Assistant, content = shot_1_user_resp),
            ChatMessage(role = ChatRole.User, content = shot_2_user_req),
            ChatMessage(role = ChatRole.Assistant, content = shot_2_user_resp),
            ChatMessage(role = ChatRole.User, content = shot_3_user_req),
            ChatMessage(role = ChatRole.Assistant, content = shot_3_user_resp)
        ).toMutableList()
//        messages = listOf(
//            ChatMessage(role = ChatRole.System, content = "你是一个专业的值班表分析专家，请按照以下格式输出json")
//        ).toMutableList()
    }

    private fun readImageFromAssets(fileName: String): String {
        return try {
            context.assets.open(fileName).use { inputStream ->
                val bytes = inputStream.readBytes()
                Base64.encodeToString(bytes, Base64.DEFAULT)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

}