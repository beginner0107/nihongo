#!/bin/bash
# .claude/session_manager.sh
# 세션 관리 및 컨텍스트 유지 스크립트

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

SESSION_FILE=".claude/current_session.md"
CHECKPOINT_DIR=".claude/checkpoints"
CONTEXT_FILE=".claude/context_state.json"

# 세션 시작
start_session() {
    echo -e "${GREEN}Starting new session...${NC}"
    mkdir -p .claude/checkpoints
    
    cat > $SESSION_FILE << EOF
# Session Started: $(date)
## Project: 일본어 회화 학습 앱
## Current Phase: ${1:-"Not specified"}

### Context Summary
\`\`\`
Project: Android Japanese Conversation App
Stack: Kotlin, Jetpack Compose, Room, Gemini API
Architecture: MVVM + Clean Architecture
\`\`\`

### Completed Tasks
- [ ] Project setup
- [ ] Dependencies configured

### Current Focus
${2:-"Initial setup"}

### Session Commands
- \`session status\` - 현재 상태 확인
- \`session save\` - 체크포인트 저장
- \`session restore [id]\` - 체크포인트 복원
EOF
    
    echo -e "${GREEN}Session initialized!${NC}"
}

# 세션 상태 저장
save_checkpoint() {
    CHECKPOINT_ID=$(date +%Y%m%d_%H%M%S)
    CHECKPOINT_FILE="$CHECKPOINT_DIR/checkpoint_$CHECKPOINT_ID.md"
    
    echo -e "${YELLOW}Saving checkpoint $CHECKPOINT_ID...${NC}"
    
    # 현재 상태 저장
    cat > $CHECKPOINT_FILE << EOF
# Checkpoint: $CHECKPOINT_ID
## Timestamp: $(date)

### Current State
$(cat $SESSION_FILE)

### Files Modified
$(git status --short 2>/dev/null || echo "Git not initialized")

### Context Variables
\`\`\`json
{
  "phase": "${CURRENT_PHASE:-1}",
  "last_command": "${LAST_COMMAND}",
  "tokens_used_estimate": "${TOKENS_USED:-0}",
  "model": "${MODEL:-sonnet}"
}
\`\`\`

### Next Steps
${NEXT_STEPS:-"Continue implementation"}
EOF
    
    echo -e "${GREEN}Checkpoint saved: $CHECKPOINT_ID${NC}"
    echo $CHECKPOINT_ID
}

# 체크포인트 복원
restore_checkpoint() {
    if [ -z "$1" ]; then
        echo -e "${RED}Please specify checkpoint ID${NC}"
        ls $CHECKPOINT_DIR 2>/dev/null | grep checkpoint_
        return 1
    fi
    
    CHECKPOINT_FILE="$CHECKPOINT_DIR/checkpoint_$1.md"
    
    if [ -f "$CHECKPOINT_FILE" ]; then
        echo -e "${YELLOW}Restoring checkpoint $1...${NC}"
        cp $CHECKPOINT_FILE $SESSION_FILE
        echo -e "${GREEN}Checkpoint restored!${NC}"
        
        # 컨텍스트 요약 출력
        echo -e "\n${YELLOW}Context Summary:${NC}"
        grep -A 5 "### Current State" $CHECKPOINT_FILE
    else
        echo -e "${RED}Checkpoint not found: $1${NC}"
        return 1
    fi
}

# 토큰 사용량 추적
track_tokens() {
    TOKENS_FILE=".claude/token_usage.log"
    echo "$(date)|$1|$2" >> $TOKENS_FILE
    
    # 일일 합계 계산
    TODAY=$(date +%Y-%m-%d)
    TODAY_USAGE=$(grep $TODAY $TOKENS_FILE | awk -F'|' '{sum+=$3} END {print sum}')
    
    echo -e "${YELLOW}Today's token usage: $TODAY_USAGE${NC}"
    
    if [ "$TODAY_USAGE" -gt 90000 ]; then
        echo -e "${RED}Warning: Approaching daily token limit!${NC}"
        echo -e "${YELLOW}Consider switching to Sonnet for simple tasks${NC}"
    fi
}

# 스마트 모델 선택
select_model() {
    TASK_TYPE=$1
    
    case $TASK_TYPE in
        "simple"|"ui"|"test")
            echo "sonnet"
            ;;
        "complex"|"architecture"|"design")
            echo "opus"
            ;;
        "review"|"fix"|"refactor")
            echo "sonnet"
            ;;
        *)
            # 토큰 사용량에 따라 자동 선택
            if [ "$TODAY_USAGE" -gt 50000 ]; then
                echo "sonnet"
            else
                echo "opus"
            fi
            ;;
    esac
}

# 컨텍스트 압축
compress_context() {
    echo -e "${YELLOW}Compressing context...${NC}"
    
    # 현재 컨텍스트를 요약
    cat > .claude/compressed_context.md << EOF
# Compressed Context - $(date)

## Project State
- Phase: ${1:-"Development"}
- Completed: Core setup, API integration, Basic UI
- Current: ${2:-"Feature implementation"}

## Key Decisions
- Architecture: MVVM + Clean
- API: Gemini 2.5 Flash
- UI: Jetpack Compose

## Active Files
$(find app/src -name "*.kt" -mtime -1 -type f | head -10)

## Last Commands
$(history | tail -5)

## Continue with:
"Continue 일본어 회화 앱 development from compressed context"
EOF
    
    echo -e "${GREEN}Context compressed! Use when starting new session${NC}"
}

# 메인 명령어 처리
case "$1" in
    "start")
        start_session "$2" "$3"
        ;;
    "save")
        save_checkpoint
        ;;
    "restore")
        restore_checkpoint "$2"
        ;;
    "status")
        cat $SESSION_FILE
        ;;
    "tokens")
        track_tokens "$2" "$3"
        ;;
    "model")
        select_model "$2"
        ;;
    "compress")
        compress_context "$2" "$3"
        ;;
    *)
        echo "Usage: $0 {start|save|restore|status|tokens|model|compress} [args]"
        exit 1
        ;;
esac