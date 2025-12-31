package com.example.redditshim

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/**
 * Shim activity that intercepts Reddit mail tracking links,
 * resolves redirects, and forwards to the configured Reddit client.
 */
class MailClickShimActivity : ComponentActivity() {

    companion object {
        private const val TAG = "MailClickShimActivity"
    }

    private val okHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(Config.CONNECT_TIMEOUT_MS, TimeUnit.MILLISECONDS)
            .readTimeout(Config.READ_TIMEOUT_MS, TimeUnit.MILLISECONDS)
            .writeTimeout(Config.WRITE_TIMEOUT_MS, TimeUnit.MILLISECONDS)
            .followRedirects(false) // We handle redirects manually
            .followSslRedirects(false)
            .build()
    }

    private val redirectResolver by lazy {
        RedirectResolver(okHttpClient)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Extract URL from intent
        val originalUrl = intent?.data?.toString()

        if (originalUrl == null) {
            if (BuildConfig.DEBUG) {
                Log.w(TAG, "No URL in intent data, finishing")
            }
            finish()
            return
        }

        if (BuildConfig.DEBUG) {
            Log.d(TAG, "Received URL: $originalUrl")
        }

        // Launch coroutine to resolve redirects and forward
        lifecycleScope.launch {
            try {
                // Resolve redirects on background thread
                val resolvedUrl = redirectResolver.resolveRedirects(originalUrl)

                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "Resolved URL: $resolvedUrl")
                }

                // Forward to target Reddit client
                forwardToTarget(resolvedUrl)

            } catch (e: Exception) {
                if (BuildConfig.DEBUG) {
                    Log.e(TAG, "Error during redirect resolution", e)
                }
                // Fallback: forward original URL
                forwardToTarget(originalUrl)
            } finally {
                // Always finish this activity
                finish()
            }
        }
    }

    /**
     * Forwards the URL to the target Reddit client.
     * First tries explicit package targeting, then falls back to generic VIEW.
     */
    private fun forwardToTarget(url: String) {
        val uri = Uri.parse(url)

        // First attempt: explicit package targeting
        val explicitIntent = Intent(Intent.ACTION_VIEW, uri).apply {
            setPackage(Config.TARGET_PACKAGE)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "Attempting explicit forward to package: ${Config.TARGET_PACKAGE}")
            }
            startActivity(explicitIntent)
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "Successfully forwarded to ${Config.TARGET_PACKAGE}")
            }
            return
        } catch (e: ActivityNotFoundException) {
            if (BuildConfig.DEBUG) {
                Log.w(TAG, "Target package not found: ${Config.TARGET_PACKAGE}, falling back to generic VIEW")
            }
        } catch (e: SecurityException) {
            if (BuildConfig.DEBUG) {
                Log.w(TAG, "Security exception targeting ${Config.TARGET_PACKAGE}, falling back to generic VIEW")
            }
        }

        // Fallback: generic ACTION_VIEW (system chooser or browser)
        val genericIntent = Intent(Intent.ACTION_VIEW, uri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "Launching generic ACTION_VIEW")
            }
            startActivity(genericIntent)
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "Successfully launched generic VIEW")
            }
        } catch (e: ActivityNotFoundException) {
            if (BuildConfig.DEBUG) {
                Log.e(TAG, "No activity found to handle VIEW intent, URL: $url")
            }
            // Silent failure - nothing we can do
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                Log.e(TAG, "Unexpected error forwarding URL", e)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // OkHttpClient cleanup happens automatically
        // No manual cleanup needed - resources will be released by GC
    }
}
