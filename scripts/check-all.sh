#!/bin/bash
# autoGLM-voice-app 全面检查脚本
# 用于在提交前检查所有可能的编译问题

set -e

echo "======================================"
echo "  autoGLM-voice-app 全面检查脚本"
echo "======================================"
echo ""

PROJECT_DIR="/root/.openclaw/workspace/autoGLM-voice-app"
cd "$PROJECT_DIR"

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

check_count=0
pass_count=0
fail_count=0

check_pass() {
    echo -e "${GREEN}✓${NC} $1"
    ((pass_count++))
    ((check_count++))
}

check_fail() {
    echo -e "${RED}✗${NC} $1"
    ((fail_count++))
    ((check_count++))
}

check_warn() {
    echo -e "${YELLOW}⚠${NC} $1"
}

echo "1️⃣  检查 Gradle 配置..."
echo "--------------------------------------"

# 检查 Gradle 版本
GRADLE_VERSION=$(grep "GRADLE_VERSION" .github/workflows/android-ci.yml | head -1 | grep -oP "'\K[0-9.]+" || echo "")
if [[ "$GRADLE_VERSION" == "8.2" ]]; then
    check_pass "Gradle 版本：$GRADLE_VERSION ✅"
else
    check_fail "Gradle 版本：$GRADLE_VERSION (应该是 8.2)"
fi

# 检查 Kotlin 版本
KOTLIN_VERSION=$(grep "kotlin" app/build.gradle.kts | grep "version" | head -1 | grep -oP "[0-9.]+" || echo "")
if [[ "$KOTLIN_VERSION" == "1.9.22" ]]; then
    check_pass "Kotlin 版本：$KOTLIN_VERSION ✅"
else
    check_warn "Kotlin 版本：$KOTLIN_VERSION"
fi

# 检查 AGP 版本
AGP_VERSION=$(grep "com.android.application" app/build.gradle.kts | grep -oP "[0-9.]+" || echo "")
if [[ "$AGP_VERSION" == "8.2.0" ]]; then
    check_pass "AGP 版本：$AGP_VERSION ✅"
else
    check_warn "AGP 版本：$AGP_VERSION"
fi

echo ""
echo "2️⃣  检查必要的资源文件..."
echo "--------------------------------------"

# 检查图标
if [[ -f "app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml" ]]; then
    check_pass "ic_launcher.xml 存在 ✅"
else
    check_fail "缺少 ic_launcher.xml"
fi

if [[ -f "app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml" ]]; then
    check_pass "ic_launcher_round.xml 存在 ✅"
else
    check_fail "缺少 ic_launcher_round.xml"
fi

if [[ -f "app/src/main/res/drawable/ic_launcher_foreground.xml" ]]; then
    check_pass "ic_launcher_foreground.xml 存在 ✅"
else
    check_fail "缺少 ic_launcher_foreground.xml"
fi

# 检查必要的 values 文件
if [[ -f "app/src/main/res/values/colors.xml" ]]; then
    check_pass "colors.xml 存在 ✅"
else
    check_fail "缺少 colors.xml"
fi

if [[ -f "app/src/main/res/values/themes.xml" ]]; then
    check_pass "themes.xml 存在 ✅"
else
    check_fail "缺少 themes.xml"
fi

echo ""
echo "3️⃣  检查 Kotlin 代码导入..."
echo "--------------------------------------"

# 检查 VoiceService.kt 的导入
VOICE_SERVICE="app/src/main/java/com/autoglm/voice/VoiceService.kt"

if grep -q "import android.os.Bundle" "$VOICE_SERVICE"; then
    check_pass "VoiceService: Bundle 导入 ✅"
else
    check_fail "VoiceService: 缺少 Bundle 导入"
fi

if grep -q "import okhttp3.MediaType.Companion.toMediaType" "$VOICE_SERVICE"; then
    check_pass "VoiceService: toMediaType 导入 ✅"
else
    check_fail "VoiceService: 缺少 toMediaType 导入"
fi

if grep -q "import java.io.IOException" "$VOICE_SERVICE"; then
    check_pass "VoiceService: IOException 导入 ✅"
else
    check_fail "VoiceService: 缺少 IOException 导入"
fi

# 检查 MainActivity.kt
MAIN_ACTIVITY="app/src/main/java/com/autoglm/voice/MainActivity.kt"

if [[ -f "$MAIN_ACTIVITY" ]]; then
    check_pass "MainActivity.kt 存在 ✅"
else
    check_fail "缺少 MainActivity.kt"
fi

echo ""
echo "4️⃣  检查 AndroidManifest.xml..."
echo "--------------------------------------"

MANIFEST="app/src/main/AndroidManifest.xml"

if grep -q "package=\"com.autoglm.voice\"" "$MANIFEST"; then
    check_pass "包名正确 ✅"
else
    check_fail "包名错误"
fi

if grep -q "android.permission.INTERNET" "$MANIFEST"; then
    check_pass "INTERNET 权限 ✅"
else
    check_fail "缺少 INTERNET 权限"
fi

if grep -q "android.permission.RECORD_AUDIO" "$MANIFEST"; then
    check_pass "RECORD_AUDIO 权限 ✅"
else
    check_fail "缺少 RECORD_AUDIO 权限"
fi

if grep -q "android.permission.SYSTEM_ALERT_WINDOW" "$MANIFEST"; then
    check_pass "SYSTEM_ALERT_WINDOW 权限 ✅"
else
    check_fail "缺少 SYSTEM_ALERT_WINDOW 权限"
fi

echo ""
echo "5️⃣  检查代码常见问题..."
echo "--------------------------------------"

# 检查 LayoutParams 使用
if grep -n "FrameLayout.LayoutParams" "$VOICE_SERVICE" | grep -q "type ="; then
    check_fail "VoiceService: FrameLayout.LayoutParams 不应该有 type 属性！"
else
    check_pass "LayoutParams 使用正确 ✅"
fi

# 检查 OkHttp 使用
if grep -n "MediaType.parse" "$VOICE_SERVICE" | grep -qv "toMediaType"; then
    check_fail "VoiceService: 应该使用 toMediaType() 而不是 MediaType.parse()"
else
    check_pass "OkHttp 使用正确 ✅"
fi

echo ""
echo "6️⃣  检查 gradle.properties..."
echo "--------------------------------------"

if [[ -f "gradle.properties" ]]; then
    check_pass "gradle.properties 存在 ✅"
    
    if grep -q "org.gradle.jvmargs" gradle.properties; then
        check_pass "JVM 参数配置 ✅"
    else
        check_warn "缺少 JVM 参数配置"
    fi
    
    if grep -q "android.useAndroidX=true" gradle.properties; then
        check_pass "AndroidX 启用 ✅"
    else
        check_warn "缺少 AndroidX 配置"
    fi
else
    check_fail "缺少 gradle.properties"
fi

echo ""
echo "======================================"
echo "  检查完成！"
echo "======================================"
echo ""
echo "总计：$check_count 项检查"
echo -e "${GREEN}通过：$pass_count${NC}"
echo -e "${RED}失败：$fail_count${NC}"
echo ""

if [[ $fail_count -gt 0 ]]; then
    echo -e "${RED}❌ 发现 $fail_count 个问题，请修复后再提交！${NC}"
    exit 1
else
    echo -e "${GREEN}✅ 所有检查通过！可以提交代码了！${NC}"
    exit 0
fi
