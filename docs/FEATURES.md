# Features

Complete feature overview of NihonGo Conversation.

## AI Conversation System

### Natural Dialogue with Gemini 2.5 Flash
- Context-aware conversations that remember previous messages
- Natural Japanese responses optimized for learners
- Streaming responses for low latency (~800ms TTFB)
- Batch requests for grammar + hints + translation (61% faster)

### Real-Time Grammar Feedback
- Automatic grammar error detection
- Natural expression suggestions for direct translations
- Conversation flow analysis
- Politeness level (keigo) recommendations
- Pattern tracking for personalized weakness analysis
- 6 feedback types: grammar errors, unnatural expressions, better alternatives, flow, politeness level

### 126+ Learning Scenarios

#### Entertainment (27 scenarios)
K-POP, J-POP, anime, drama discussions

#### Work (14 scenarios)
Office communication, emails, meetings, code reviews

#### Daily Life (15 scenarios)
Trash disposal, internet setup, package delivery

#### Travel (13 scenarios)
- Airport immigration
- Subway/train navigation
- Tourist spots
- Ramen shops, izakaya
- Onsen ryokan
- Souvenir shopping
- Taxi service

#### Technology (9 scenarios)
Code review, incident response, technical discussions

#### JLPT Practice (5 scenarios)
- N5: Self-introduction, です/ます forms
- N4: て-form, た-form, past/future
- N3: Conditionals, passive/causative
- N2: Advanced keigo, business expressions
- N1: Highest-level keigo, idioms, academic discussions

#### E-Sports (5 scenarios)
League of Legends, LCK viewing parties

#### Business (4 scenarios)
Job interviews, customer complaint handling, presentations

#### Romance (2 scenarios)
Asking someone out, girlfriend conversations

#### Other (Culture, Health, Finance, Housing, Emergency)
Hip-hop culture, hospital visits, US stocks, real estate contracts, earthquake evacuation

### Custom Scenarios
- Add personalized scenarios
- Delete custom scenarios
- Custom badge display
- Fully customizable instructions

## Voice Features

### Voice-Only Mode
- Text completely hidden during conversation
- Real-time visual indicators (listening, thinking, speaking)
- Conversation timer with progress tracking (default 5 min)
- Full transcript review after session
- 5 voice states: IDLE, LISTENING, PROCESSING, SPEAKING, THINKING

### Advanced Pronunciation Analysis

#### Pitch Accent Analysis (高低アクセント)
- Mora-by-mora pitch pattern (H/L)
- 4 accent type classification: 平板, 頭高, 中高, 尾高
- Pitch curve visualization
- Native pattern comparison

#### Intonation Pattern Analysis (イントネーション)
- Sentence type recognition (declarative, question, exclamation, command)
- Rising/falling detection at sentence endings
- Sentence-level pitch curves
- Context-appropriate improvement suggestions

#### Speed & Rhythm Analysis
- Speaking rate evaluation (too slow ~ too fast)
- Mora timing consistency measurement
- DTW comparison with native speakers
- Naturalness score (0-100)

#### Problematic Sound Detection
- 10 common pronunciation issues:
  - ら-row sounds
  - つ/ちゅ distinction
  - Long vowels (長音)
  - Geminate consonants (促音)
  - ん sound variations
  - は/を particle pronunciation
  - が/んが distinction
  - し/ち distinction
  - つ/す distinction
  - ふ pronunciation
- Severity levels: critical, high, medium, low
- Specific improvement suggestions with native examples
- Minimal pairs practice

#### 6-Dimensional Pronunciation Scoring
- Accuracy
- Pitch
- Intonation
- Rhythm
- Clarity
- Naturalness

#### Grading System
6 levels from Beginner to Native

### Speech Recognition & Synthesis
- **TTS**: Japanese voice with speed control (0.5x - 2.0x)
- **STT**: Real-time speech recognition
- Auto-play AI responses
- Pronunciation accuracy scoring (0-100)
- Levenshtein distance analysis

## Translation System

### 3-Provider Automatic Fallback
1. **Cache First**: Permanent cache with unlimited reuse
2. **Microsoft Translator** (Primary): 2M chars/month free, Korea Central region, 200-400ms
3. **DeepL** (Fallback): 500K chars/month free, high accuracy, 300-600ms
4. **ML Kit** (Offline): Unlimited, on-device, 100-200ms

