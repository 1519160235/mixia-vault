# 密匣 · 本地加密密码库

一个**完全离线**的 Android 密码管理器。没有网络权限，没有账号体系，没有云同步——数据只存在你的手机里。

- 最低系统：Android 8.0（API 26），目标 API 37（Android 17）
- 包名：`com.mixia.vault`
- 当前版本：**1.2.0**（versionCode 7）

> **下载 APK**：打开仓库的 Actions 页面 → 选最新一次成功的 `Build APK` → 页面底部 **Artifacts** 下载 `mixia-apk.zip`，解压即得 APK。

---

## 功能

### 密码 / 账号管理
- 默认分类：网站、应用、Wi-Fi、邮箱、银行卡、服务器、其他；可自由增删改名、调整顺序
- 字段：标题、账号、密码、网址、标签、备注、收藏
- 照片附件：密码截图、安全码、二维码等，随记录一起加密
- 首页搜索（标题/账号/网址/标签/备注）、分类筛选、多种排序
- 密码强度与熵值提示
- 密码生成器：长度 8–48，可选大小写/数字/符号，可排除易混淆字符 `1lI0O`
- 一键复制，复制后 45 秒自动清空剪贴板（可关闭）

### 助记词保险箱（BIP39）
- 生成 12/24 词标准 BIP39 助记词（`SecureRandom` 熵源）
- 导入时校验词表、词数、SHA-256 校验和
- 支持 BIP39 附加密码（第 25 个词）
- 本地派生 ETH（EIP-55）/ BTC（Legacy P2PKH、Bech32）地址、私钥 Hex 与 WIF
- 内置路径：`m/44'/60'/0'/0/0`、`m/44'/0'/0'/0/0`、`m/84'/0'/0'/0/0`
- 助记词默认打码，复制前二次确认

### 安全设计
| 项目 | 实现 |
|---|---|
| 主密码派生 | PBKDF2-HMAC-SHA256，250,000 次，16 字节随机盐 |
| 数据加密 | AES-256-GCM，每次保存使用新随机 IV，头部作为 AAD |
| 照片加密 | 同一库密钥 AES-256-GCM 单独加密，存于 `filesDir/photos` |
| 主密码存储 | **不保存**，任何形式都不保存 |
| 截图/录屏 | 所有界面 `FLAG_SECURE` 屏蔽 |
| 自动锁定 | 离开应用可选立即/30 秒/1 分钟/5 分钟 |
| 暴力尝试 | 连续输错 5 次锁定 60 秒 |
| 指纹解锁 | 可选，Android Keystore 生物识别绑定密钥 |
| 网络 | 清单里**没有 INTERNET 权限**，系统层面无法联网 |
| 第三方依赖 | 无 |

---

## 目录结构

```
app/src/main/
  AndroidManifest.xml
  java/com/mixia/vault/
    core/    AES-GCM、PBKDF2、BIP39、BIP32、secp256k1、Base58、Bech32、Keccak、RIPEMD160
    model/   Entry / Wallet / Vault（分类与排序）
    data/    VaultStore（保险库）、PhotoStore（加密照片）
    util/    AppDialog、AppMenu、剪贴板、密码生成、指纹
    *.java   界面：解锁、列表、编辑、助记词、设置
  res/       深色主题、矢量图标、布局
  assets/    bip39-english.txt
tools/       纯 JVM 测试程序
```

---

## 本地构建

```bash
./gradlew assembleDebug      # 调试包
./gradlew assembleRelease    # 发布包（需本地 keystore，见下）
```

需要 JDK 17 与 Android SDK（platform 34、build-tools 34.0.0）。

### 签名
仓库**不包含**签名密钥（公开仓库，安全考虑）。本地构建时把 keystore 放到 `keystore/mixia-release.jks`，Gradle 会自动使用；也可用环境变量覆盖：

```
SIGNING_KEY_FILE / SIGNING_STORE_PASSWORD / SIGNING_KEY_ALIAS / SIGNING_KEY_PASSWORD
```

GitHub Actions 中若配置了 `SIGNING_KEY_BASE64`（keystore 的 base64）、`SIGNING_STORE_PASSWORD`、`SIGNING_KEY_ALIAS`、`SIGNING_KEY_PASSWORD` 四个 Secret，CI 产出的 release 包会使用同一把钥匙，可直接覆盖安装；未配置则退回 debug 签名。

---

## 更新与发版

日常改代码：

```bash
git add -A
git commit -m "说明这次改了什么"
git push
```

推送后 GitHub Actions 自动编译，到 Actions 页面下载最新 APK。

正式发版（自动创建 Release 并附 APK）：

```bash
git tag v1.2
git push origin v1.2
```

版本号在 `app/src/main/AndroidManifest.xml`（`versionCode` / `versionName`）与 `app/build.gradle` 中同步修改。

---

## 测试

```bash
# 密码学（22 项）：RIPEMD160 / Keccak / PBKDF2 / BIP39 / BIP32 / ETH / BTC / AES-GCM
javac -encoding UTF-8 -d out app/src/main/java/com/mixia/vault/core/*.java tools/TestVectors.java
java -cp out TestVectors app/src/main/assets/bip39-english.txt

# 数据模型（17 + 22 项）：JSON 往返、排序、筛选、分类、照片、旧数据兼容
```

覆盖的已知答案：

- BIP39 `abandon … about` + `TREZOR` 的标准种子
- ETH `0x9858EfFD232B4033E47d90003D41EC34EcaEda94`
- Hardhat 助记词 → `0xf39Fd6e51aad88F6F4ce6aB8827279cffFb92266`
- BTC Legacy `1LqBGSKuX5yYUonjxT5qGfpUsXKYYWeabA`
- BTC Bech32 `bc1qcr8te4kr609gcawutmrza0j4xv80jy8z306fyu`

---

## 免责声明

完整条款见 [DISCLAIMER.md](DISCLAIMER.md)。首次启动时也会展示并需要你确认。核心几条：

- **主密码无法找回**，忘记即数据永久无法解密。
- **助记词等于钱包所有权**，泄露或丢失后果自负。
- **剪贴板是系统共享通道**：复制密码时输入法可读取并可能留存历史，本应用无法删除其他应用的数据。
- 不联网 ≠ 绝对安全，无法防御已 root 设备、恶意系统组件、屏幕录制或物理接触。
- 本应用按「现状」提供，不附带任何担保，不适合存放无法承受丢失风险的资产。

## 重要提醒

- 主密码**无法找回**，忘记即数据永久无法解密。
- 助记词等于钱包资产所有权，请抄纸离线保存，不要截图或存网盘。
- 换机前用「设置 → 导出加密备份」导出 `.mxvault`（不含照片）。
- 本项目为个人自用，使用前请自行评估风险。

## 许可证

MIT
