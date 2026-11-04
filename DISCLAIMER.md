# 免责声明 / Disclaimer

最后更新：2026-11-04

## 中文

**密匣（MiXia Vault）** 是一个个人自用的离线密码管理工具，按「现状」（AS IS）提供，不附带任何明示或暗示的担保，包括但不限于适销性、特定用途适用性和不侵权的担保。

### 1. 数据丢失风险
- **主密码无法找回。** 应用不保存、不上传、也无法重置主密码。一旦忘记主密码，保险库中的数据将**永久无法解密**，开发者无法提供任何恢复手段。
- **助记词与私钥的保管责任完全由使用者承担。** 助记词等同于加密货币钱包的全部所有权。助记词泄露、抄写错误、丢失，均可能导致资产永久损失，开发者不承担任何责任。
- 卸载应用、清除应用数据、更换设备、设备损坏或丢失，均可能导致数据丢失。请自行、定期导出加密备份并妥善离线保存。
- 备份文件本身仍是加密的，恢复时需要当时的主密码；**备份不含照片附件**。

### 2. 安全边界
- 本应用**未申请 INTERNET 权限**，从系统层面无法联网。但这不等于设备本身安全：已 root 的设备、恶意系统组件、屏幕录制、无障碍服务、输入法、键盘记录器等，均可能在你不知情的情况下获取信息。
- **剪贴板与输入法风险（重要）。** 复制密码时内容会进入系统剪贴板；剪贴板是系统级共享通道，当前获得焦点的输入法（微信输入法、搜狗输入法、Gboard 等）可以合法读取，并可能自动保存进它自己的「剪贴板历史」。这些数据属于输入法的私有存储，**本应用没有任何权限删除或访问**。
  本应用能做的仅限：将复制内容标记为敏感（Android 13+ 系统浮层不预览）、密码复制后最多 15 秒自动清空系统剪贴板、提供「清除系统剪贴板」按钮。
  如在意此风险，请 ① 在输入法设置中关闭「剪贴板历史 / 云同步」；② 尽量用「查看密码」后手动输入，而非复制。
- 本应用无法防御已被攻破的操作系统、物理接触攻击、或用户自身的社工风险。
- 指纹解锁依赖 Android Keystore 与设备生物识别实现，其安全性取决于设备厂商的实现质量。

### 3. 使用范围
- 本应用面向个人日常使用，**不适合**作为企业级密钥管理系统、也不适合管理高价值且无法承受丢失风险的资产。
- 请勿将本应用用于违反所在地法律法规的用途。
- 使用本应用即表示你已理解并接受上述全部风险。如果你不同意，请不要安装或使用。

### 4. 责任限制
在适用法律允许的最大范围内，开发者不对任何直接、间接、附带、特殊或后果性损害承担责任，包括但不限于数据丢失、资产损失、业务中断或利润损失，无论该损害是否源于使用或无法使用本应用。

---

## English

**MiXia Vault** is a personal, offline password manager provided "AS IS", without warranty of any kind, express or implied, including but not limited to the warranties of merchantability, fitness for a particular purpose and noninfringement.

- **The master password cannot be recovered.** It is never stored or transmitted. Forgetting it means the vault is permanently undecryptable.
- **You are solely responsible for your mnemonic phrases and private keys.** A leaked, mistyped or lost mnemonic can cause irreversible loss of crypto assets.
- **Clipboard is a system-wide channel.** Any focused input method can read copied content and may retain it in its own clipboard history. This app cannot access or erase other apps' private data.
- The app requests **no INTERNET permission**, but this does not protect against rooted devices, malicious system components, screen capture, accessibility services or keyloggers.
- In no event shall the authors be liable for any claim, damages or other liability arising from the use of the software.

By installing or using this application you acknowledge and accept these terms.
