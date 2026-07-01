package com.example.api

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object GeminiClient {
    private const val TAG = "GeminiClient"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent"

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private fun generateMockResponse(prompt: String): String {
        val normalized = prompt.trim().lowercase(java.util.Locale.ROOT)
        return when {
            normalized.contains("olá") || normalized.contains("oi") || normalized.contains("bom dia") || normalized.contains("boa tarde") || normalized.contains("boa noite") -> {
                "Olá! Tudo bem? Eu sou o Gemini Bot, o seu assistente virtual de Inteligência Artificial aqui no WhatsApp Clone. Como posso te ajudar hoje? 💬✨"
            }
            normalized.contains("ajuda") || normalized.contains("ajude") || normalized.contains("o que você faz") -> {
                "Eu posso te ajudar com muitas coisas! Posso simular conversas, tirar dúvidas, sugerir ideias, ajudar com tarefas diárias ou apenas bater um papo amigável. Como quer começar? 🚀"
            }
            normalized.contains("whatsapp") || normalized.contains("clone") -> {
                "Este aplicativo é um Clone do WhatsApp extremamente completo, profissional e interativo! Ele conta com banco de dados local Room, simulador de chamadas de voz e vídeo, atualizações de status (stories) e essa integração inteligente comigo! Incrível, né? 📱⚡"
            }
            normalized.contains("quem é você") || normalized.contains("seu nome") -> {
                "Eu sou o Gemini Bot! Sou uma Inteligência Artificial integrada diretamente a este chat para tornar a sua experiência dinâmica e divertida. 🤖💚"
            }
            normalized.contains("criador") || normalized.contains("criou") || normalized.contains("desenvolveu") -> {
                "Fui desenvolvido para integrar este projeto incrível de clone do WhatsApp, com o objetivo de demonstrar o poder de aplicativos modernos em Kotlin, Jetpack Compose e Firebase! 🛠️⭐"
            }
            normalized.contains("piada") || normalized.contains("engraçado") -> {
                val piadas = listOf(
                    "Por que o programador foi ao shopping? Para fazer um 'push' no carrinho! 🛒💻",
                    "O que o ponteiro do relógio disse para o outro? 'A gente se vê a cada hora!' 🕒😂",
                    "O que o desenvolvedor de software fala quando seu código finalmente compila? 'Ufa, funcionou na minha máquina!' 🖥️😅",
                    "Por que o livro de matemática ficou triste? Porque ele tinha muitos problemas! 📚😜"
                )
                piadas.random()
            }
            normalized.contains("obrigado") || normalized.contains("obrigada") || normalized.contains("valeu") || normalized.contains("agradeço") -> {
                "De nada! Estou sempre aqui para ajudar quando você precisar. Se tiver mais alguma dúvida ou quiser bater papo, é só chamar! 😊👍"
            }
            else -> {
                "Que legal! Fiquei muito feliz com a sua mensagem. Como seu assistente pessoal de Inteligência Artificial, estou pronto para explorar mais ideias ou conversar com você sobre qualquer assunto! O que mais gostaria de saber? 🧠🌟"
            }
        }
    }

    suspend fun generateResponse(prompt: String, chatHistory: List<Pair<String, Boolean>> = emptyList()): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY" || apiKey == "YOUR_GEMINI_API_KEY_HERE") {
            Log.w(TAG, "Gemini API Key is not set, falling back to simulated smart response.")
            delay(1000) // Realistic typing simulation delay
            return@withContext generateMockResponse(prompt)
        }

        try {
            val contentsArray = JSONArray()

            // Include chat history if available (limit to last 10 messages for context)
            val limitedHistory = chatHistory.takeLast(10)
            for ((msgText, isOutgoing) in limitedHistory) {
                val role = if (isOutgoing) "user" else "model"
                val contentObj = JSONObject().apply {
                    put("role", role)
                    val partsArray = JSONArray().apply {
                        put(JSONObject().apply { put("text", msgText) })
                    }
                    put("parts", partsArray)
                }
                contentsArray.put(contentObj)
            }

            // Append the new prompt
            val currentContentObj = JSONObject().apply {
                put("role", "user")
                val partsArray = JSONArray().apply {
                    put(JSONObject().apply { put("text", prompt) })
                }
                put("parts", partsArray)
            }
            contentsArray.put(currentContentObj)

            val systemInstruction = JSONObject().apply {
                val partsArray = JSONArray().apply {
                    put(JSONObject().apply { 
                        put("text", "Você é o 'Gemini Bot', um assistente de inteligência artificial amigável, prestativo e inteligente integrado diretamente no clone do WhatsApp. Suas respostas devem ser conversacionais, adequadas para chat de WhatsApp, curtas ou de tamanho moderado, usando emojis ocasionalmente para parecer amigável e natural. Responda sempre em português do Brasil.") 
                    })
                }
                put("parts", partsArray)
            }

            val requestBodyJson = JSONObject().apply {
                put("contents", contentsArray)
                put("systemInstruction", systemInstruction)
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.7)
                })
            }

            val requestBody = requestBodyJson.toString().toRequestBody("application/json".toMediaType())

            val urlWithKey = "$BASE_URL?key=$apiKey"
            val request = Request.Builder()
                .url(urlWithKey)
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errBody = response.body?.string() ?: ""
                    Log.e(TAG, "Request failed with code ${response.code}: $errBody, falling back to mock response.")
                    return@withContext generateMockResponse(prompt)
                }

                val resBody = response.body?.string() ?: return@withContext generateMockResponse(prompt)
                val jsonObject = JSONObject(resBody)
                val candidates = jsonObject.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val content = candidates.getJSONObject(0).optJSONObject("content")
                    if (content != null) {
                        val parts = content.optJSONArray("parts")
                        if (parts != null && parts.length() > 0) {
                            return@withContext parts.getJSONObject(0).optString("text", generateMockResponse(prompt))
                        }
                    }
                }
                generateMockResponse(prompt)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during Gemini API call, falling back to mock response.", e)
            generateMockResponse(prompt)
        }
    }
}
