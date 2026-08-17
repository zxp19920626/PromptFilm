# PromptFilm

PromptFilm 是一个单仓库工程，当前基础框架包含 Java 后端与 Android 客户端。

## 目录

```text
backend-java/                 Spring Boot API
android-app/app/              Android 应用装配与启动入口
android-app/core/common/      跨模块基础类型与结果模型
android-app/core/network/     OkHttp3 网络能力
android-app/core/router/      ARouter 路由契约与统一导航入口
android-app/core/ui/          XML 页面共用的 UI 基础组件
android-app/feature/login/    登录业务模块
android-app/feature/payment/  支付业务模块
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

数据库连接、令牌密钥等运行配置只从环境变量读取，仓库不保存真实凭据。
