# esbridge 模块编译指南

本模块是 ESBridge 的核心库，编译产物为 Android AAR 文件。

## 环境要求

| 工具 | 版本要求 |
|------|----------|
| JDK | 11 或以上 |
| Android Gradle Plugin | 8.12.0 |
| Kotlin | 2.0.21 |
| compileSdk | 36 |
| minSdk | 19 |

建议使用 **Android Studio** 打开项目，它会自动管理 Gradle 和 SDK 版本。

## 编译步骤

在项目根目录（`ESBridge/`）执行以下命令。

### 编译 Debug AAR

```bash
./gradlew :esbridge:assembleDebug
```

### 编译 Release AAR

```bash
./gradlew :esbridge:assembleRelease
```

产物路径：

```
esbridge/build/outputs/aar/esbridge-release.aar
```

### 仅编译（不打包）

```bash
./gradlew :esbridge:compileReleaseKotlin
```

## 发布到本地 Maven 仓库

项目已配置 `maven-publish` 插件，可将 AAR 发布到本机的 `~/.m2` 目录：

```bash
./gradlew :esbridge:publishToMavenLocal
```

发布坐标：

```
name.yangyun:ESBridge:1.0
```

之后可在其他项目中通过以下方式引用：

```kotlin
// settings.gradle.kts
repositories {
    mavenLocal()
}

// build.gradle.kts
implementation("name.yangyun:ESBridge:1.0")
```

## 运行测试

```bash
# 单元测试
./gradlew :esbridge:test

# 设备端测试（需连接 Android 设备或模拟器）
./gradlew :esbridge:connectedAndroidTest
```

## 清理构建产物

```bash
./gradlew :esbridge:clean
```
