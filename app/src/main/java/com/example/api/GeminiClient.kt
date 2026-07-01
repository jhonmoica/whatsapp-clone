package com.example.api

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
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

    suspend fun generateResponse(prompt: String, chatHistory: List<Pair<String, Boolean>> = emptyList()): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            Log.e(TAG, "Gemini API Key is not set or is using placeholder value.")
            return@withContext "Desculpe, a chave de API do Gemini não está configurada. Configure o GEMINI_API_KEY no painel de Secrets!"
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
                    Log.e(TAG, "Request failed with code ${response.code}: $errBody")
                    return@withContext "Opa! Tive um problema de conexão com meus servidores (Código ${response.code})."
                }

                val resBody = response.body?.string() ?: return@withContext "Desculpe, recebi uma resposta vazia."
                val jsonObject = JSONObject(resBody)
                val candidates = jsonObject.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val content = candidates.getJSONObject(0).optJSONObject("content")
                    if (content != null) {
                        val parts = content.optJSONArray("parts")
                        if (parts != null && parts.length() > 0) {
                            return@withContext parts.getJSONObject(0).optString("text", "Não consegui formular uma resposta.")
                        }
                    }
                }
                "Não consegui processar essa mensagem."
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during Gemini API call", e)
            "Desculpe, ocorreu um erro ao tentar processar sua mensagem: ${e.localizedMessage}"
        }
    }
}
