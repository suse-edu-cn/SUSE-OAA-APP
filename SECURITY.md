# Security Policy (安全策略)

**Project OAA (青蟹)** 高度重视用户数据的安全与隐私。本项目作为一个连接学生与学校教务系统的客户端，在处理敏感信息（如学号、密码、成绩）时，遵循最小权限原则和本地化存储原则。

## 🛡️ 安全架构与措施 (Security Architecture & Measures)

我们在代码实现层面采取了以下措施来保障安全性：

### 1. 网络通信安全 (Network Security)

* **全链路 HTTPS**: 所有与应用后端 (`api.suseoaa.com`) 及学校教务系统 (`jwgl.suse.edu.cn`) 的通信均强制使用 HTTPS 协议，防止中间人攻击 (MITM)。
* **RSA 加密传输**: 针对教务系统的登录请求，我们严格遵循学校系统的加密标准。用户的密码在本地使用从学校服务器获取的公钥进行 **RSA 加密** (`RSA/ECB/PKCS1Padding`) 后才进行传输，确保密码在网络传输过程中不以明文形式暴露。
* **CSRF 防护**: 在模拟登录教务系统时，应用会自动获取并携带 `csrftoken`，不仅为了通过服务器验证，也遵循了标准的 Web 安全规范。

### 2. 身份认证与会话管理 (Authentication & Session Management)

* **JWT 认证机制**: 对于协会内部服务，采用 JSON Web Token (JWT) 进行无状态认证。Token 仅存储在受保护的 `DataStore` 中，并通过 `AuthInterceptor` 自动添加到请求头 (`Authorization: Bearer ...`)。
* **会话隔离**: 针对教务系统的 `Cookie` 使用内存级 `SchoolCookieJar` 进行管理。Cookie 仅存在于应用运行期间的内存中，且通过 Hilt 单例模式严格控制其生命周期，防止会话泄露。

### 3. 数据存储安全 (Data Storage Security)

* **Token 安全存储**: 应用的认证 Token 使用 Android Jetpack `DataStore` 存储，这是 Google 推荐的替代 SharedPreferences 的更安全的数据存储方案。
* **本地化数据**: 课表、成绩等敏感数据存储在本地 SQLite 数据库 (`Room Database`) 中。**郑重声明：** 用户的教务系统密码仅存储在用户自己的手机设备本地数据库中，用于自动登录和会话续期，**绝不会** 上传至青蟹的服务器或任何第三方服务器。
* *注意：由于需要支持“自动重登”和“多账号快速切换”功能，密码目前以明文形式存储在应用私有目录的数据库中。在未 root 的设备上，其他应用无法访问该数据。建议用户设置手机锁屏密码以增强安全性。*



## 🔍 支持的版本 (Supported Versions)

目前我们仅对最新版本的代码提供安全更新和支持。

| Version | Supported |
| --- | --- |
| Latest | :white_check_mark: |
| < 1.0 | :x: |

## 🐛 漏洞上报 (Reporting a Vulnerability)

如果您发现了本项目的安全漏洞，请**不要**在 GitHub Issue 中公开提交。请按照以下步骤进行负责任的披露：

1. 发送邮件至 **[安全团队邮箱:huangzhuoruihzr@gmail.com]**。
2. 在邮件主题中注明 `[Security Vulnerability]`。
3. 请详细描述漏洞的复现步骤、受影响的模块以及潜在的危害。
4. 我们将在收到报告后的 48 小时内确认收到，并在评估后提供修复时间表。

## 📝 内容举报 (Reporting Content)

除了安全漏洞，我们也接受关于仓库内容的举报（如不当言论、侵权内容等）。

如果您发现任何违反[行为准则](CODE_OF_CONDUCT.md)或适用法律的内容，请通过以下方式联系管理员：

*   **邮箱**: huangzhuoruihzr@gmail.com
*   **邮件主题**: `[Content Report] Project OAA`

我们将严肃处理所有的举报，并采取必要的行动（如删除内容、封禁用户等）。

## ⚠️ 免责声明 (Disclaimer)

本项目（Project OAA）是非官方的第三方客户端。

* 项目代码开源，旨在促进技术交流与校园便利。
* 用户需自行承担使用本软件产生的风险。
* 我们承诺不收集用户的教务系统密码，但建议用户定期修改教务系统密码以保障账户安全。

---

**最后更新时间**: 2026-01-05
