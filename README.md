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

## Wan2.7 最小链路演示

本地 `demo` profile 会关闭数据库、JPA 和 Flyway 自动配置，只验证 Android、本地后端与
阿里云百炼 Wan2.7 的“上传素材 → 创建异步任务 → 查询结果 → 播放视频”链路。API Key
只由服务端环境变量读取。三种能力固定使用以下模型快照：

- 文生视频：`wan2.7-t2v-2026-06-12`
- 图生视频：`wan2.7-i2v-2026-04-25`
- 参考生视频：`wan2.7-r2v-2026-06-12`

先在阿里云百炼新加坡业务空间中取得业务空间 ID 和同地域 API Key。后端默认使用
独立的本机 Docker Compose 栈运行，不依赖本机 MySQL，也不会连接项目之外的容器：

```bash
cd PromptFilm
export WAN_BASE_URL='https://WORKSPACE_ID.ap-southeast-1.maas.aliyuncs.com/api/v1'
export DASHSCOPE_API_KEY='请替换为新加坡地域的API Key'
docker compose --project-name promptfilm-demo -f compose.demo.yml up --build --detach
docker compose --project-name promptfilm-demo -f compose.demo.yml ps
curl --fail 'http://127.0.0.1:18080/health'
```

若本机启用 IVPN 后出现 `WAN_UPSTREAM_ERROR`，可先用下面的零费用探针确认是否为
TLS 握手超时。本机验证时，IVPN 东京与新加坡线路都会在 TLS ClientHello 后超时；
临时暂停后阿里云任务可正常创建和查询，暂停到期会自动恢复：

```bash
docker exec promptfilm-demo-backend-1 \
  curl --max-time 10 --output /dev/null --verbose \
  'https://WORKSPACE_ID.ap-southeast-1.maas.aliyuncs.com/api/v1/tasks/network-probe'
ivpn connection -pause 15
```

API Key 只注入容器进程，不写入 Compose 文件或镜像。完成本地演示后停止该独立栈：

```bash
docker compose --project-name promptfilm-demo -f compose.demo.yml down
```

统一创建接口使用 `multipart/form-data`。本地 Demo 通过 `X-DashScope-DataInspection`
请求头使用业务方确认的数据检查策略；该值由 `WAN_DATA_INSPECTION` 注入。新开一个终端
创建 2 秒、720P、竖屏文生视频任务：

```bash
curl 'http://127.0.0.1:18080/api/demo/video-tasks' \
  -F 'mode=TEXT' \
  -F 'prompt=一位演员站在演唱会舞台中央，在暖色追光下自然回头看向镜头' \
  -F 'resolution=720P' \
  -F 'ratio=9:16' \
  -F 'duration=2' \
  -F 'promptExtend=true' \
  -F 'watermark=true'
```

图生视频使用 `IMAGE` 并上传恰好一张首帧图片。输出比例由首帧决定：

```bash
curl 'http://127.0.0.1:18080/api/demo/video-tasks' \
  -F 'mode=IMAGE' \
  -F 'prompt=人物缓慢回头看向镜头' \
  -F 'images=@/绝对路径/first-frame.png;type=image/png'
```

参考生视频使用 `REFERENCE` 并按提示词中的“图1、图2”顺序上传一至五张图片：

```bash
curl 'http://127.0.0.1:18080/api/demo/video-tasks' \
  -F 'mode=REFERENCE' \
  -F 'prompt=图1的人物站在图2的舞台上自然演唱' \
  -F 'images=@/绝对路径/person.jpg;type=image/jpeg' \
  -F 'images=@/绝对路径/stage.png;type=image/png'
```

响应中的 `data.output.task_id` 用于查询任务，创建成功后只需轮询该任务，不要重复创建：

```bash
curl 'http://127.0.0.1:18080/api/demo/video-tasks/TASK_ID'
```

当 `data.output.task_status` 为 `SUCCEEDED` 时，`data.output.video_url` 是生成结果地址。
`PENDING` 或 `RUNNING` 表示任务仍在处理。若返回 `WAN_UPSTREAM_ERROR`，可根据
`data.upstreamStatus` 区分鉴权、参数或上游服务错误；服务不会把上游响应正文和凭据返回给客户端。

Android 模拟器中的演示页面默认访问 `http://10.0.2.2:18080`，其中 `10.0.2.2`
指向开发机。后端启动后构建并安装 Android debug 包：

```bash
cd android-app
ANDROID_HOME="$ANDROID_SDK_PATH" ./gradlew testDebugUnitTest assembleDebug
ANDROID_HOME="$ANDROID_SDK_PATH" "$ANDROID_SDK_PATH/platform-tools/adb" \
  install -r app/build/outputs/apk/debug/app-debug.apk
```

打开 PromptFilm，选择“文生视频”“图生视频”或“参考生视频”。后两种模式通过 Android
系统文档选择器读取图片；客户端自动每 3 秒查询一次，任务成功后通过 `TextureView` 直接
加载并循环播放生成视频。创建任务前可选择 720P/1080P、输出比例、2 至 15 秒时长，
并分别控制智能改写和水印；图生视频的输出比例跟随首帧图片。Android 应用只访问本地后端，
不包含百炼 API Key。
