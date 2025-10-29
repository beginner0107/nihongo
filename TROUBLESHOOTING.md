# 🔧 Troubleshooting Guide

## App Crash on Startup (NetworkMonitor Issue)

### Problem
After adding network optimizations, the app crashes immediately on startup with:
```
PROCESS STARTED (xxxx) for package com.nihongo.conversation
PROCESS ENDED (xxxx) for package com.nihongo.conversation
```

### Root Cause
The `NetworkMonitor` class requires access to Android's `ConnectivityManager` to monitor network state. This requires the `ACCESS_NETWORK_STATE` permission in AndroidManifest.xml.

### Solution
Add the following permission to `AndroidManifest.xml`:

```xml
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
```

**Full AndroidManifest.xml:**
```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />  <!-- ADD THIS -->
    <uses-permission android:name="android.permission.RECORD_AUDIO" />

    <application
        android:name=".NihongoApp"
        android:allowBackup="true"
        android:label="@string/app_name"
        android:supportsRtl="true"
        android:theme="@style/Theme.NihongoConversation">
        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:theme="@style/Theme.NihongoConversation">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>

</manifest>
```

### Steps to Fix

1. **Add Permission** (Already Done)
   ```xml
   <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
   ```

2. **Clean and Rebuild**
   ```bash
   ./gradlew clean
   ./gradlew assembleDebug
   ```

3. **Reinstall the App**
   - Uninstall the old version from your device
   - Install the new APK with the updated manifest

### Why This Permission is Needed

The `NetworkMonitor` class needs to:
- Check if the device is online/offline
- Detect connection type (WiFi, Cellular, Ethernet)
- Monitor connection changes in real-time
- Determine if connection is metered (cellular data)

Without this permission, Android throws a `SecurityException` when trying to access `ConnectivityManager`, causing the app to crash immediately.

---

## Common Build Issues

### Issue: JDK jlink Error
```
Error while executing process /path/to/jlink with arguments {...}
```

**Cause:** Gradle incompatibility with JDK 23

**Solution:** This is a Gradle issue unrelated to the app code. The Kotlin compilation succeeds. Options:
1. Use JDK 17 or 21 (LTS versions)
2. Ignore the error if Kotlin compilation succeeds
3. Run just Kotlin compilation: `./gradlew compileDebugKotlin`

### Issue: Hilt/Dagger Dependency Injection Errors

**Symptoms:**
- App crashes with `NullPointerException`
- Missing `@Inject` constructor
- Circular dependency errors

**Solution:**
1. Verify all `@Singleton` classes have `@Inject constructor`
2. Check that all modules are `@InstallIn(SingletonComponent::class)`
3. Rebuild to regenerate Hilt components: `./gradlew clean build`

**Example - Correct Setup:**
```kotlin
// Singleton with injected dependencies
@Singleton
class NetworkMonitor @Inject constructor(
    @ApplicationContext private val context: Context
) {
    // ...
}

// Module providing dependencies
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides
    @Singleton
    fun provideGson(): Gson = GsonBuilder().create()
}
```

### Issue: DataStore Initialization Error

**Symptoms:**
- `IllegalStateException: Cannot read from DataStore before it is initialized`

**Solution:**
DataStore is initialized lazily. The first read will create the file. No action needed - this is expected behavior.

---

## Network Optimization Issues

### Issue: Offline Mode Not Working

**Check:**
1. Is `ACCESS_NETWORK_STATE` permission granted?
2. Are common phrases initialized?
   ```kotlin
   // Call this once on app startup
   geminiApiService.initializeOfflineData()
   ```
3. Is NetworkMonitor detecting connectivity correctly?
   ```kotlin
   networkMonitor.isOnline.collect { isOnline ->
       Log.d("Network", "Online: $isOnline")
   }
   ```

### Issue: Cached Responses Not Being Used

**Check:**
1. Is OfflineManager caching responses?
2. Are cache keys consistent?
3. Check cache size: `offlineManager.getCacheSize()`

