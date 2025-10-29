# Emergency Build Fixes - NihonGo Conversation App

## 🚨 Critical Issues Resolved

### 1. ✅ gradle.properties - Memory Settings Added

**File:** `gradle.properties`

```properties
# ADDED:
org.gradle.jvmargs=-Xmx4096m -XX:MaxMetaspaceSize=512m
org.gradle.daemon=true
org.gradle.parallel=true
```

---

### 2. ✅ app/build.gradle.kts - Removed Material Icons Extended

**File:** `app/build.gradle.kts` (Line 79)

```kotlin
// REMOVED:
// implementation("androidx.compose.material:material-icons-extended")
```

---

### 3. ✅ ChatScreen.kt - All Icons Replaced with Emoji

**File:** `presentation/chat/ChatScreen.kt`

#### Import Changes:
```kotlin
// REMOVED:
// import androidx.compose.material.icons.Icons
// import androidx.compose.material.icons.automirrored.filled.ArrowBack
// import androidx.compose.material.icons.filled.*

// REPLACED WITH:
// Icons replaced with emoji text
```

#### Code Changes:

1. **Back Button** (Line 70-72):
```kotlin
// BEFORE:
Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, ...)

// AFTER:
Text("←", style = MaterialTheme.typography.headlineMedium)
```

2. **Review Button** (Line 76):
```kotlin
// BEFORE:
Icon(imageVector = Icons.Default.HistoryEdu, ...)

// AFTER:
Text("📖", style = MaterialTheme.typography.headlineSmall)
```

3. **Volume Toggle** (Line 79-82):
```kotlin
// BEFORE:
Icon(imageVector = if (uiState.autoSpeak) Icons.Default.VolumeUp else Icons.Default.VolumeOff, ...)

// AFTER:
Text(if (uiState.autoSpeak) "🔊" else "🔇", style = MaterialTheme.typography.headlineSmall)
```

4. **Error Icon** (Line 171-174):
```kotlin
// BEFORE:
Icon(imageVector = Icons.Default.ErrorOutline, ...)

// AFTER:
Text("⚠️", style = MaterialTheme.typography.bodyLarge)
```

5. **Hint Button** (Line 355):
```kotlin
// BEFORE:
Icon(imageVector = Icons.Default.Lightbulb, ...)

// AFTER:
Text("💡", style = MaterialTheme.typography.bodyLarge)
```

---

### 4. ✅ VoiceButton.kt - All Icons Replaced

**File:** `presentation/chat/VoiceButton.kt`

#### Import Changes:
```kotlin
// REMOVED:
// import androidx.compose.material.icons.Icons
// import androidx.compose.material.icons.filled.Mic
// import androidx.compose.material.icons.filled.Stop
```

#### Code Changes:

1. **Mic/Stop Button** (Line 75-79):
```kotlin
// BEFORE:
Icon(imageVector = if (isListening) Icons.Default.Stop else Icons.Default.Mic, ...)

// AFTER:
Text(if (isListening) "■" else "🎤", style = MaterialTheme.typography.headlineMedium, color = Color.White)
```

2. **Error State** (Line 123-126):
```kotlin
// BEFORE:
Icon(imageVector = Icons.Default.Stop, tint = MaterialTheme.colorScheme.error, ...)

// AFTER:
Text("⚠️", style = MaterialTheme.typography.bodyLarge)
```

---

### 5. 🔄 REMAINING FILES TO FIX

#### HintDialog.kt
**File:** `presentation/chat/HintDialog.kt`

