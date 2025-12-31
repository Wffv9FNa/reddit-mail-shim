# Pre-Release TODO

This document tracks items that need to be completed before making this repository public.

## Critical Items (Must Complete)

### 1. Change Package Name ✅
- **Previous:** `com.example.redditshim`
- **New:** `com.Wffv9FNa.redditshim`
- **Status:** COMPLETED
- **Files updated:**
  - ✅ `app/build.gradle.kts:7` (namespace)
  - ✅ `app/build.gradle.kts:11` (applicationId)
  - ✅ Package declarations in all Kotlin files
  - ✅ Directory structure: `app/src/main/kotlin/com/Wffv9FNa/redditshim/`
  - ✅ Build verified (debug APK: 2.7M)

### 2. Add LICENSE File ✅
- **Status:** COMPLETED
- **License chosen:** MIT License
- **File:** `LICENSE` created in repository root
- **Note:** Update copyright holder name from "[Your Name]" placeholder before publishing

### 3. Create README.md ✅
- **Status:** COMPLETED
- **File:** `README.md` created in repository root
- **Contents:**
  - ✅ What the app does (technical problem statement)
  - ✅ Why it exists (solves tracking link problem)
  - ✅ Installation instructions (ADB + build from source)
  - ✅ Configuration instructions (Config.kt with line numbers and common package names)
  - ✅ Privacy statement (comprehensive: network, storage, logging, permissions, code transparency)
  - ✅ Testing instructions (ADB commands, logcat monitoring, fallback testing)
  - ✅ Build instructions (debug, release, clean builds with output paths)
  - ✅ Supported Reddit clients list (8 common clients with package names)
  - ✅ Requirements (Android 5.0+, API level 21)
  - ✅ Architecture breakdown (component descriptions with file paths and line numbers)
  - ✅ Known limitations (5 documented constraints)
  - ✅ Troubleshooting section (common issues with diagnostic commands)
  - ✅ Contributing guidelines (accepted/rejected contribution types)

### 4. Add ProGuard Rules ✅
- **Status:** COMPLETED
- **File:** `app/proguard-rules.pro` created/updated
- **Contents:**
  - ✅ OkHttp rules (dontwarn + keep directives for okhttp3, okio, conscrypt, bouncycastle, openjsse)
  - ✅ Kotlin Coroutines rules (MainDispatcherFactory, CoroutineExceptionHandler preservation)
  - ✅ AndroidX Lifecycle rules (LifecycleScope support)
  - ✅ Application classes preservation (com.Wffv9FNa.redditshim.** with line numbers)
  - ✅ Main activity explicit keep rule (MailClickShimActivity)
  - ✅ Debug attribute preservation (SourceFile, LineNumberTable, Exceptions, Signature)
  - ✅ Release logging removal (strips Log.d/v/i calls for APK size + privacy)
  - ✅ Aggressive optimization settings (5 passes, reduces 2.7MB → 731KB)
- **Fixed:** Updated package name from `com.example.redditshim` to `com.Wffv9FNa.redditshim`
- **APK naming:** Configured custom output names: `reddit-mail-shim-{version}-{buildType}.apk`

## Important Items (Should Complete)

### 5. Update CLAUDE.md
- **Issue:** Line 52 states "No content modification or parameter stripping" as a non-goal
- **Reality:** `MailClickShimActivity.kt:66-110` actually strips tracking parameters
- **Action:** Either update CLAUDE.md to reflect current behavior OR remove the URL cleaning feature for consistency

### 6. Privacy Policy/Statement ✅
- **Status:** COMPLETED
- **Location:** README.md section "Privacy and Data Collection"
- **Coverage:**
  - ✅ No data collection
  - ✅ No telemetry
  - ✅ No analytics
  - ✅ Network activity limited to redirect resolution only
  - ✅ No persistent storage
  - ✅ Logging behavior (debug vs release builds)
  - ✅ Permissions explanation (INTERNET only)
  - ✅ Code transparency statement

