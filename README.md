# 📱 autoGLM-voice-app

> AutoGLM 语音助手 - 独立 Android 应用版本
> 无需 Auto.js，直接安装使用

## 📥 下载

### 预编译 APK（推荐）
[下载最新版 APK](https://github.com/z-ph/autoGLM-voice-app/releases/latest)

### 自行编译
见下方"编译指南"

---

## ✨ 功能

- 🎤 语音识别
- 🧠 AutoGLM AI 控制
- 📱 手机自动化
- 🔘 悬浮按钮
- ⚙️ 可配置 API Key

---

## 🚀 快速开始

### 1. 安装
```
下载 APK → 安装 → 打开应用
```

### 2. 授权
```
开启无障碍服务
允许悬浮窗权限
允许录音权限
```

### 3. 配置
```
设置 → 输入智谱 API Key → 保存
```

### 4. 使用
```
点击悬浮按钮 → 说话 → 自动执行
```

---

## 🔧 编译指南

### 环境要求
- Android Studio Arctic Fox 或更高版本
- JDK 11+
- Android SDK 30+

### 编译步骤

```bash
# 1. 克隆项目
git clone https://github.com/z-ph/autoGLM-voice-app.git
cd autoGLM-voice-app

# 2. 用 Android Studio 打开
# File → Open → 选择项目目录

# 3. 等待 Gradle 同步完成

# 4. 构建 APK
# Build → Build Bundle(s) / APK(s) → Build APK(s)

# 5. APK 输出位置
app/build/outputs/apk/debug/app-debug.apk
```

### 命令行编译

```bash
# Windows
gradlew.bat assembleDebug

# macOS/Linux
./gradlew assembleDebug
```

---

## 📁 项目结构

```
autoGLM-voice-app/
├── app/
│   ├── src/main/
│   │   ├── java/com/autoglm/voice/
│   │   │   ├── MainActivity.kt
│   │   │   ├── VoiceService.kt
│   │   │   ├── AccessibilityService.kt
│   │   │   └── ApiClient.kt
│   │   ├── res/
│   │   └── AndroidManifest.xml
│   └── build.gradle.kts
├── build.gradle.kts
├── settings.gradle.kts
└── README.md
```

---

## 📄 许可证

MIT License

---

## 🙏 致谢

- [智谱 AI](https://open.bigmodel.cn) - AutoGLM API
- [AutoX](https://github.com/kkevsekk1/AutoX) - 灵感来源

---

**让手机听懂你的话！🎤**
