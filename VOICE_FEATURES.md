# Voice Features Implementation (STT/TTS)

## Summary
Successfully integrated Speech-to-Text (STT) and Text-to-Speech (TTS) functionality into the Japanese conversation learning app.

## New Files Created

### 1. VoiceManager.kt (`core/voice/`)
**Purpose**: Central manager for all voice operations
- **STT**: Android SpeechRecognizer with Japanese language support
- **TTS**: Android TextToSpeech with Japanese voice
- **State Management**: StateFlow for UI updates
- **Event System**: Channel-based events for recognition results
- **Lifecycle**: Proper initialization and cleanup

**Key Features**:
- Japanese language recognition (`ja-JP`)
- Japanese text-to-speech
- State tracking (Idle, Listening, Speaking, Error)
- Error handling with user-friendly messages
- Utterance progress tracking

### 2. VoiceButton.kt (`presentation/chat/`)
**Purpose**: Animated voice recording button
- **Visual States**:
  - Normal: Primary color mic icon
  - Listening: Red stop icon with pulsing animation
  - Speaking: Secondary color (disabled)
- **Animations**: Infinite scale animation when listening
- **Accessibility**: Clear visual feedback

### 3. VoiceStateIndicator.kt (`presentation/chat/`)
**Purpose**: Status indicator for voice operations
- Shows "聞いています..." when listening
- Shows "話しています..." when speaking
- Displays error messages
- Auto-hides when idle

## Updated Files

### ChatViewModel.kt
**Changes**:
- Injected `VoiceManager`
- Added `voiceState: StateFlow<VoiceState>`
- Added `autoSpeak: Boolean` to UI state
- Voice event observer for recognition results
- Auto-speak AI responses when enabled
- Methods: `startVoiceRecording()`, `stopVoiceRecording()`, `speakMessage()`, `toggleAutoSpeak()`
- Proper cleanup in `onCleared()`

### ChatScreen.kt
**Changes**:
- Permission handling for RECORD_AUDIO
- Voice state observation
- Auto-speak toggle in TopAppBar
- VoiceStateIndicator display
- VoiceButton in MessageInput
- Tap AI messages to replay them
- Permission launcher for runtime requests

## Features

### 1. Speech-to-Text (STT)
- **Activation**: Press and hold mic button
- **Language**: Japanese (ja-JP)
- **Flow**:
  1. User presses mic button
  2. Permission check
  3. Start listening (pulsing animation)
  4. Speech recognition
  5. Text appears in input field
  6. User can edit or send directly

### 2. Text-to-Speech (TTS)
- **Auto-Speak**: AI responses automatically spoken (toggle-able)
- **Manual Replay**: Tap any AI message bubble to replay
- **Language**: Japanese voice
- **Visual Feedback**: Speaking indicator while active

### 3. Permission Handling
- Runtime permission request for RECORD_AUDIO
- Re-request on denied if user tries to record
- Graceful fallback to text-only mode

### 4. UI/UX Enhancements
- **Mic Button**:
  - Primary color: Ready to record
  - Red + pulsing: Currently recording
  - Secondary color: AI is speaking
- **Status Indicators**:
  - Shows current voice operation
  - Error messages inline
- **Auto-Speak Toggle**:
  - Volume icon in TopAppBar
  - VolumeUp: Auto-speak enabled
  - VolumeOff: Auto-speak disabled

## User Flow

### Recording Voice Input
1. Tap mic button
2. Grant permission (if first time)
3. Speak in Japanese
4. Stop recording (auto or manual)
5. Edit recognized text if needed
6. Send message

### Listening to AI Responses
**Auto Mode** (default):
1. Send message
2. AI responds
3. Response is automatically spoken
4. Continue conversation

**Manual Mode**:
1. Toggle auto-speak off
2. Tap any AI message bubble to replay

## Technical Implementation

### State Management
```kotlin
sealed class VoiceState {
    object Idle
    object Listening
    object Speaking
    data class Error(message: String)
}
```

### Event System
```kotlin
sealed class VoiceEvent {
    data class RecognitionResult(text: String)
    data class SpeakingComplete(utteranceId: String)
    data class Error(message: String)
}
```

### Integration Points
- **VoiceManager**: Singleton, Hilt-injected
- **ChatViewModel**: Observes voice events, controls voice operations
- **ChatScreen**: UI components, permission handling

## Error Handling

### Recognition Errors
- Audio error
- Network issues
- No speech detected
- Timeout
- Insufficient permissions
- All shown as user-friendly Japanese messages

### TTS Errors
- Initialization failure
- Playback errors
- Graceful degradation to text-only

## Performance Considerations
- Lazy TTS initialization
- Proper resource cleanup
- Debouncing not needed (single tap actions)
- Memory-efficient state management

## Testing Checklist

- [ ] Grant microphone permission
- [ ] Record Japanese speech
- [ ] Verify text recognition accuracy
- [ ] Send message via voice
- [ ] Auto-speak AI response
- [ ] Toggle auto-speak off/on
- [ ] Tap AI message to replay
- [ ] Deny permission and retry
- [ ] Test with network issues
- [ ] Test with no speech input
- [ ] Verify cleanup on screen exit

## Known Limitations
- Requires Google services for speech recognition
- Network required for STT (Google Cloud Speech)
- TTS quality depends on device's Japanese voice data
- Background recording not supported

## Next Steps (Future Enhancements)
- [ ] Offline STT support
- [ ] Custom wake word
- [ ] Voice command shortcuts
- [ ] Recording playback before sending
- [ ] Pitch/speed controls for TTS
- [ ] Multiple voice options
- [ ] Conversation transcription export

## Dependencies
All voice features use built-in Android APIs:
- `android.speech.SpeechRecognizer`
- `android.speech.tts.TextToSpeech`
- `android.speech.RecognizerIntent`

No additional library dependencies required!

## Permission Required
```xml
<uses-permission android:name="android.permission.RECORD_AUDIO" />
```

Already added to AndroidManifest.xml

## File Count
- **New Files**: 3
  - VoiceManager.kt
  - VoiceButton.kt
  - (VoiceStateIndicator in VoiceButton.kt)
- **Updated Files**: 2
  - ChatViewModel.kt
  - ChatScreen.kt

## Lines of Code
- VoiceManager: ~200 lines
- VoiceButton: ~120 lines
- ChatViewModel additions: ~50 lines
- ChatScreen additions: ~60 lines
**Total**: ~430 lines of new/modified code

## Architecture Impact
- Clean separation of concerns
- VoiceManager as a reusable singleton
- ViewModel handles voice state
- UI components are stateless
- Follows existing Clean Architecture pattern

---

**Status**: ✅ Complete and Ready for Testing

All Phase 2 voice features successfully implemented!
