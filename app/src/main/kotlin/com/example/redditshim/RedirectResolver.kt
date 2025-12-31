package com.example.redditshim

import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

class RedirectResolver(private val client: OkHttpClient) {

    companion object {
        private const val TAG = "RedirectResolver"

        /**
         * HTTP status codes that indicate a redirect.
         */
        private val REDIRECT_CODES = setOf(301, 302, 303, 307, 308)
    }

    /**
     * Resolves HTTP redirects for the given URL.
     * Returns the final destination URL after following up to MAX_REDIRECT_HOPS redirects.
     * Stops early if the URL host matches any domain in REDDIT_DOMAINS.
     * Must be called from a background thread (e.g., via withContext(Dispatchers.IO)).
     */
    suspend fun resolveRedirects(startUrl: String): String = withContext(Dispatchers.IO) {
        var currentUrl = startUrl
        var hops = 0

        if (BuildConfig.DEBUG) {
            Log.d(TAG, "Starting redirect resolution for: $startUrl")
        }

        while (hops < Config.MAX_REDIRECT_HOPS) {
            // Check if current URL is already a Reddit destination
            val currentUri = Uri.parse(currentUrl)
            val host = currentUri.host?.lowercase()

            if (host != null && Config.REDDIT_DOMAINS.contains(host)) {
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "Hit Reddit domain '$host' at hop $hops, stopping early")
                }
                return@withContext currentUrl
            }

            // Special handling for reddit.app.link - extract $original_url parameter
            if (host == "reddit.app.link") {
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "Detected reddit.app.link URL: $currentUrl")
                }
                // Manually parse query string since getQueryParameter may not handle $ correctly
                val query = currentUri.query
                val originalUrl = query?.split("&")?.find { it.startsWith("\$original_url=") || it.startsWith("original_url=") }?.substringAfter("=")?.let { encoded ->
                    try {
                        java.net.URLDecoder.decode(encoded, "UTF-8")
                    } catch (e: Exception) {
                        if (BuildConfig.DEBUG) {
                            Log.w(TAG, "Failed to decode original_url: ${e.message}")
                        }
                        null
                    }
                }
                if (originalUrl != null) {
                    if (BuildConfig.DEBUG) {
                        Log.d(TAG, "Extracted original URL from reddit.app.link: $originalUrl")
                    }
                    currentUrl = originalUrl
                    hops++
                    continue
                } else {
                    if (BuildConfig.DEBUG) {
                        Log.w(TAG, "No \$original_url parameter found. Query: $query")
                    }
                }
            }

            // Try HEAD request first (lightweight)
            val nextUrl = tryResolveRedirect(currentUrl, useHead = true)

            if (nextUrl == null) {
                // HEAD failed or returned 405, try GET
                val nextUrlFromGet = tryResolveRedirect(currentUrl, useHead = false)
                if (nextUrlFromGet == null || nextUrlFromGet == currentUrl) {
                    // No more redirects
                    if (BuildConfig.DEBUG) {
                        Log.d(TAG, "No more redirects at hop $hops, final URL: $currentUrl")
                    }
                    return@withContext currentUrl
                }
                currentUrl = nextUrlFromGet
            } else if (nextUrl == currentUrl) {
                // No redirect, we're done
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "No redirect at hop $hops, final URL: $currentUrl")
                }
                return@withContext currentUrl
            } else {
                // Got a redirect
                currentUrl = nextUrl
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "Hop $hops: Redirected to $currentUrl")
                }
            }

            hops++
        }

        if (BuildConfig.DEBUG) {
            Log.d(TAG, "Reached max hops ($hops), final URL: $currentUrl")
        }

        return@withContext currentUrl
    }

    /**
     * Attempts to resolve a single redirect for the given URL.
     * Returns the Location header value if a redirect is found, or the same URL if not.
     * Returns null on error or if the server rejects the method.
     */
    private fun tryResolveRedirect(url: String, useHead: Boolean): String? {
        val request = Request.Builder()
            .url(url)
            .method(if (useHead) "HEAD" else "GET", null)
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                val code = response.code

                if (code == 405 && useHead) {
                    // Method Not Allowed for HEAD, signal to retry with GET
                    return null
                }

                if (REDIRECT_CODES.contains(code)) {
                    // Extract Location header
                    val location = response.header("Location")
                    if (location != null) {
                        // Resolve relative URLs
                        return resolveUrl(url, location)
                    }
                }

                // No redirect
                return url
            }
        } catch (e: IOException) {
            if (BuildConfig.DEBUG) {
                Log.w(TAG, "Network error resolving redirect for $url: ${e.message}")
            }
            // Return original URL on error
            return url
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                Log.e(TAG, "Unexpected error resolving redirect for $url", e)
            }
            return url
        }
    }

    /**
     * Resolves a potentially relative URL against a base URL.
     * Returns an absolute URL.
     */
    private fun resolveUrl(baseUrl: String, relativeUrl: String): String {
        return try {
            val base = java.net.URL(baseUrl)
            val resolved = java.net.URL(base, relativeUrl)
            resolved.toString()
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                Log.w(TAG, "Failed to resolve relative URL '$relativeUrl' against '$baseUrl'", e)
            }
            // Fallback: assume it's absolute
            relativeUrl
        }
    }
}
