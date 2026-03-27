# 雾岛

[![License](https://img.shields.io/github/license/123Duo3/XDNMB)](https://github.com/123Duo3/XDNMB/blob/main/LICENSE)
![On Development](https://img.shields.io/badge/on-development-yellow)

一个开源的 [匿名版 X 岛](https://www.nmbxd.com) 第三方客户端项目，当前支持 Android、iOS、macOS 和桌面端。

## 下载

您可以在桌面端网页右侧和移动端页面下方的 [Releases 页面](https://github.com/123Duo3/XDNMB/releases) 中找到最新版本和历史版本的下载链接。

## 如何编译

[`/shared`](./shared/src) 用于在各个平台之间共享的业务逻辑。

### Android 端

[`/androidApp`](./androidApp) 用于 Android 应用入口和 Android 平台相关 UI 代码。

要构建 Android 开发版本，可以直接在 IDE 中使用运行配置，或者在终端执行：

- macOS / Linux
  ```shell
  ./gradlew :androidApp:assembleDebug
  ```
- Windows
  ```shell
  .\gradlew.bat :androidApp:assembleDebug
  ```

### 桌面端（JVM）

[`/desktopApp`](./desktopApp) 用于桌面端（JVM）应用入口和桌面平台相关实现。

要运行桌面端开发版本，可以直接在 IDE 中使用运行配置，或者在终端执行：

- macOS / Linux
  ```shell
  ./gradlew :desktopApp:run
  ```
- Windows
  ```shell
  .\gradlew.bat :desktopApp:run
  ```

### Apple 平台（SwiftUI）

[`/iosApp`](./iosApp) 是 SwiftUI 宿主工程，负责 Apple 平台原生入口，可通过 Xcode 构建并运行到 iOS 和 macOS。

要运行 iOS 或 macOS 开发版本，可以先使用 Gradle 完整构建一次项目，然后用 Xcode 打开 [`/iosApp/iosApp.xcodeproj`](./iosApp/iosApp.xcodeproj) 并选择对应目标运行。

您也可以在 Xcode 后台运行时，继续使用安装了 Kotlin Multiplatform 插件的 Android Studio 进行开发、编译与安装。

更多构建方式和常见问题可以参考 Kotlin Multiplatform 官方文档：

* [Kotlin Multiplatform 入门](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html)
* [Kotlin Multiplatform Mobile 环境配置](https://kotlinlang.org/docs/multiplatform-mobile-setup.html)

## 关于

* Copyright (C) 2026 123哆3 & Konyaco.
* 欢迎提交 Pull Request 帮助开发，也欢迎通过 Issue 反馈问题。
* 您可以在 [爱发电](https://afdian.net/a/123duo3) 进行捐赠，以支持开发。

## 特别鸣谢

感谢 [良辰](https://github.com/kevinluo6191) 对开发的大力支持。

