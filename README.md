# AITool

原生 Android 与 iOS 的最小可运行工程。

当前只包含应用入口和占位首页，不预设分层、业务组件、路由、网络层或依赖注入。后续先确定 Android 的组件化架构，再让 iOS 按相同的组件边界对齐。

## 工程目录

- `android/`：Kotlin + Jetpack Compose
- `ios/`：Swift + SwiftUI

## Android

使用 Android Studio 打开 `android/`，同步 Gradle 后运行 `app`。

命令行验证：

```bash
cd android
./gradlew assembleDebug
```

## iOS

使用 Xcode 打开 `ios/AITool.xcodeproj`，选择模拟器后运行 `AITool`。

命令行验证：

```bash
cd ios
xcodebuild -project AITool.xcodeproj \
  -scheme AITool \
  -sdk iphonesimulator \
  -configuration Debug \
  CODE_SIGNING_ALLOWED=NO build
```

## 临时标识

- 应用名：`AITool`
- Android applicationId：`com.example.aitool`
- iOS Bundle Identifier：`com.example.aitool`

正式名称和包标识确定后可统一替换。

