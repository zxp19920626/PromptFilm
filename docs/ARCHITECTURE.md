# PromptFilm 基础架构

## 仓库边界

后端与 Android 客户端放在同一仓库中，但分别维护独立构建入口。当前阶段只搭建基础能力、登录模块占位页和支付模块占位页，不实现真实登录、支付、业务数据库表或生产部署。

## Android 依赖方向

Android 使用 Kotlin、XML 布局、OkHttp3、Glide 与 ARouter。模块依赖只能沿以下方向流动：

```text
app
 ├─ feature:login   ─┐
 └─ feature:payment ─┼─> core:router -> core:common
                     ├─> core:network -> core:common
                     └─> core:ui -> core:common
```

- `app` 只负责应用初始化、模块装配和首页入口。
- `feature:*` 之间禁止建立 Gradle 依赖，也禁止直接引用对方的 Activity 或实现类。
- 跨业务模块跳转统一使用 `core:router` 暴露的路由地址和导航入口。
- `core:network` 集中创建 OkHttpClient，业务模块不得各自维护网络客户端。
- `core:ui` 集中维护主题、基础 Activity、通用状态视图和 Glide 图片加载扩展。
- 后续新增业务使用新的 `feature:<name>` 模块，不复制基础能力。

ARouter `1.5.2` 仍依赖 KAPT 与旧 support 元数据，因此当前 AGP 9 构建显式启用旧 Kotlin DSL 和 Jetifier 兼容开关。该约束集中记录在 `android-app/gradle.properties`；后续替换路由实现或升级到支持 KSP/AndroidX 的版本时，应同步移除这些兼容开关并回归全部路由。

## 后端技术栈

后端沿用参考项目 `main_java` 分支的技术组合：Spring Boot、Spring Web、Validation、Spring Data JPA、MySQL、Flyway、Actuator 与 OpenAPI。基础代码按 `common`、`health`、`auth`、`payment` 包拆分；后续业务继续按领域包组织，页面或客户端不得依赖持久化实现细节。

## 配置边界

- 数据库地址、账号、密码与令牌密钥通过环境变量注入。
- Flyway 禁止执行 clean，JPA 只校验结构，不自动改表。
- 当前基础框架不包含生产编排、生产域名或真实第三方支付配置。
