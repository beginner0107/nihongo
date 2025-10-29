# Claude Code CLI 효율적 사용 가이드

## 🚀 효율적인 작업 전략

### 1. 작업 시작 전 준비

#### 프로젝트 구조 생성
```bash
# 한 번에 전체 구조 생성 요청
claude-code "Create Android project structure for 일본어 회화 앱:
- Package: com.nihongo.conversation
- Architecture: MVVM + Clean
- Create all necessary directories and base files
- Add gradle dependencies in one go"
```

#### 컨텍스트 프리로딩
```bash
# 세션 시작 시 컨텍스트 로드
claude-code "Load context from CLAUDE.md, current phase: [PHASE_NUMBER]"
```

### 2. 명령어 최적화

#### ❌ 비효율적인 방식
```bash
claude-code "Create a User class"
claude-code "Add id field to User"
claude-code "Add name field to User"
claude-code "Add level field to User"
```

#### ✅ 효율적인 방식
```bash
claude-code "Create User data class with:
- id: String
- name: String  
- level: Level enum
- createdAt: Long
Include Room @Entity annotations"
```

### 3. 배치 작업 패턴

#### 여러 파일 한 번에 생성
```bash
claude-code "Create domain models:
1. User.kt - user entity with Room
2. Message.kt - message with sender/content/timestamp
3. Conversation.kt - conversation container
4. Scenario.kt - learning scenario
Place in domain/model/, include all necessary annotations"
```

#### 관련 기능 묶어서 구현
```bash
claude-code "Implement complete chat feature:
1. ChatScreen composable
2. ChatViewModel with StateFlow
3. ChatRepository interface and implementation
4. Message sending/receiving logic
Follow MVVM pattern"
```

### 4. 컨텍스트 절약 기법

#### 참조 사용
```bash
# 첫 요청
claude-code "Create ChatViewModel with message sending. Mark as REF_CHAT_VM"

# 이후 요청
claude-code "Add voice input to REF_CHAT_VM, only show changes"
```

#### 차등 업데이트
```bash
claude-code "In ChatScreen.kt:
- Line 45-50: Replace with voice button
- Line 72: Add animation
Show only modified parts with 2 lines context"
```

---

## 🛡️ 예외 핸들링 전략

### 1. 컴파일 에러 처리

```bash
# 에러 발생 시
claude-code "
Build failed with error:
[ERROR_MESSAGE]
File: [FILE_NAME]
Line: [LINE_NUMBER]
Fix only this specific error, show minimal change"
```

#### 자동 에러 수집 스크립트
```bash
#!/bin/bash
# .claude/handle_errors.sh

# 빌드 에러 캡처
./gradlew build 2>&1 | tee build_output.txt

if [ $? -ne 0 ]; then
    ERROR=$(grep -E "error:|ERROR" build_output.txt | head -5)
    echo "Build failed. Sending to Claude..."
    
    claude-code "Fix build errors:
    $ERROR
    Show only fixes, no explanation"
fi
```

### 2. 런타임 에러 처리

```kotlin
// 글로벌 에러 핸들러
class GlobalErrorHandler {
    fun setup() {
        Thread.setDefaultUncaughtExceptionHandler { _, exception ->
            logError(exception)
            saveErrorContext(exception)
            // Claude에게 전달할 컨텍스트 생성
            createClaudeContext(exception)
        }
    }
    
    private fun createClaudeContext(exception: Throwable): String {
        return """
        Runtime error in 일본어 회화 앱:
        Exception: ${exception.message}
        Stack trace (top 5):
        ${exception.stackTrace.take(5).joinToString("\n")}
        
        Fix with minimal changes
        """.trimIndent()
    }
}
```

### 3. API 에러 처리

```kotlin
// Retrofit 에러 인터셉터
class ErrorInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        try {
            val response = chain.proceed(chain.request())
            if (!response.isSuccessful) {
                handleApiError(response)
            }
            return response
        } catch (e: Exception) {
            handleNetworkError(e)
            throw e
        }
    }
    
    private fun handleApiError(response: Response) {
        when (response.code) {
            429 -> handleRateLimitError()
            401 -> handleAuthError()
            else -> logGenericError(response)
        }
    }
}
```

### 4. 상태 복구 전략

```bash
# 상태 손실 시 복구
claude-code "
Session lost context. Restore from:
- Last checkpoint: [CHECKPOINT_ID]
- Current files modified: [FILE_LIST]
- Last successful feature: [FEATURE_NAME]
Continue from this point"
```

---

## 📋 일반적인 문제 해결

### 1. Out of Context (컨텍스트 초과)

```bash
# 증상: Claude가 이전 작업을 기억 못함

# 해결책 1: 컨텍스트 압축
claude-code "Summarize current state:
- Completed: [LIST]
- Current: [CURRENT_WORK]
- Next: [NEXT_TASK]"

# 해결책 2: 새 세션 시작
claude-code "New session for 일본어 회화 앱.
Load compressed context from .claude/compressed_context.md
Continue [SPECIFIC_TASK]"
```

### 2. Hallucination (잘못된 코드 생성)

```bash
# 예방책
claude-code "
Generate [FEATURE] following these constraints:
- Use only Android SDK 24+ APIs
- Stick to existing project structure
- Use already imported dependencies
- Reference: [SPECIFIC_DOCS_OR_CODE]"

# 검증
claude-code "Verify this code:
[GENERATED_CODE]
Check for:
- Import correctness
- API compatibility
- Type safety"
```