```kotlin
// Line 6-8: REMOVE these imports:
// import androidx.compose.material.icons.Icons
// import androidx.compose.material.icons.filled.Close
// import androidx.compose.material.icons.filled.Lightbulb
// import androidx.compose.material.icons.filled.VolumeUp

// Line 49-52: Replace Lightbulb icon:
// BEFORE:
Icon(imageVector = Icons.Default.Lightbulb, ...)
// AFTER:
Text("💡", style = MaterialTheme.typography.headlineMedium)

// Line 61-64: Replace Close icon:
// BEFORE:
Icon(imageVector = Icons.Default.Close, ...)
// AFTER:
Text("✕", style = MaterialTheme.typography.headlineMedium)

// Line 161-164: Replace VolumeUp icon:
// BEFORE:
Icon(imageVector = Icons.Default.VolumeUp, ...)
// AFTER:
Text("🔊", style = MaterialTheme.typography.bodyLarge)
```

#### DifficultyIndicator.kt
**File:** `presentation/chat/DifficultyIndicator.kt`

```kotlin
// Line 7-8: REMOVE these imports:
// import androidx.compose.material.icons.Icons
// import androidx.compose.material.icons.filled.School
// import androidx.compose.material.icons.filled.Star

// Line 45-49: Replace School icon:
// BEFORE:
Icon(imageVector = Icons.Default.School, ...)
// AFTER:
Text("🎓", style = MaterialTheme.typography.bodySmall)

// Line 57-62: Replace Star icons (appears twice):
// BEFORE:
repeat(stars) {
    Icon(imageVector = Icons.Default.Star, ...)
}
// AFTER:
repeat(stars) {
    Text("⭐", style = MaterialTheme.typography.bodySmall)
}

// Line 98-102: Replace Star icons in CompactDifficultyIndicator:
// BEFORE:
repeat(stars) {
    Icon(imageVector = Icons.Default.Star, ...)
}
// AFTER:
Text("⭐".repeat(stars), style = MaterialTheme.typography.bodySmall)
```

#### GrammarBottomSheet.kt
**File:** `presentation/chat/GrammarBottomSheet.kt`

```kotlin
// Line 10-13: REMOVE these imports:
// import androidx.compose.material.icons.Icons
// import androidx.compose.material.icons.filled.Close
// import androidx.compose.material.icons.filled.ExpandMore
// import androidx.compose.material.icons.filled.Lightbulb
// import androidx.compose.material.icons.filled.MenuBook

// Line 94-98: Replace MenuBook icon:
// BEFORE:
Icon(imageVector = Icons.Default.MenuBook, ...)
// AFTER:
Text("📚", style = MaterialTheme.typography.headlineMedium)

// Line 107-110: Replace Close icon:
// BEFORE:
Icon(imageVector = Icons.Default.Close, ...)
// AFTER:
Text("✕", style = MaterialTheme.typography.headlineMedium)

// Line 155-159: Replace Lightbulb icon:
// BEFORE:
Icon(imageVector = Icons.Default.Lightbulb, ...)
// AFTER:
Text("💡", style = MaterialTheme.typography.bodyLarge)

// Line 205-209: Replace ExpandMore icon:
// BEFORE:
Icon(imageVector = Icons.Default.ExpandMore, ...)
// AFTER:
Text(if (showDetailedExplanation) "▲" else "▼", style = MaterialTheme.typography.bodyLarge)
```

---

### 6. ⚠️ AndroidManifest.xml - Icon References

**File:** `app/src/main/AndroidManifest.xml`

```xml
<!-- BEFORE (Lines 10-12): -->
<application
    ...
    android:icon="@mipmap/ic_launcher"
    android:roundIcon="@mipmap/ic_launcher_round"
    ...>

<!-- AFTER: -->
<application
    ...
    android:label="@string/app_name"
    ...>
<!-- REMOVED: android:icon and android:roundIcon lines -->
```

---

## 📝 Icon Emoji Mapping Reference

| Icon | Emoji | Usage |
|------|-------|-------|
| ArrowBack | ← | Back button |
| HistoryEdu | 📖 | Review |
| VolumeUp | 🔊 | Volume on |
| VolumeOff | 🔇 | Volume off |
| ErrorOutline | ⚠️ | Error |
| Lightbulb | 💡 | Hint |
| Close | ✕ | Close dialog |
| MenuBook | 📚 | Grammar |
| ExpandMore | ▼ | Expand |
| Stop | ■ | Stop recording |
| Mic | 🎤 | Microphone |
| School | 🎓 | Learning |
| Star | ⭐ | Rating/Difficulty |

