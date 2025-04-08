package com.example.musicvibrationapp.emotion.api

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStream
import java.util.Properties
import java.util.concurrent.TimeUnit

class LLMService(private val context: Context) {
    private val TAG = "LLM_Service"
    private val apiKey: String
    private val modelName: String
    private val apiUrl: String
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
    
    init {
        // Load LLM configuration from properties file
        val config = loadLlmConfiguration()
        apiKey = config.first
        modelName = config.second
        apiUrl = config.third
    }
        
    private fun loadLlmConfiguration(): Triple<String, String, String> {
        val properties = Properties()
        try {
            val inputStream: InputStream = context.assets.open("api_keys.properties")
            properties.load(inputStream)
            
            val apiKey = properties.getProperty("LLM_API_KEY", "your_api_key")
            val modelName = properties.getProperty("LLM_MODEL_NAME", "your_model_name")
            val apiUrl = properties.getProperty("LLM_API_URL", "your_api_endpoint_url")
            
            return Triple(apiKey, modelName, apiUrl)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load LLM configuration from properties file", e)
            return Triple("your_api_key", "your_model_name", "your_api_endpoint_url")
        }
    }

    suspend fun sendMessage(message: String): String = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "LLM_Request: Preparing request to LLM API")
            
            val json = JSONObject().apply {
                put("model", modelName)
                put("messages", JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "system")
                        put("content", """
                            You are a professional music emotion analysis expert. You need to analyze based on Plutchik's eight basic emotions model:
                            1. Joy
                            2. Trust
                            3. Fear
                            4. Surprise
                            5. Sadness
                            6. Disgust
                            7. Anger
                            8. Anticipation

                            Your tasks are:
                            1. Analyze the provided audio feature data and video text information
                            2. Must choose one emotion that matches best from the above 8 emotions
                            3. The answer format must contain the line "Primary Emotion: [Emotion Type]"
                            4. Then you can briefly explain the basis for your judgment
                            
                            Note:
                            - Must and can only choose one from the 8 basic emotions
                            - Do not use other emotion vocabulary
                            - Keep the answer concise and clear
                        """.trimIndent())
                    })
                    put(JSONObject().apply {
                        put("role", "user")
                        put("content", message)
                    })
                })
            }
            
            Log.v(TAG, "LLM_Request: Full request body:\n${json}")
            
            val request = Request.Builder()
                .url(apiUrl)
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", "Bearer $apiKey")
                .post(json.toString().toRequestBody("application/json".toMediaType()))
                .build()

            Log.d(TAG, "LLM_Request: Sending request to LLM API")
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""
            
            Log.d(TAG, "LLM_Response: Status=${response.code}, Length=${responseBody.length}")
            Log.v(TAG, "LLM_Response: Full response body:\n$responseBody")
            
            responseBody

        } catch (e: Exception) {
            Log.e(TAG, "LLM_Error: API call failed", e)
            "Error: ${e.message}"
        }
    }

    companion object {
        @Volatile
        private var instance: LLMService? = null

        fun getInstance(context: Context): LLMService =
            instance ?: synchronized(this) {
                instance ?: LLMService(context).also { instance = it }
            }
    }
} 