### 3. 반복적인 실패

```bash
# 다른 접근법 요청
claude-code "
Previous approach failed 3 times.
Problem: [PROBLEM]
Tried: [ATTEMPTS]
Suggest alternative implementation approach"
```

### 4. 성능 문제

```bash
# 성능 분석 요청
claude-code "
ChatScreen is laggy.
Analyze and optimize:
- Recomposition frequency
- State management
- List performance
Show specific optimizations only"
```

---

## 🔄 워크플로우 자동화

### 1. 작업 체인 스크립트
```bash
#!/bin/bash
# .claude/workflow.sh

# Phase 1 작업 자동화
tasks=(
    "Create project structure"
    "Setup dependencies"
    "Create domain models"
    "Setup Room database"
    "Create repositories"
    "Build basic UI"
    "Connect API"
)

for task in "${tasks[@]}"; do
    echo "Executing: $task"
    claude-code "$task for 일본어 회화 앱"
    
    # 빌드 확인
    if ./gradlew build; then
        echo "✓ $task completed"
        echo "$task" >> .claude/completed_tasks.txt
    else
        echo "✗ $task failed, fixing..."
        ./claude/handle_errors.sh
    fi
    
    # 토큰 절약을 위한 쿨다운
    sleep 2
done
```

### 2. 지능형 재시도
```bash
#!/bin/bash
# .claude/smart_retry.sh

retry_with_context() {
    local attempt=1
    local max_attempts=3
    local task=$1
    
    while [ $attempt -le $max_attempts ]; do
        echo "Attempt $attempt for: $task"
        
        if [ $attempt -eq 1 ]; then
            claude-code "$task"
        elif [ $attempt -eq 2 ]; then
            # Sonnet으로 전환
            claude-code --model sonnet "$task (simpler approach)"
        else
            # 더 구체적인 지시
            claude-code --model opus "$task
            Previous attempts failed.
            Use most basic implementation.
            Avoid advanced features."
        fi
        
        if ./gradlew build; then
            echo "Success on attempt $attempt"
            return 0
        fi
        
        ((attempt++))
    done
    
    echo "Failed after $max_attempts attempts"
    return 1
}
```

### 3. 컨텍스트 보존 래퍼
```python
#!/usr/bin/env python3
# .claude/context_wrapper.py

import json
import subprocess
import datetime

class ClaudeContextManager:
    def __init__(self):
        self.context_file = ".claude/context.json"
        self.load_context()
    
    def load_context(self):
        try:
            with open(self.context_file, 'r') as f:
                self.context = json.load(f)
        except:
            self.context = {
                "session_start": str(datetime.datetime.now()),
                "completed_tasks": [],
                "current_phase": 1,
                "token_usage": 0
            }
    
    def save_context(self):
        with open(self.context_file, 'w') as f:
            json.dump(self.context, f, indent=2)
    
    def execute_task(self, task, model="auto"):
        # 컨텍스트 포함한 명령 생성
        context_prompt = f"""
        Continue 일본어 회화 앱 development.
        Phase: {self.context['current_phase']}
        Completed: {', '.join(self.context['completed_tasks'][-5:])}
        Task: {task}
        """
        
        # 모델 자동 선택
        if model == "auto":
            model = self.select_model(task)
        
        # 실행
        result = subprocess.run(
            ["claude-code", "--model", model, context_prompt],
            capture_output=True,
            text=True
        )
        
        if result.returncode == 0:
            self.context['completed_tasks'].append(task)
            self.save_context()
        
        return result
    
    def select_model(self, task):
        # 간단한 휴리스틱 기반 모델 선택
        simple_keywords = ['ui', 'test', 'fix', 'update', 'add']
        complex_keywords = ['architect', 'design', 'optimize', 'refactor']
        
        task_lower = task.lower()
        
        if any(keyword in task_lower for keyword in complex_keywords):
            return "opus"
        elif any(keyword in task_lower for keyword in simple_keywords):
            return "sonnet"
        else:
            # 토큰 사용량 기반 결정
            if self.context['token_usage'] > 50000:
                return "sonnet"
            else:
                return "opus"

# 사용 예
if __name__ == "__main__":
    manager = ClaudeContextManager()
    manager.execute_task("Create chat UI with voice button")
```

---

## 💡 프로 팁

1. **명확한 경계 설정**
   ```
   "Create ONLY the ChatScreen composable, no navigation, no viewmodel"
   ```

2. **예제 제공**
   ```
   "Follow this pattern: [CODE_EXAMPLE]"
   ```

3. **제약사항 명시**
   ```
   "Must work offline, max 100 lines, no external dependencies"
   ```

4. **검증 요청**
   ```
   "After generating, verify Compose preview works"
   ```

5. **점진적 구축**
   ```
   "Start with minimal working version, we'll add features later"
   ```

---

## 🎯 체크리스트

작업 시작 전:
- [ ] CLAUDE.md 파일 준비
- [ ] 프로젝트 구조 명확화
- [ ] 의존성 목록 준비
- [ ] 에러 핸들링 스크립트 설정

작업 중:
- [ ] 배치 작업 우선
- [ ] 컨텍스트 압축 주기적 실행
- [ ] 체크포인트 저장
- [ ] 토큰 사용량 모니터링

작업 후:
- [ ] 생성된 코드 테스트
- [ ] 컨텍스트 정리
- [ ] 다음 세션을 위한 요약 작성
- [ ] 학습된 패턴 문서화