---

## 🚀 Build Commands

After making these changes:

```bash
# Sync Gradle
./gradlew clean

# Compile
./gradlew compileDebugKotlin

# Build APK
./gradlew assembleDebug

# Install
./gradlew installDebug
```

---

## ✅ Quick Verification Checklist

- [x] gradle.properties has memory settings
- [x] material-icons-extended removed from build.gradle.kts
- [x] ChatScreen.kt icons replaced
- [x] VoiceButton.kt icons replaced
- [ ] HintDialog.kt icons need replacement
- [ ] DifficultyIndicator.kt icons need replacement
- [ ] GrammarBottomSheet.kt icons need replacement
- [ ] AndroidManifest.xml icon references need removal
- [ ] Build test

---

## 🔥 Priority Order

If you need to fix files incrementally, do them in this order:

1. ✅ gradle.properties (DONE)
2. ✅ build.gradle.kts (DONE)
3. ✅ ChatScreen.kt (DONE)
4. ✅ VoiceButton.kt (DONE)
5. HintDialog.kt (NEEDED)
6. GrammarBottomSheet.kt (NEEDED)
7. DifficultyIndicator.kt (NEEDED)
8. AndroidManifest.xml (NEEDED)

---

## 💡 Quick Fix Script

You can use find and replace in your IDE:

**Find:** `Icon(\s*imageVector = Icons\.`
**Replace:** `Text("` + [appropriate emoji]

But be careful to match each icon type correctly!

---

## ⚡ Emergency Minimal Build

If you just want to get it compiling ASAP:

1. ✅ Done: gradle.properties memory settings
2. ✅ Done: Remove material-icons-extended
3. ✅ Done: Fix ChatScreen.kt and VoiceButton.kt
4. Comment out remaining @Composable functions that use Icons temporarily

```kotlin
// Temporarily disable these screens to test core functionality:
// @Composable
// fun HintDialog(...) { /* ... */ }
```

---

## 📚 Files Status

| File | Status | Priority |
|------|--------|----------|
| gradle.properties | ✅ FIXED | Critical |
| build.gradle.kts | ✅ FIXED | Critical |
| ChatScreen.kt | ✅ FIXED | High |
| VoiceButton.kt | ✅ FIXED | High |
| HintDialog.kt | 🔄 NEEDS FIX | Medium |
| GrammarBottomSheet.kt | 🔄 NEEDS FIX | Medium |
| DifficultyIndicator.kt | 🔄 NEEDS FIX | Medium |
| AndroidManifest.xml | 🔄 NEEDS FIX | Low |

---

## 🎯 Expected Result

After all fixes:
- Build memory errors eliminated
- No missing icon resource errors
- App compiles successfully
- All UI elements show emoji instead of vector icons
- Smaller APK size (no icon library)

---

## 🆘 If Build Still Fails

1. Check for any remaining `import androidx.compose.material.icons` statements
2. Search for remaining `Icon(` usages: `grep -r "Icon(" app/src/`
3. Verify no `Icons.` references remain: `grep -r "Icons\." app/src/`
4. Clean build: `./gradlew clean`
5. Invalidate caches in Android Studio

---

## ✨ Summary

**Completed:**
- ✅ Memory settings configured (4GB heap)
- ✅ Material Icons Extended removed
- ✅ ChatScreen.kt fully converted to emoji
- ✅ VoiceButton.kt fully converted to emoji

**Remaining:**
- 🔄 3 more UI files need icon replacement
- 🔄 AndroidManifest.xml needs icon removal

**Estimated Time to Complete:** 10-15 minutes for remaining files
