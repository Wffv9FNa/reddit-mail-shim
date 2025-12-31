# Reddit Mail Link Shim

Android intent filter shim that intercepts Reddit digest email tracking links (`click.redditmail.com`) and forwards users to their installed Reddit client using the final resolved URL after following HTTP redirects.

## Problem Statement

Reddit digest emails contain tracking links like `https://click.redditmail.com/CL0/https://...` that route through a tracking server before redirecting to actual Reddit content. Tapping these links in email clients typically opens the browser, not your preferred Reddit app. This shim sits between the email client and the final destination, resolves all redirects, strips tracking parameters, and launches your Reddit client directly.

## Technical Operation

1. User taps `click.redditmail.com` link in email client
2. Android intent system routes to `MailClickShimActivity` via intent filter registration
3. Activity spawns background coroutine to resolve HTTP redirects
4. Redirect resolution follows 3xx responses (max 8 hops, 5s timeouts per request)
5. Uses HEAD requests, falls back to GET if server returns 405 or omits Location header
6. Terminates early when URL matches Reddit domain allowlist (reddit.com, redd.it, v.redd.it, etc.)
7. Strips tracking parameters: `utm_*`, `$*`, `_branch_*`, `%24*`, `%5F*`
8. Constructs ACTION_VIEW intent with explicit package targeting for configured Reddit client
9. Falls back to generic ACTION_VIEW if explicit targeting fails (SecurityException, ActivityNotFoundException)
10. Activity finishes immediately, leaves no trace in Recents

## Requirements

- Android 5.0+ (API level 21)
- Internet permission (redirect resolution requires network access)
- Target Reddit client installed (e.g., Sync for Reddit, Relay, Boost, Joey, Infinity, rif, Apollo-equivalent)

## Pre-Built APK Limitation

**IMPORTANT:** Pre-built release APKs from GitHub are hardcoded to forward links to **Sync for Reddit** (`com.laurencedawson.reddit_sync`) only. This package name is compiled into the binary at build time (`Config.kt:4`).

**If you use a different Reddit client:**
1. You must build from source (see Method 2 below)
2. Edit `app/src/main/kotlin/com/Wffv9FNa/redditshim/Config.kt` line 4 before building
3. Change `TARGET_PACKAGE` to your client's package name (see Configuration section for common package names)

**Why not support all clients in one APK?**
- Runtime app detection requires QUERY_ALL_PACKAGES permission (privacy/security concern on Android 11+)
- Configuration UI violates zero-UI design principle
- Shipping 9+ APK variants per release adds maintenance burden
- Most users use a single Reddit client consistently

Pre-built APK users with Sync installed get direct forwarding. Users with other clients installed will fall back to system chooser/browser (shim still resolves redirects and strips tracking parameters, but loses direct-to-app benefit).

## Installation

### Method 1: Direct APK Installation
```bash
adb install reddit-mail-shim-1.0-release.apk
```

### Method 2: Build from Source
```bash
git clone <repository-url>
cd shim
./gradlew assembleRelease
adb install app/build/outputs/apk/release/reddit-mail-shim-1.0-release.apk
```

### Post-Installation Setup
1. Open any `click.redditmail.com` link (from Reddit digest email or manual ADB launch)
2. Android will present app chooser dialog
3. Select "Reddit Mail Shim"
4. Choose "Always" to set as default handler
5. Shim will forward to Reddit client and disappear

## Configuration

All configuration lives in `app/src/main/kotlin/com/Wffv9FNa/redditshim/Config.kt`:

**Target Reddit Client Package (line 4):**
```kotlin
const val TARGET_PACKAGE = "com.laurencedawson.reddit_sync"  // Default: Sync for Reddit
```

Common package names:
- Sync for Reddit: `com.laurencedawson.reddit_sync`
- Sync for Reddit (Pro): `com.laurencedawson.reddit_sync.pro`
- Sync for Reddit (Dev): `com.laurencedawson.reddit_sync.dev`
- Relay for Reddit: `free.reddit.news` or `reddit.news`
- Boost for Reddit: `com.rubenmayayo.reddit`
- Joey for Reddit: `o.o.joey`
- Infinity for Reddit: `ml.docilealligator.infinityforreddit`
- rif is fun: `com.andrewshu.android.reddit`
- rif golden platinum: `com.andrewshu.android.redditdonation`

**Reddit Domain Allowlist (lines 6-9):**
Domains that signal end of redirect chain. Expand if Reddit introduces new domains:
```kotlin
val REDDIT_DOMAINS = setOf(
    "reddit.com", "www.reddit.com", "old.reddit.com", "new.reddit.com",
    "redd.it", "v.redd.it", "i.redd.it", "redditmedia.com"
)
```

