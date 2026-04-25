# Milli Clock Overlay

一个 Android 悬浮毫秒时钟应用。

它会在屏幕上显示一个可拖动的悬浮时间层：

- 日期单独一行
- 时间单独一行
- 毫秒使用红色显示
- 点击悬浮时间可显示右上角关闭按钮
- 支持前台服务常驻显示

## 功能特性

- 读取系统当前时间戳
- 显示格式为 `HH:mm:ss.SSS`
- 悬浮窗支持拖动
- 日期与时间分行显示，减少横向占用
- 毫秒单独高亮，便于录屏、演示和对时
- 使用浅黑半透明背景，尽量减少遮挡

## 技术参数

- Package: `com.codex.milliclock`
- Min SDK: `26`
- Target SDK: `34`
- Compile SDK: `35`
- Language: Java
- Build Tool: Gradle / Android Gradle Plugin

## 权限说明

应用使用以下权限：

- `SYSTEM_ALERT_WINDOW`
- `FOREGROUND_SERVICE`
- `FOREGROUND_SERVICE_SPECIAL_USE`
- `POST_NOTIFICATIONS`

其中悬浮窗权限必须手动授予，否则无法显示悬浮时间。

## 已知限制

- 某些系统设置页、权限页、安全页会主动隐藏第三方悬浮窗，这是 Android / ROM 的系统限制，不是应用崩溃。
- 当前 `release` 构建在没有 keystore 的情况下会生成未签名 APK。

## 本地构建

### Debug

```bash
gradlew.bat assembleDebug
```

输出：

- `app/build/outputs/apk/debug/app-debug.apk`

### Release

```bash
gradlew.bat assembleRelease
```

输出：

- `app/build/outputs/apk/release/app-release-unsigned.apk`

## Release 签名

项目已经支持可选的本地签名配置。

1. 复制 `keystore.properties.example` 为 `keystore.properties`
2. 填入你自己的 keystore 信息
3. 重新执行：

```bash
gradlew.bat assembleRelease
```

示例配置：

```properties
storeFile=release-keystore.jks
storePassword=your_store_password
keyAlias=release
keyPassword=your_key_password
```

说明：

- `keystore.properties` 已被 `.gitignore` 排除，不会提交到仓库
- `local.properties` 也已排除，因为它包含本机 Android SDK 路径

## 项目结构

```text
app/
  src/main/
    java/com/codex/milliclock/
      MainActivity.java
      ClockOverlayService.java
    res/
gradle/
keystore.properties.example
README.md
```

## 当前仓库内容

仓库已包含：

- Android 工程源码
- Gradle Wrapper
- Debug / Release 构建配置
- 可选签名模板

适合继续做这些扩展：

- 更丰富的主题切换
- 自动贴边 / 点击穿透
- 辅助功能版本，适配更多系统页面
- GitHub Releases 自动发布