### Features
- Automatic quota tracking
- Provider selection based on availability
- Cached translations (<10ms, ~95% hit rate)
- Fully offline capable with ML Kit
- Per-message translation toggle
- Translation source indicator

## Sentence Card System

### Whole Sentences from Real Conversations
- Complete sentences instead of isolated words
- Context information: conversation ID, scenario title, previous messages
- Grammar pattern extraction: 〜てください, 〜ことができる, 10+ patterns
- Native pronunciation audio for each sentence

### 4 Practice Modes
1. **Reading Mode**: See sentence, recall meaning
2. **Listening Mode**: Hear audio, type what you heard
3. **Fill-in-the-Blank**: Practice particles, verbs, patterns
4. **Speaking Mode**: See translation, speak in Japanese

### Fill-in-the-Blank Generator
- Automatic particle blanks (は, が, を, に, で)
- Verb detection and blanking
- Grammar pattern blanks
- Hints and incorrect answer choices

### SM-2 Spaced Repetition Algorithm
- Optimal review scheduling
- Per-mode completion tracking
- Pattern-based statistics
- Strength/weakness analysis by grammar pattern

## Learning Management

### Conversation Management
- End conversation button
- Auto-save to history
- Start new conversation
- Resume previous conversations
- Search and filter all conversations

### Review Mode
- Show only completed conversations
- Learning statistics
- Extract important phrases
- TTS playback

### Learning Statistics
- Daily/weekly/monthly progress
- Learning streak tracking
- Per-scenario progress
- Chart visualization
- 30-day calendar heatmap
- Accuracy trends
- Personal best records

## Message Context Menu

Long-press any message to:
- Copy text
- Play with TTS
- Analyze grammar
- Translate to Korean
- Share with external apps

### Grammar Explanation
- Long-press for instant analysis
- Color-coded syntax highlighting
- Cached for instant reload
- Detailed breakdown of patterns

### Per-Message Translation
- Korean translation button on each AI message
- Selective translation view
- Translation caching

## User & Settings

### Difficulty Adjustment
- JLPT level-based AI responses (N5-N4 / N3-N2 / N1)
- Vocabulary complexity analysis
- Sentence structure adaptation

### User Profiles
- Avatar selection
- Learning goals
- Personalized AI responses
- Multi-user support with DataStore-based session management
- Auto-login
- Data isolation per user

### UI/UX
- Material 3 design system
- Typing indicators
- Smooth animations
- Message timestamps
- Auto-scroll to bottom (smart detection)
- Context menu on all messages

## Furigana Support

### User Messages
- Kuromoji tokenizer-based automatic furigana
- Hiragana/katakana conversion
- Ruby text display above kanji
- Toggle on/off per message

## Offline Support

### Fully Offline Scenarios
1. ML Kit model pre-download (~30MB)
2. 20 common phrases built-in (DataStore)
3. Recent 50 translations cached (memory)

### When Network Unavailable
- Automatic offline mode
- ML Kit translation
- Cached response playback
- Review previous conversations

## Performance Optimizations

### Response Caching
- Common phrases cached
- 99.7% faster re-requests (300ms → 1ms)
- 20 built-in phrases + 50 dynamic cache entries

### Payload Optimization
- Recent 20 messages only
- 2000 char limit per message
- System prompt compressed to 500 chars
- 60% payload reduction (15KB → 6KB)

### Network Optimizations
- GZIP compression (70-90% size reduction)
- Connection pooling (50% latency reduction: 600ms → 300ms)
- Streaming responses for TTFB

### Database Optimizations
- 11 optimized indexes (including composite)
- Database views (conversation_stats)
- Streaming query optimization
- Queries <50ms

## Categories & Organization

### 9 Category Tabs
1. All (전체)
2. Entertainment (엔터)
3. Work (직장)
4. Daily Life (일상)
5. Travel (여행)
6. Technology (기술)
7. E-Sports (게임)
8. JLPT (JLPT)
9. Other (기타)

### Search & Filter
- Real-time scenario search
- Search by title, description, category
- Difficulty filter
- Favorites system
- Custom scenario management

## Data Management

### Conversation History
- Full conversation search/filtering
- Status-based view
- Quick resume
- Deletion management

### Export & Import
(Coming soon)
- Backup conversations
- Export flashcards
- Import custom scenarios

## Accessibility

- Full keyboard navigation support
- Screen reader compatible
- High contrast mode support
- Adjustable text sizes
- Voice-only mode for hands-free learning
