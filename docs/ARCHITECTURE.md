# PromptFilm 项目结构

## 仓库边界

后端、Android 与 iOS 客户端放在同一仓库中，并分别维护独立构建入口。当前客户端仅保留可运行的空项目，不实现登录、支付、网络层或业务模块。

## Android

Android 当前仅包含 `app` 模块、一个启动 Activity 和一张 XML 占位页面。首个真实业务进入开发前，再根据已经确认的业务边界补充分层和依赖。

## iOS

iOS 当前使用 SwiftUI，仅包含应用入口、一个占位页面和基础资源目录。工程使用 Xcode 自动生成 Info.plist，不包含签名团队、第三方 SDK 或生产配置。

## 后端技术栈

后端沿用参考项目 `main_java` 分支的技术组合：Spring Boot、Spring Web、Validation、Spring Data JPA、MySQL、Flyway、Actuator 与 OpenAPI。基础代码按 `common`、`health`、`auth`、`payment` 包拆分；后续业务继续按领域包组织，页面或客户端不得依赖持久化实现细节。

## 配置边界

- 数据库地址、账号、密码与令牌密钥通过环境变量注入。
- Flyway 禁止执行 clean，JPA 只校验结构，不自动改表。
- 当前基础框架不包含生产编排、生产域名或真实第三方支付配置。