**Redirect Limits (lines 11-15):**
```kotlin
const val MAX_REDIRECT_HOPS = 8           // Prevent infinite redirect loops
const val CONNECT_TIMEOUT_MS = 5000L      // 5 seconds to establish connection
const val READ_TIMEOUT_MS = 5000L         // 5 seconds to read response
const val WRITE_TIMEOUT_MS = 5000L        // 5 seconds to write request
```

After modifying `Config.kt`, rebuild and reinstall:
```bash
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/reddit-mail-shim-1.0-debug.apk
```

## Build Instructions

**Debug Build (unsigned, includes debugging symbols):**
```bash
./gradlew assembleDebug
# Output: app/build/outputs/apk/debug/reddit-mail-shim-1.0-debug.apk (2.7 MB)
```

**Release Build (requires signing configuration):**
```bash
./gradlew assembleRelease
# Output: app/build/outputs/apk/release/reddit-mail-shim-1.0-release.apk (731 KB with ProGuard)
```

**Clean Build (recommended after configuration changes):**
```bash
./gradlew clean assembleDebug
```

### Release Signing Configuration

Create `signing.properties` in project root (excluded from git):
```properties
storeFile=/path/to/keystore.jks
storePassword=<keystore-password>
keyAlias=<key-alias>
keyPassword=<key-password>
```

Generate keystore if needed:
```bash
keytool -genkey -v -keystore release.keystore -alias shim -keyalg RSA -keysize 2048 -validity 10000
```

## Testing

**Test Intent Dispatch:**
```bash
adb shell am start -a android.intent.action.VIEW -d "https://click.redditmail.com/CL0/https://www.reddit.com/r/Android/comments/abc123/example/"
```

**Verify Shim Registration:**
```bash
adb shell pm query-activities -a android.intent.action.VIEW -d "https://click.redditmail.com"
# Should list: com.Wffv9FNa.redditshim/.MailClickShimActivity
```

**Monitor Redirect Resolution (debug builds only):**
```bash
adb logcat -s RedditShim:*
```

**Test with Real Email Link:**
1. Forward a Reddit digest email to your device
2. Tap any post/comment link in the email
3. Verify shim intercepts and forwards to Reddit client
4. Confirm content matches email preview
5. Verify shim does not appear in Recents (Settings > Apps > Recent apps)

**Test Fallback Behavior (Reddit client not installed):**
```bash
adb shell pm uninstall com.laurencedawson.reddit_sync
adb shell am start -a android.intent.action.VIEW -d "https://click.redditmail.com/..."
# Should open in browser instead of crashing
```

## Privacy and Data Collection

**Network Activity:**
- HTTP requests sent ONLY to resolve redirects from `click.redditmail.com` to final Reddit URL
- No requests to analytics servers, telemetry endpoints, or third-party services
- No DNS queries beyond standard Android DNS resolution for redirect targets

**Data Storage:**
- Zero persistent storage of URLs, browsing history, or user data
- No SharedPreferences, SQLite databases, or file writes
- All state exists transiently in memory during activity lifecycle (< 1 second typical)

**Logging:**
- Debug builds: Log redirect chain and final URL to logcat (local device only, never transmitted)
- Release builds: No URL logging, only error conditions

**Permissions:**
- `INTERNET`: Required for HTTP redirect resolution. No other permissions requested or used.

**Code Transparency:**
- Single-file activity (`MailClickShimActivity.kt`, 130 lines)
- Single-file redirect resolver (`RedirectResolver.kt`, 90 lines)
- Single-file configuration (`Config.kt`, 20 lines)
- No obfuscated dependencies, no native code, no runtime code generation

## Architecture

**Component Breakdown:**

`MailClickShimActivity` (app/src/main/kotlin/com/Wffv9FNa/redditshim/MailClickShimActivity.kt):
- Entry point activity with intent filter for `click.redditmail.com` (http/https schemes)
- Launches coroutine on `lifecycleScope` to resolve redirects off main thread
- Strips tracking parameters from resolved URL (lines 66-110)
- Attempts explicit package-targeted intent first, generic VIEW intent on failure
- Configured: `exported="true"`, `excludeFromRecents="true"`, `noHistory="true"`, `launchMode="singleTask"`

`RedirectResolver` (app/src/main/kotlin/com/Wffv9FNa/redditshim/RedirectResolver.kt):
- OkHttpClient-based redirect resolution (lines 15-25)
- HEAD request primary, GET fallback for non-compliant servers (lines 30-50)
- Handles relative Location headers via `Uri.resolve()` (line 45)
- Special case: Extracts `url` parameter from `reddit.app.link` shortlinks (lines 55-60)
- Returns original URL on error rather than failing silently

`Config` (app/src/main/kotlin/com/Wffv9FNa/redditshim/Config.kt):
- Kotlin object (singleton) with compile-time constants
- No runtime configuration UI (intentional: minimizes attack surface)

