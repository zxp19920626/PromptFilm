# PromptFilm

PromptFilm 是一个单仓库工程，当前包含 Java 后端、Android 空项目与 iOS 空项目。

## 目录

```text
backend-java/                 Spring Boot API
android-app/                  单 app 模块的 Android 空项目
ios-app/                      SwiftUI iOS 空项目
design/                       产品与客户端设计稿
docs/                         架构说明
```

## 本地验收

后端需要 JDK 11 或更高版本：

```bash
cd backend-java
mvn test
```

Android 需要 JDK 17 与 Android SDK：

```bash
cd android-app
./gradlew testDebugUnitTest assembleDebug
```

iOS 需要 Xcode：

```bash
xcodebuild -project ios-app/PromptFilm.xcodeproj \
  -scheme PromptFilm \
  -sdk iphonesimulator \
  -configuration Debug \
  CODE_SIGNING_ALLOWED=NO build
```

数据库连接、令牌密钥等运行配置只从环境变量读取，仓库不保存真实凭据。