### 7. Decide on Shim.md
- **Current:** Design document in repository root
- **Options:**
  - Move to `docs/DESIGN.md`
  - Delete it (info is in CLAUDE.md)
  - Keep it as-is
- **Recommendation:** Move to `docs/DESIGN.md` or delete

### 8. Release Signing Configuration ✅
- **Status:** COMPLETED
- **Documentation:** README.md section "Release Signing Configuration"
- ✅ Document how to generate a signing key (keytool command included)
- ✅ Add instructions to README for building release APK (./gradlew assembleRelease)
- ✅ Confirm keystore files stay in `.gitignore` (verified lines 26-27)
- ✅ signing.properties format documented with example

## Nice to Have (Optional)

### 9. CONTRIBUTING.md ✅
- **Status:** COMPLETED (integrated into README.md)
- **Location:** README.md sections "Contributing" and "Technical Support"
- ✅ Guidelines for pull requests (clear technical rationale required)
- ✅ How to report issues (Technical Support section with diagnostic steps)
- ✅ Accepted/rejected contribution types (explicit lists with rationale)
- ✅ Development philosophy (minimalism, privacy, zero-complexity principle)

### 10. GitHub Repository Setup
If using GitHub:
- [ ] Add repository description
- [ ] Add topics/tags (android, reddit, privacy, tracking, kotlin)
- [ ] Enable Issues
- [ ] Add issue templates (bug report, feature request)
- [ ] Consider adding GitHub Action for automated builds
- [ ] Add shields/badges to README (API level, license, etc.)

### 11. Commit .claude/settings.local.json
- **Current:** Shows as modified in `git status`
- **Action:** Either commit the changes or revert them
- **Decision needed:** Is this file meant to be local-only or shared?

### 12. Version Strategy ✅
- **Status:** COMPLETED
- **Current:** `versionCode = 1`, `versionName = "1.0"`
- **Documentation:** README.md section "Version History"
- ✅ Version history section established (documents 1.0 release features)
- ✅ Implicit semantic versioning (MAJOR.MINOR format, extensible to PATCH)
- **Note:** Future releases should update both README.md Version History and app/build.gradle.kts

## Pre-Release Checklist

Before publishing to GitHub/public:

- [ ] All critical items completed
- [ ] Clean build succeeds: `./gradlew.bat clean assembleRelease`
- [ ] Test on physical device with real tracking link from Reddit email
- [ ] Verify no sensitive information in commit history
- [ ] Review all commit messages for professionalism
- [ ] Spell-check all documentation
- [ ] Test installation from APK on clean device
- [ ] Verify app works with Sync for Reddit (pre-built APK target)
- [ ] Verify fallback to browser works when Sync for Reddit not installed
- [ ] Test on multiple Android versions (if possible)
- [ ] Verify app doesn't appear in Recents after forwarding
- [ ] Check APK size is reasonable (target < 1MB, current: 731KB ✅)

## Known Issues to Address

- None currently identified

## Release Configuration Decisions

### Single APK Variant (Sync for Reddit Only)
- **Decision:** Ship one pre-built APK variant targeting Sync for Reddit only
- **Rationale:**
  - Avoid QUERY_ALL_PACKAGES permission (privacy concern)
  - No configuration UI (maintains zero-UI design principle)
  - Avoid shipping 9+ APK variants per release (maintenance burden)
- **Documentation:** README.md "Pre-Built APK Limitation" section explains this clearly
- **User impact:** Users with other Reddit clients must build from source with modified Config.kt
- **Future consideration:** Could ship multiple variants if demand warrants it

## Future Enhancements (Post-Release)

Ideas for future versions:
- Support for additional email tracking domains
- Multi-variant APK builds (one per major Reddit client)
- Automated tests
- GitHub Actions for automated builds

---

**Last Updated:** 2025-12-31
