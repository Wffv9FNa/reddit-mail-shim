Project: Reddit Mail Link Shim (Android)

1. Purpose

Build a tiny Android "shim" app that intercepts Reddit digest email tracking links (e.g., https://click.redditmail.com/...) and forwards the user to the installed Reddit client (e.g., Sync) using the final resolved Reddit URL after following redirects.

This avoids modifying or resigning the target Reddit client APK.


---

2. Goals

1. Default link handling

When a user taps a click.redditmail.com link in email, Android should route the intent to this shim by default.



2. Redirect resolution

The shim should follow HTTP redirects (3xx) to obtain the final destination URL.

Stop early when a URL is on a "reddit destination host" (e.g., reddit.com, www.reddit.com, redd.it, v.redd.it, redditmedia.com).



3. Forwarding

Forward the resolved URL via ACTION_VIEW to a specified target package (a Reddit client).

If explicit package targeting fails, fall back to generic ACTION_VIEW to allow the system chooser or browser.



4. Minimal UX footprint

No visible UI in the normal case.

Activity should start, resolve, forward, and finish quickly.





---

3. Non-goals

No attempt to bypass app signature checks or patch existing APKs.

No content modification, parameter stripping, or ad/tracking removal.

No background service. Only runs on link tap.

No analytics, logging to external services, or data collection.



---

4. User stories

1. "When I tap a Reddit digest link in Gmail, it opens in my Reddit client rather than a browser."


2. "If the client cannot open the link, it falls back gracefully to the browser."


3. "It works even if the mail link is a tracking redirect and not a direct reddit.com URL."




---

5. Functional requirements

1. Intent filter

Activity must declare:

ACTION_VIEW

Categories: DEFAULT, BROWSABLE

<data android:scheme="https" android:host="click.redditmail.com"/>

<data android:scheme="http" android:host="click.redditmail.com"/> (optional but recommended)




2. Exported

Activity must be android:exported="true".



3. Networking

Must request android.permission.INTERNET.

Must not do network on the main thread.



4. Redirect handling

Implement redirect resolution with:

Maximum hops (e.g., 5-8)

Connect/read timeouts (e.g., 5s each)

Handle both HEAD and GET (fallback to GET if HEAD returns 405 or omits Location)

Properly resolve relative Location headers.




5. Forwarding

Attempt explicit package-targeted Intent(ACTION_VIEW, resolvedUri) to configured target package.

On failure (ActivityNotFoundException, SecurityException), launch generic VIEW intent.



6. Finish

The shim activity must call finish() after forwarding attempt.



7. Configurability

Target package name should be editable in code (constant).

Redirect target host allowlist should be editable in code.





---

6. Non-functional requirements

1. Safety and privacy

No telemetry.

No persistent storage of URLs or browsing history.

Do not log full URLs to logcat in release builds.



2. Reliability

Fail gracefully:

If redirect resolution fails, forward the original URL to generic VIEW.

Avoid infinite loops with hop limit.

Robust to malformed URLs.




3. Performance

Complete resolution + forward ideally under 1-2 seconds on normal networks.

No repeated retries.



4. Compatibility

Minimum SDK: 21+.

Target SDK: current stable (e.g., 34).

Works with common email clients that dispatch external VIEW intents (Gmail, etc.).

Should not require root.



5. Maintainability

Single-activity app.

Minimal dependencies (prefer standard library + AndroidX).





---

7. Deliverables

1. Source code repository

Gradle Android project with:

AndroidManifest.xml

MailClickShimActivity.kt

build.gradle(.kts) files

minimal ProGuard rules (optional)




2. Build artifact

Debug APK for quick testing.

Release APK (signed) for daily use.



3. Configuration notes

Document how to:

Change target package.

Add additional tracking hosts.

Add additional destination host allowlist entries.

Install APK and set as default handler.




4. Test procedure

ADB commands for:

Forcing an intent dispatch to the shim.

Confirming the shim is listed as a handler for the URL.


Manual steps:

Tap link in email and set "Always".






---

8. Acceptance criteria

1. Tapping https://click.redditmail.com/... opens the installed Reddit client on the correct final post/comment feed page at least for standard digest links.


2. If the Reddit client is not installed, the link opens in the browser.


3. If redirect resolution fails, the app still opens something (generic VIEW) rather than silently failing.


4. No visible UI in normal operation, and the shim does not remain in Recents.




---

9. Implementation outline (for an LLM)

Components

MailClickShimActivity: entry point, resolves redirect chain, forwards intent, finishes.


Core functions

resolveRedirects(startUri): Uri

Loop up to N hops:

request HEAD

if no redirect or 405 -> request GET once

if redirect -> update URL

if host matches allowlist -> return early


Return last known URL or original.


forwardToTarget(uri)

Try explicit package VIEW intent

If it fails, fire generic VIEW intent.



Manifest

One intent-filter for the mail tracking domain(s).



---

10. Constraints and risks

Some mail clients open links inside an in-app browser; user may need to disable that to allow external intents.

The tracking host or redirect behavior may change; the app should be easy to update with extra hosts or patterns.

The target Reddit client may have limitations on what it accepts; generic VIEW fallback must work.



---


---