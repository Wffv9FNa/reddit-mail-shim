# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is an Android "shim" app that intercepts Reddit digest email tracking links (e.g., `https://click.redditmail.com/...`) and forwards users to their installed Reddit client (e.g., Sync) using the final resolved URL after following redirects.

**Key design principles:**
- Minimal UX footprint: no visible UI in normal operation
- No telemetry, tracking, or data collection
- Single-activity app with minimal dependencies
- Fail gracefully with fallback to generic VIEW intents

## Architecture

### Core Flow
1. User taps `click.redditmail.com` link in email
2. Android routes intent to `MailClickShimActivity` (via intent filter)
3. Activity resolves HTTP redirects (3xx) on background thread until hitting a Reddit destination host
4. Forwards resolved URL to target Reddit client via explicit package-targeted ACTION_VIEW
5. Falls back to generic ACTION_VIEW if explicit targeting fails
6. Activity finishes immediately

### Key Components

**MailClickShimActivity**
- Entry point activity (must be `exported="true"`)
- Handles intent filter for `click.redditmail.com` (both http/https)
- Coordinates redirect resolution and forwarding
- Must not perform network operations on main thread

**Redirect Resolution (`resolveRedirects(startUri): Uri`)**
- Maximum 5-8 hops with connect/read timeouts (~5s each)
- Uses HEAD requests, falls back to GET if HEAD returns 405 or omits Location
- Properly resolves relative Location headers
- Stops early when URL matches Reddit destination host allowlist (reddit.com, www.reddit.com, redd.it, v.redd.it, redditmedia.com)
- Returns last known URL or original on failure

**Forwarding (`forwardToTarget(uri)`)**
- First attempts: explicit package-targeted `Intent(ACTION_VIEW, resolvedUri)` to configured target package
- On `ActivityNotFoundException` or `SecurityException`: launches generic VIEW intent for system chooser/browser

### Configuration Points

These should be editable constants in code:
- **Target package name**: The Reddit client package to forward to
- **Redirect target host allowlist**: Reddit domains that signal end of redirect chain
- **Max redirect hops**: Limit to prevent infinite loops (5-8 recommended)
- **Network timeouts**: Connect and read timeouts for HTTP requests

## Development

### Requirements
- Minimum SDK: 21+
- Target SDK: 34 (current stable)
- Language: Kotlin
- Build system: Gradle (Kotlin DSL preferred)

### Manifest Requirements
```xml
<uses-permission android:name="android.permission.INTERNET"/>

<activity
    android:name=".MailClickShimActivity"
    android:exported="true">
    <intent-filter>
        <action android:name="android.intent.action.VIEW"/>
        <category android:name="android.intent.category.DEFAULT"/>
        <category android:name="android.intent.category.BROWSABLE"/>
        <data android:scheme="https" android:host="click.redditmail.com"/>
        <data android:scheme="http" android:host="click.redditmail.com"/>
    </intent-filter>
</activity>
```

### Testing

**ADB testing:**
```bash
# Test intent dispatch to shim
adb shell am start -a android.intent.action.VIEW -d "https://click.redditmail.com/..."

# Verify shim is registered as handler
adb shell pm query-activities -a android.intent.action.VIEW -d "https://click.redditmail.com"
```

**Manual testing:**
1. Install APK: `adb install app/build/outputs/apk/debug/reddit-mail-shim-1.0-debug.apk`
2. Tap a tracking link in email (Gmail, etc.)
3. Select shim and choose "Always"
4. Verify it opens in Reddit client with correct content

### Safety Constraints

- **No telemetry or analytics**
- **No persistent storage** of URLs or browsing history
- **No full URL logging** to logcat in release builds
- **No content modification** or parameter stripping
- **Fail gracefully**: If resolution fails, forward original URL to generic VIEW rather than silently failing

### Build Outputs

- **Debug APK**: For testing (`reddit-mail-shim-1.0-debug.apk`, 2.7 MB, app/build/outputs/apk/debug/)
- **Release APK**: Signed for daily use (`reddit-mail-shim-1.0-release.apk`, 731 KB, app/build/outputs/apk/release/)

## Acceptance Criteria

1. Tapping `https://click.redditmail.com/...` opens the installed Reddit client on the correct final post/comment
2. If Reddit client not installed, link opens in browser
3. If redirect resolution fails, app still opens something (generic VIEW) rather than failing silently
4. No visible UI in normal operation
5. Shim does not remain in Recents after forwarding
