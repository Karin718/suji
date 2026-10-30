# 速记（SuJi）

一款**本地优先**的安卓背诵 / 记忆 App，功能类似「默默背单词」，但不止背单词——任何能拆成「问题 → 答案」的东西都能背：单词、古诗、法条、医学名词、专业课笔记、错题……

核心能力：

- 🧠 **艾宾浩斯遗忘曲线排程**：每张卡片按「5 分钟 → 30 分钟 → 12 小时 → 1 天 → 2 天 → 4 天 → 7 天 → 15 天 → 30 天 → 60 天 → 120 天」逐级推进，答「记得」前进一阶、答「模糊」原地重练（间隔减半）、答「忘记」回到第一阶。曲线可在设置里切换「标准 / 轻松 / 高强度」。
- 📥 **多种导入方式**：手动粘贴（`苹果 ---- apple` 或 `苹果：apple`）、从 `.txt / .csv / .md / .json` 文件导入、以及 **AI 整理**——把原始资料丢进去自动拆成卡片。
- 📚 **背诵本管理**：按主题建背诵本、卡片增删改、搜索、批量导入、进度重置。
- 📊 **统计**：今日复习量、连续打卡、掌握情况、近 7 天柱状图。
- 🔔 **每日提醒**：到点自动发通知。
- 🔄 **检查更新**（v1.0.1 新增）：设置里填好 GitHub 仓库地址，一键检查并跳转下载新版本。
- 💾 **备份**：一键导出 / 恢复 `.json`（完整保留复习进度），数据全部存在本机。

---

## 1. 界面预览

不装环境也能看界面效果：浏览器打开项目根目录的 [`preview.html`](preview.html)（静态预览，非真实交互）。

---

## 2. 拿到 APK

已经编译好的安装包：

- 本仓库的 **Releases** 页面 → 下载 `suji-vX.Y.Z.apk`（GitHub Actions 每次 push 自动产出）。
- 或本地构建（见下）。

装到手机：把 APK 传到手机 → 点开 → 允许「安装未知来源应用」即可。

---

## 3. 发布到 GitHub（推荐，以后自动出包 + 检查更新）

用 **GitHub Desktop**（图形界面，不需要命令行）：

1. 安装 [GitHub Desktop](https://desktop.github.com/) 并登录。
2. `File → Add local repository` → 选择本目录（`suji-android`）。
3. 会提示「没有仓库」，点 `create a repository`，起个名字（如 `suji`），选公开或私有 → `Create`。
4. 左下角填一句提交说明（如「速记 v1.0.1」）→ 点 `Commit to main`。
5. 点 `Publish repository` 推送到 GitHub。
6. 等 2~5 分钟，GitHub 的 **Actions** 会自动编译并发布 Release（`.github/workflows/build.yml` 已配好）。

以后每次改代码：改完 → GitHub Desktop 里看到改动 → 填说明 → `Commit` → `Push origin`，GitHub 自动出新的 APK 和 Release。

### 配合「检查更新」

App 内：**设置 → 检查更新**，仓库地址填：

```
https://github.com/你的用户名/仓库名
```

点「检查更新」即可。发现新版本会弹窗，点「去下载」跳到浏览器下载 APK（同一签名覆盖安装，数据不丢）。

> 注意：GitHub 在国内可能时快时慢，若检查更新走不通，直接上 GitHub 网页手动下载 APK 也行。

---

## 4. 本地构建

> 本项目自带共享签名 keystore（`keystore/suji-release.jks`），本机与 GitHub 打包出的 APK **签名一致**，可互相覆盖升级。

### 方式 A：命令行（已配好一键脚本）

Windows 下双击运行根目录的 `build-apk.ps1`（或 PowerShell 执行），产物 `速记-release.apk`。需要本机有 JDK 17、Android SDK（compileSdk 34）和 Gradle 8.7。

```bash
# 等价的手动命令（Windows 用 gradlew.bat）
./gradlew assembleRelease        # macOS / Linux
gradlew.bat assembleRelease      # Windows
```

### 方式 B：Android Studio

`File → Open` 打开本目录，等 Sync 完成后 `Build → Build APK(s)`。

---

## 5. 配置 AI 整理

**设置 → AI 整理**，填一个 OpenAI 兼容接口（DeepSeek 为例）：

| 字段 | 填什么 |
| --- | --- |
| 接口地址 | `https://api.deepseek.com/v1` |
| 模型 | `deepseek-chat` |
| API Key | 你的 Key |

兼容 OpenAI、通义、Kimi、本地 Ollama。Key 只存本机，不填也能用其它功能。

---

## 6. 导入格式

```
abandon ---- v. 放弃；抛弃
benevolent：adj. 仁慈的

《静夜思》下一句
低头思故乡
```

支持 `---- / —— / ：/ : / Tab / |` 分隔，或「一段一张（首行正面、余行背面）」；`.csv` / `.json` 文件也支持，自动识别 UTF-8/GBK。

---

## 7. 项目结构

```
app/src/main/java/com/suji/app/
├── SujiApp.java             # Application 入口
├── data/                    # Room：Deck / Card / ReviewLog / DAO / Repo / Prefs
├── core/Scheduler.java      # 艾宾浩斯调度核心
├── ai/                      # OpenAI 兼容客户端 + 提示词 + 解析
├── util/                    # 文本解析 / 备份 / 文件 / 提醒 / 检查更新(Updater)
└── ui/                      # 各界面 Activity + 适配器 + 柱状图
```

技术栈：Java 17 + AndroidX（AppCompat / Material 3 / Room / WorkManager），minSdk 24（Android 7.0+），targetSdk 34。

---

## 8. 签名说明

- 签名文件：`keystore/suji-release.jks`（别名 `suji`，密码 `suji123456`），已提交到仓库。
- 目的：保证本机与 GitHub Actions 打出的包**签名一致**，能互相覆盖升级、保留数据。
- ⚠️ 安全提示：密钥在仓库里意味着**拿到仓库的人都能冒充这个 App 签名**。个人自用没问题；若要公开分发或上架，请另建一个私密 keystore 并替换 `app/build.gradle` 里的配置。

---

## 9. 常见问题

- **换手机迁移数据**：旧机「设置 → 数据 → 导出备份」，新机「从备份恢复」。
- **升级后数据还在吗**：在。只要包名 `com.suji.app` 和签名不变，覆盖安装不丢数据。
- **删除示例数据**：首页长按背诵本删除，或设置里「清空所有数据」。