**Dependencies:**
- OkHttp 4.12.0: HTTP client (redirect resolution)
- Kotlin Coroutines 1.7.3: Async execution (off main thread)
- AndroidX Core KTX 1.12.0: Kotlin extensions for Android APIs
- AndroidX Lifecycle Runtime KTX 2.6.2: `lifecycleScope` coroutine scope

**APK Size:**
- Debug: 2.7 MB (`reddit-mail-shim-1.0-debug.apk`, includes debugging symbols, unoptimized)
- Release: 731 KB (`reddit-mail-shim-1.0-release.apk`, ProGuard enabled, symbols stripped)

## Known Limitations

1. **Reddit client must support standard reddit.com URLs**: Some clients use proprietary URL schemes. If explicit targeting fails, shim falls back to generic VIEW intent (typically opens browser).

2. **No support for non-Reddit tracking domains**: Only intercepts `click.redditmail.com`. Other email tracking services (e.g., `links.emailprovider.com`) require separate intent filter registration.

3. **Redirect resolution requires network connectivity**: If device is offline, shim forwards original tracking URL to Reddit client. Client must handle redirect resolution internally or fail gracefully.

4. **No per-subreddit client routing**: All Reddit URLs go to same configured client. Cannot route `/r/Android` to one client and `/r/AskReddit` to another.

5. **Tracking parameter stripping is best-effort**: URL cleaning targets common tracking params (`utm_*`, Branch.io markers). Novel tracking schemes may pass through undetected.

## Troubleshooting

**Shim doesn't appear when tapping email links:**
- Verify installation: `adb shell pm list packages | grep redditshim`
- Check intent filter registration: `adb shell pm query-activities -a android.intent.action.VIEW -d "https://click.redditmail.com"`
- Clear default handler: Settings > Apps > Default apps > Opening links > Reddit Mail Shim > Clear defaults

**Opens browser instead of Reddit client:**
- Verify target package installed: `adb shell pm list packages | grep <package-name>`
- Check logcat for ActivityNotFoundException: `adb logcat -s RedditShim:*`
- Verify package name in `Config.kt:4` matches installed client exactly

**Stuck on redirect resolution (loading forever):**
- Increase timeouts in `Config.kt:13-15` (default 5000ms)
- Check network connectivity: `adb shell ping -c 3 reddit.com`
- Monitor redirect chain: `adb logcat -s RedditShim:*` (debug builds only)

**Shim appears in Recents after use:**
- Verify manifest attributes: `android:excludeFromRecents="true"` and `android:noHistory="true"`
- Bug: Some Android versions ignore `excludeFromRecents` for activities with intent filters. No known workaround.

## Version History

**1.0 (Current)**
- Initial release
- Support for `click.redditmail.com` tracking links
- HTTP redirect resolution (max 8 hops)
- Tracking parameter stripping
- Explicit package targeting with generic VIEW fallback
- `reddit.app.link` shortlink URL extraction

## License

MIT License. See `LICENSE` file for full text.

## Contributing

This is a minimal shim app by design. Feature additions should be evaluated against the core principle: zero UI, zero telemetry, zero complexity beyond redirect-and-forward.

**Accepted contribution types:**
- Bug fixes (crashes, incorrect redirect handling, intent routing failures)
- Additional tracking parameter patterns for URL cleaning
- Support for additional Reddit email tracking domains
- Performance improvements (reduced latency, smaller APK size)
- Documentation improvements (clearer testing instructions, additional client package names)

**Rejected contribution types:**
- Configuration UI (use `Config.kt` constants instead)
- Analytics or telemetry (privacy violation)
- Support for non-Reddit domains (scope creep)
- Persistent storage of user preferences (privacy violation)

Submit pull requests with clear technical rationale. Include test results showing before/after behavior.

## Technical Support

**Before opening an issue:**
1. Run test command: `adb shell am start -a android.intent.action.VIEW -d "https://click.redditmail.com/..."`
2. Capture logcat: `adb logcat -s RedditShim:* > shim.log`
3. Verify manifest registration: `adb shell pm query-activities -a android.intent.action.VIEW -d "https://click.redditmail.com"`
4. Test with browser fallback: Temporarily uninstall Reddit client, verify shim opens browser

**Include in issue report:**
- Android version (e.g., Android 13, API level 33)
- Device model (e.g., Pixel 6, Samsung Galaxy S21)
- Target Reddit client package name and version
- Full tracking URL (redact personal identifiers if needed)
- Logcat output from test execution
- Expected behavior vs. actual behavior

**Response time expectations:**
This is a volunteer project. Issues will be triaged based on severity (crashes > incorrect behavior > feature requests). No SLA guaranteed.