**Debug Logging:**
```kotlin
// In GeminiApiService
offlineManager.getCachedResponse(cacheKey)?.let { cached ->
    Log.d("Cache", "Cache HIT for key: $cacheKey")
    emit(cached)
    return@flow
}
Log.d("Cache", "Cache MISS for key: $cacheKey")
```

### Issue: High Data Usage Despite Optimizations

**Verify:**
1. GZIP compression enabled (automatic with OkHttp)
2. Payload optimization applied:
   ```kotlin
   // Should see these in logs:
   // - History limited to 20 messages
   // - System prompt truncated to 500 chars
   // - Messages truncated to 2000 chars
   ```
3. Connection pooling active (check OkHttp logs)

---

## Performance Testing

### Test Network Optimization

```kotlin
@Test
fun `verify payload optimization reduces size`() {
    val longHistory = List(50) { "message $it" to true }
    val optimized = geminiApiService.optimizeHistory(longHistory)

    // Should only keep last 20
    assertEquals(20, optimized.size)

    // Should truncate long messages
    assertTrue(optimized.all { it.first.length <= 2000 })
}
```

### Test Offline Support

```kotlin
@Test
fun `verify offline cache works`() = runTest {
    val key = "test|1"
    val response = "こんにちは"

    offlineManager.cacheResponse(key, response)
    val cached = offlineManager.getCachedResponse(key)

    assertEquals(response, cached)
}
```

### Test Network Monitoring

```kotlin
@Test
fun `verify network state detection`() {
    val isOnline = networkMonitor.isCurrentlyOnline()
    val connectionType = networkMonitor.getConnectionType()

    // Should detect current connection
    assertNotNull(connectionType)
}
```

---

## Debugging Tips

### Enable Detailed Logging

**build.gradle.kts:**
```kotlin
android {
    buildTypes {
        debug {
            buildConfigField("boolean", "DEBUG_NETWORK", "true")
        }
    }
}
```

**NetworkMonitor.kt:**
```kotlin
if (BuildConfig.DEBUG_NETWORK) {
    Log.d("Network", "Connection type: ${getConnectionType()}")
    Log.d("Network", "Is metered: ${isMeteredConnection()}")
}
```

### Monitor Network Calls

**NetworkModule.kt:**
```kotlin
.addInterceptor(HttpLoggingInterceptor().apply {
    level = if (BuildConfig.DEBUG) {
        HttpLoggingInterceptor.Level.BODY
    } else {
        HttpLoggingInterceptor.Level.NONE
    }
})
```

### Check Cache Statistics

```kotlin
suspend fun printCacheStats() {
    val cacheSize = offlineManager.getCacheSize()
    val cached = offlineManager.getAllCachedResponses().first()
    val pending = offlineManager.getPendingMessages()
    val phrases = offlineManager.getCommonPhrases()

    Log.d("Cache", """
        Cache Statistics:
        - Size: ${cacheSize / 1024}KB
        - Cached responses: ${cached.size}
        - Pending messages: ${pending.size}
        - Common phrases: ${phrases.size}
    """.trimIndent())
}
```

---

## Migration Checklist

If upgrading from a version without network optimizations:

- [ ] Add `ACCESS_NETWORK_STATE` permission
- [ ] Uninstall old version of app
- [ ] Install new version with updated manifest
- [ ] Clear app data (optional, but recommended)
- [ ] Test offline mode with airplane mode
- [ ] Verify cached responses work
- [ ] Check data usage is reduced

---

## Getting Help

If issues persist:

1. **Check Logcat:** `adb logcat | grep -i "error\|exception\|fatal"`
2. **Clean Build:** `./gradlew clean assembleDebug`
3. **Verify Permissions:** Check Settings > Apps > Nihongo > Permissions
4. **Test Network:** Toggle airplane mode to test offline support
5. **Check Hilt:** Verify all `@Inject` constructors and `@Module` annotations

For performance issues, check [NETWORK_OPTIMIZATIONS.md](NETWORK_OPTIMIZATIONS.md) for detailed metrics and benchmarks.
