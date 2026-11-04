# 密匣 · 本地加密密码库

一个**完全离线**的 Android 密码管理器。没有网络权限，没有账号体系，没有云同步——数据只存在你的手机里。

- APK：`dist/MixiaVault-1.1.apk`（已签名，可直接安装）
- 最低系统：Android 8.0（API 26），目标 API 34
- 包名：`com.mixia.vault`，versionCode 2

---

## v1.1 更新

- **首页右上角菜单重做**：改为底部弹出式卡片菜单（带图标、副标题、分隔和取消按钮），不再是系统 PopupMenu。
- **所有弹窗重做**：设置里的「关于 / 加密方式 / 自动锁定 / 修改主密码 / 备份 / 删除确认」等全部换成应用自绘的深色卡片弹窗，不再出现 Android 老式浅色对话框。
- **全面屏适配**：状态栏、刘海/挖孔、手势条统一按 WindowInsets 处理，内容不再顶到屏幕最上沿，也不再被导航条遮挡。
- **照片附件**：可给任意记录添加一张或多张照片（密码截图、恢复码、二维码）。照片用同一个库密钥 AES-256-GCM 单独加密存放，可点开全屏查看、可单独删除。
- **自定义分类**：分类名称可新增/重命名/删除，顺序可上下调整；顺序会同步影响首页标签栏和「按分类」排序。
- **排序**：首页新增排序按钮，支持「最近更新 / 最近创建 / 标题 A→Z / 按分类」。

---

## 功能

### 1. 密码 / 账号管理
- 默认分类：网站、应用、Wi-Fi、邮箱、银行卡、服务器、其他；可自由增删改名、调整顺序
- 字段：标题、账号、密码、网址、标签、备注、收藏
- 照片附件：密码截图、安全码、二维码等，随记录一起加密
- 首页搜索（标题/账号/网址/标签/备注）、分类筛选、多种排序
- 密码强度与熵值提示
- 密码生成器：长度 8–48，可选大小写/数字/符号，可排除易混淆字符 `1lI0O`
- 一键复制（账号、密码、网址等），复制后 45 秒自动清空剪贴板（可关闭）

### 2. 助记词保险箱（BIP39）
- 生成 12 词或 24 词标准 BIP39 助记词（熵来自 `SecureRandom`）
- 导入已有助记词时做完整校验（词表、词数、SHA-256 校验和）
- 支持 BIP39 附加密码（第 25 个词，passphrase）
- 本地派生并展示：
  - 地址（ETH 走 EIP-55 校验和格式；BTC 支持 Legacy P2PKH 与 Bech32 隔离见证）
  - 私钥 Hex、BTC 的 WIF 私钥
- 路径可编辑，内置 `m/44'/60'/0'/0/0`、`m/44'/0'/0'/0/0`、`m/84'/0'/0'/0/0`
- 助记词默认打码，需手动点“显示”；复制前会二次确认并警告

### 3. 安全设计
| 项目 | 实现 |
|---|---|
| 主密码派生 | PBKDF2-HMAC-SHA256，250,000 次，16 字节随机盐 |
| 数据加密 | AES-256-GCM，每次保存使用新的随机 IV，头部作为 AAD |
| 照片加密 | 同一库密钥 AES-256-GCM 单独加密，存于 `filesDir/photos` |
| 落盘内容 | `vault.dat` + 加密照片文件，无明文 |
| 主密码存储 | **不保存**，任何形式都不保存 |
| 截图/录屏 | 所有界面 `FLAG_SECURE` 屏蔽 |
| 自动锁定 | 离开应用可选立即/30 秒/1 分钟/5 分钟 |
| 暴力尝试 | 连续输错 5 次锁定 60 秒 |
| 指纹解锁 | 可选；用 Android Keystore 中“必须生物识别才能使用”的密钥包裹库密钥 |
| 备份 | 导出的 `.mxvault` 仍是 AES-256 加密文件（照片不在备份内） |
| 网络 | 清单里**没有 INTERNET 权限**，系统层面无法联网 |
| 第三方依赖 | 无。没有广告/统计/网络 SDK |

> 说明：`USE_BIOMETRIC` / `USE_FINGERPRINT` 仅用于可选的指纹解锁；不开启指纹时不会用到。

---

## 安装

1. 把 `dist/MixiaVault-1.1.apk` 传到手机（数据线、微信文件传输助手等均可）。
2. 点击安装，系统提示“未知来源”时允许一次。
3. 首次打开 → 设置主密码（**至少 10 位**）。

从 1.0 升级可直接覆盖安装（同一个签名证书），数据保留。

### 重要警告
- 主密码**无法找回**。忘记主密码 = 数据永久无法解密。请务必记牢，可设置一个只有你看得懂的提示。
- 助记词等于钱包资产所有权。请抄写在纸上离线保存，**不要截图、不要存网盘、不要发聊天软件**。
- 换手机/卸载前，先在「设置 → 导出加密备份」导出 `.mxvault` 文件；重装后用「从备份恢复」导入（需要当时的主密码）。
- 备份文件**不包含照片**，只包含文字记录与助记词；重要照片请另行保管。

