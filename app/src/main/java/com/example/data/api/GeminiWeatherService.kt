package com.example.data.api

import android.util.Log
import com.example.BuildConfig
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class GeminiPart(val text: String)

@JsonClass(generateAdapter = true)
data class GeminiContent(val parts: List<GeminiPart>)

@JsonClass(generateAdapter = true)
data class GeminiRequest(val contents: List<GeminiContent>)

@JsonClass(generateAdapter = true)
data class GeminiResponse(val candidates: List<GeminiCandidate>?)

@JsonClass(generateAdapter = true)
data class GeminiCandidate(val content: GeminiContent?)

object GeminiWeatherService {
    private const val TAG = "GeminiWeatherService"
    private val client = OkHttpClient.Builder()
        .connectTimeout(45, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .build()

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val requestAdapter = moshi.adapter(GeminiRequest::class.java)
    private val responseAdapter = moshi.adapter(GeminiResponse::class.java)

    suspend fun getWeatherAdvice(
        city: String,
        temp: Double,
        apparentTemp: Double,
        humidity: Int,
        windSpeed: Double,
        description: String
    ): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            Log.w(TAG, "API Key is missing or placeholders used.")
            return@withContext "عذراً، لم يتم ضبط مفتاح Gemini API بشكل صحيح في الإعدادات للتطبيق للحصول على نصائح الذكاء الاصطناعي الحية."
        }

        val prompt = """
            بصفتك "عالِم الطقس ونظام ذكي خبير"، قم بتحليل الطقس الحالي لمدينة $city:
            - درجة الحرارة الحالية: $temp درجة مئوية.
            - درجة الحرارة المحسوسة: $apparentTemp درجة مئوية.
            - الرطوبة النسبية: $humidity%.
            - سرعة الرياح: $windSpeed كم/ساعة.
            - حالة السماء والجو العام: $description.

            الرجاء تقديم:
            1. كبسولة تحليلية موجزة وجذابة جداً (بصوت ودود، شاعري وبليغ) للوضع الحالي باللغة العربية الفصحى.
            2. توصيتين طبيتين أو فيزيائيتين وعمليتين للمستخدم بأسلوب مهذب وممتع (مثلاً: نوعية الملابس المناسبة، تهوية الغرفة، الوقاية، أو الإضاءة، شرب السوائل).
            اجعل الرد مختصراً، جذاباً، لا يتجاوز 150 كلمة، واستخدم رموز إيموجي تناسب الطقس لإضافة لمسة فنية!
        """.trimIndent()

        val geminiReq = GeminiRequest(
            contents = listOf(
                GeminiContent(
                    parts = listOf(GeminiPart(text = prompt))
                )
            )
        )

        try {
            val jsonBody = requestAdapter.toJson(geminiReq)
            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = jsonBody.toRequestBody(mediaType)

            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"

            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errBody = response.body?.string() ?: ""
                    Log.e(TAG, "Error from Gemini API: ${response.code} -> $errBody")
                    return@withContext "خطأ في الاتصال بالذكاء الاصطناعي (كود: ${response.code})."
                }
                val resString = response.body?.string() ?: ""
                val geminiRes = responseAdapter.fromJson(resString)
                val adviceText = geminiRes?.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                return@withContext adviceText ?: "لم نتمكن من تلخيص الطقس، الجو رائع على أي حال!"
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during Gemini Call", e)
            return@withContext "تعذر الحصول على نصائح الطقس بسبب عطل مؤقت."
        }
    }
}
