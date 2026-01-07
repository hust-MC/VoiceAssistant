# 百度语音SDK集成指南

## 📋 目录
1. [当前状态](#当前状态)
2. [申请百度API](#申请百度api)
3. [下载SDK](#下载sdk)
4. [集成步骤](#集成步骤)
5. [测试验证](#测试验证)
6. [常见问题](#常见问题)

---

## 当前状态

项目已完成语音SDK的架构搭建：

| 模块 | 文件 | 状态 |
|------|------|------|
| 配置类 | `SpeechConfig.kt` | ✅ 已创建 |
| 语音识别管理器 | `SpeechRecognizerManager.kt` | ✅ 已创建 |
| 语音合成管理器 | `TTSManager.kt` | ✅ 已创建 |
| 模拟管理器 | `MockSpeechManager.kt` | ✅ 已创建 |
| 统一管理器 | `VoiceAssistantManager.kt` | ✅ 已创建 |

**当前运行模式：模拟模式**
- 使用Android系统TTS进行语音播报
- 模拟语音识别流程（点击快捷按钮测试）

---

## 申请百度API

### 步骤1：注册百度AI开放平台

1. 访问：https://ai.baidu.com/
2. 点击右上角"登录/注册"
3. 完成账号注册和实名认证

### 步骤2：创建应用

1. 登录后进入控制台
2. 选择"语音技术"
3. 点击"创建应用"
4. 填写应用信息：
   - 应用名称：VoiceAssistant
   - 接口选择：勾选"语音识别"和"语音合成"
   - 应用类型：Android
5. 创建完成后，获取：
   - **App ID**
   - **API Key**
   - **Secret Key**

---

## 下载SDK

### 方式1：官网下载（推荐）

1. 访问：https://ai.baidu.com/sdk#asr
2. 下载"语音识别 Android SDK"
3. 下载"语音合成 Android SDK"

### 下载后的文件结构

```
下载的SDK/
├── 语音识别SDK/
│   ├── core/
│   │   └── bdasr_V*.aar
│   └── doc/
│       └── API文档
├── 语音合成SDK/
│   ├── core/
│   │   └── bdtts_V*.aar
│   └── doc/
│       └── API文档
```

---

## 集成步骤

### 步骤1：复制SDK文件

将下载的aar文件复制到项目的`app/libs`目录：

```bash
# 在项目根目录执行
cp 下载目录/bdasr_V*.aar app/libs/
cp 下载目录/bdtts_V*.aar app/libs/
```

复制后的目录结构：
```
app/libs/
├── bdasr_V3_xxxxx.aar    # 语音识别SDK
└── bdtts_V3_xxxxx.aar    # 语音合成SDK
```

### 步骤2：配置API密钥

编辑 `app/src/main/java/com/max/voiceassistant/speech/SpeechConfig.kt`：

```kotlin
object SpeechConfig {
    // 替换为你的百度API配置
    const val APP_ID = "你的App ID"
    const val API_KEY = "你的API Key"  
    const val SECRET_KEY = "你的Secret Key"
    
    // ... 其他配置
}
```

### 步骤3：关闭模拟模式

编辑 `app/src/main/java/com/max/voiceassistant/speech/VoiceAssistantManager.kt`：

```kotlin
companion object {
    // 改为 false 使用真实SDK
    const val USE_MOCK_MODE = false
}
```

### 步骤4：Sync项目

在Android Studio中：
1. 点击 "File" -> "Sync Project with Gradle Files"
2. 或点击工具栏的 "Sync Now"

### 步骤5：运行测试

1. 连接Android设备或启动模拟器
2. 运行应用
3. 点击麦克风按钮开始语音识别
4. 说出指令（如"播放音乐"、"打开空调"）

---

## 测试验证

### 模拟模式测试

在集成真实SDK之前，可以使用模拟模式测试：

1. 运行应用
2. 点击麦克风按钮
3. 点击快捷命令按钮（播放音乐、打开空调等）
4. 观察对话列表和状态变化

### 真实模式测试

集成SDK后：

1. 确保设备已联网
2. 授予录音权限
3. 点击麦克风按钮
4. 对着设备说话
5. 观察识别结果和语音反馈

### 测试用例

| 说法 | 预期结果 |
|------|---------|
| "播放音乐" | 媒体播放 |
| "暂停" | 媒体暂停 |
| "下一首" | 切换下一首 |
| "音量调大" | 音量增加 |
| "打开空调" | 空调开启 |
| "温度调高" | 温度+1度 |
| "温度26度" | 设置为26度 |
| "现在几点" | 播报当前时间 |
| "今天星期几" | 播报星期 |

---

## 常见问题

### Q1: 提示"语音功能未初始化"

**原因**：SDK初始化失败
**解决**：
1. 检查API配置是否正确
2. 检查网络连接
3. 检查aar文件是否正确放置在libs目录

### Q2: 识别结果为空

**原因**：可能是网络问题或授权问题
**解决**：
1. 确保设备已联网
2. 检查API Key是否有效
3. 检查是否已授予录音权限

### Q3: TTS没有声音

**原因**：
1. 设备音量为0
2. TTS初始化失败

**解决**：
1. 调高设备音量
2. 检查TTS初始化日志

### Q4: 编译报错找不到类

**原因**：SDK未正确引入
**解决**：
1. 确保aar文件在libs目录
2. 执行Gradle Sync
3. 检查build.gradle中的fileTree配置

### Q5: 真机运行崩溃

**原因**：ABI不兼容
**解决**：
在build.gradle.kts中确认NDK配置：
```kotlin
ndk {
    abiFilters += listOf("armeabi-v7a", "arm64-v8a")
}
```

---

## 代码架构说明

```
speech/
├── SpeechConfig.kt           # 配置常量
├── SpeechRecognizerManager.kt # 语音识别（百度SDK封装）
├── TTSManager.kt             # 语音合成（百度SDK封装）
├── MockSpeechManager.kt      # 模拟模式（测试用）
└── VoiceAssistantManager.kt  # 统一管理器（自动切换模式）
```

### 工作流程

```
用户点击麦克风
    ↓
VoiceAssistantManager.startListening()
    ↓
[模拟模式]                    [真实模式]
MockSpeechManager            SpeechRecognizerManager
    ↓                              ↓
模拟识别流程                   百度SDK识别
    ↓                              ↓
回调 onResult(text)           回调 onResult(text)
    ↓                              ↓
MainViewModel.processUserInput(text)
    ↓
IntentParser.parse() → CommandExecutor.execute()
    ↓
VoiceAssistantManager.speak(response)
    ↓
[模拟模式]                    [真实模式]
系统TTS播报                   百度TTS播报
```

---

## 参考资源

- [百度语音识别文档](https://ai.baidu.com/ai-doc/SPEECH/Ek39uxgre)
- [百度语音合成文档](https://ai.baidu.com/ai-doc/SPEECH/Gk38y8lzk)
- [SDK下载页面](https://ai.baidu.com/sdk#asr)

---

## 更新日志

- **v1.0** - 初始版本，完成SDK架构搭建
  - 支持模拟模式和真实模式切换
  - 集成语音识别和语音合成
  - 完整的回调机制