---

## 从源码构建

### 方式 A：Android Studio
用 Android Studio 打开本目录（Gradle 工程已配好，AGP 8.5.2，无第三方依赖）：

```bash
./gradlew assembleRelease
```

签名配置位于 `app/build.gradle`，使用仓库内 `keystore/mixia-release.jks`：

- storePassword / keyPassword：`mixia2026`
- keyAlias：`mixia`

正式发布请替换为你自己的 keystore，并妥善保管。

### 方式 B：命令行手工构建（生产此 APK 的方式）
只需 JDK 17 + Android SDK（platform 34、build-tools 34.0.0）：

```powershell
$sdk = ".toolchain\android-sdk"
$jdk = ".toolchain\jdk\jdk-17.0.21+9"
$src = "app\src\main"

& "$sdk\build-tools\34.0.0\aapt2.exe" compile --dir "$src\res" -o ".build\res.zip"
& "$sdk\build-tools\34.0.0\aapt2.exe" link -o ".build\app-unsigned.apk" `
    -I "$sdk\platforms\android-34\android.jar" --manifest "$src\AndroidManifest.xml" `
    --java ".build\gen" -A "$src\assets" --min-sdk-version 26 --target-sdk-version 34 `
    ".build\res.zip" --auto-add-overlay

& "$jdk\bin\javac.exe" -encoding UTF-8 --release 8 -classpath "$sdk\platforms\android-34\android.jar" `
    -d ".build\classes" (Get-ChildItem "$src\java",".build\gen" -Recurse -Filter *.java).FullName
& "$jdk\bin\jar.exe" cf ".build\classes.jar" -C ".build\classes" .
& "$sdk\build-tools\34.0.0\d8.bat" --lib "$sdk\platforms\android-34\android.jar" --min-api 26 `
    --output ".build\dex" ".build\classes.jar"
# 把 .build\dex\classes.dex 写入 APK 根目录，然后：
& "$sdk\build-tools\34.0.0\zipalign.exe" -f -p 4 ".build\with-dex.apk" ".build\aligned.apk"
& "$sdk\build-tools\34.0.0\apksigner.bat" sign --ks keystore\mixia-release.jks `
    --ks-pass pass:mixia2026 --key-pass pass:mixia2026 `
    --out dist\MixiaVault-1.1.apk ".build\aligned.apk"
```

> 版本号写在 `app/src/main/AndroidManifest.xml` 的 `versionCode` / `versionName`。

---

## 测试

```powershell
# 密码学（22 项）
java -cp .build\test TestVectors app\src\main\assets\bip39-english.txt

# 数据模型：JSON、排序、筛选、搜索、密码生成（17 项）
java -cp ".build\modeltest;.toolchain\json.jar" ModelTest

# 分类 / 排序 / 照片 / 旧数据兼容（22 项）
java -cp ".build\modeltest2;.toolchain\json.jar" ModelTest2
```

当前结果：**22 / 17 / 22 全部通过**。

密码学部分覆盖的已知答案：

- BIP39 `abandon … about` + passphrase `TREZOR` 的标准种子
- ETH 地址 `0x9858EfFD232B4033E47d90003D41EC34EcaEda94`
- Hardhat 默认助记词 → `0xf39Fd6e51aad88F6F4ce6aB8827279cffFb92266`
- BTC Legacy `1LqBGSKuX5yYUonjxT5qGfpUsXKYYWeabA`
- BTC 隔离见证 `bc1qcr8te4kr609gcawutmrza0j4xv80jy8z306fyu`

模型测试还验证了：v1.0 旧数据（无 `categories` / `photos` 字段）能正常读取并自动补全分类。

---

## 目录结构

```
app/src/main/
  AndroidManifest.xml
  java/com/mixia/vault/
    core/     加密、BIP39、BIP32、secp256k1、Base58、Bech32、Keccak、RIPEMD160
    model/    Entry / Wallet / Vault（分类与排序）
    data/     VaultStore（保险库文件）、PhotoStore（加密照片）
    util/     剪贴板、密码生成、指纹、AppDialog（自绘弹窗）、AppMenu（底部菜单）
    *.java    界面：解锁、列表、编辑、助记词、设置
  res/        深色主题、矢量图标、布局（含全面屏 insets）
  assets/     bip39-english.txt（2048 词）
tools/        测试程序
dist/         已签名 APK
keystore/     自签名证书
```

---

## 已知限制

- 只有 Android 版，没有桌面端/浏览器插件。
- 不支持自动填充（Autofill）、不支持多设备同步（设计上就不联网）。
- 备份是整库覆盖式恢复，不做条目级合并；照片不参与备份。
- 指纹解锁需 Android 11+（API 30）且设备有可用生物识别。
- 未在真机上做自动化 UI 测试（构建环境无模拟器）；密码学、数据层、分类排序逻辑均已用测试验证。

## 许可证

个人自用项目，随你修改使用。使用前请自行评估风险，重要资产请务必离线备份助记词。
