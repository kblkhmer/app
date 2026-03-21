package com.lotterykh.app

import android.content.Context
import android.util.Base64
import android.util.Log
import com.google.android.play.core.integrity.IntegrityManagerFactory
import com.google.android.play.core.integrity.IntegrityTokenRequest
import java.security.SecureRandom

class PlayIntegrityHelper(private val context: Context) {

    companion object {
        private const val TAG = "PlayIntegrity"
        private const val API_KEY = "AQ.Ab8RN6KVTE8dzqJGh1o00EIXsRPCC20FGdxO8EMH859RuonR8Q"
        private const val CLOUD_PROJECT_NUMBER = 0L // Set your Google Cloud project number
    }

    fun interface IntegrityCallback {
        fun onResult(token: String?, error: String?)
    }

    fun requestIntegrityToken(callback: IntegrityCallback) {
        val nonce = generateNonce()
        val integrityManager = IntegrityManagerFactory.create(context)

        val requestBuilder = IntegrityTokenRequest.builder()
            .setNonce(nonce)

        if (CLOUD_PROJECT_NUMBER != 0L) {
            requestBuilder.setCloudProjectNumber(CLOUD_PROJECT_NUMBER)
        }

        integrityManager.requestIntegrityToken(requestBuilder.build())
            .addOnSuccessListener { response ->
                val token = response.token()
                Log.d(TAG, "Integrity token obtained successfully")
                callback.onResult(token, null)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Integrity check failed: ${e.message}")
                callback.onResult(null, e.message)
            }
    }

    private fun generateNonce(): String {
        val bytes = ByteArray(24)
        SecureRandom().nextBytes(bytes)
        return Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
    }
}
