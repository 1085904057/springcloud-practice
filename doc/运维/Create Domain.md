# NameCheap 购买一级域名并托管到 Cloudflare 操作手册（含原理详解）

## 一、操作流程（可直接执行）

### （一）在 NameCheap 购买一级域名（以example.com为例）

1. 注册并登录 NameCheap 账户，在主页搜索框输入目标域名（如example.com），确认未被注册后加入购物车。
2. 结算页填写 WHOIS 联系人信息，勾选域名隐私保护（Privacy Protection），选择支付方式完成购买。
3. 购买成功后，进入 "Domain List"，找到该域名并点击右侧 "Manage"，确认域名状态为 "Active"。

### （二）在 Cloudflare 添加域名并获取 NS 地址

1. 登录 Cloudflare 账户，点击 "Add a Site"，输入完整域名（example.com），点击 "Continue"。
2. 选择套餐（推荐先选 "Free" 免费套餐），点击 "Continue"。
3. 自动扫描 DNS 记录，无需修改直接点击 "Continue"（后续可在 Cloudflare 补充）。
4. 记录 Cloudflare 生成的专属 NS 地址（如ada.ns.cloudflare.com、bob.ns.cloudflare.com），点击 "Continue" 进入激活页。

### （三）在 NameCheap 修改域名 NS 为 Cloudflare 地址

1. 回到 NameCheap 域名管理页，找到 "Nameservers" 选项，下拉选择 "Custom DNS"。
2. 删除原有 NS 记录，依次粘贴 Cloudflare 的两条 NS 地址，每行一个，点击绿色对勾保存。
3. 保存后返回 Cloudflare 激活页，点击 "Done, check nameservers" 触发验证。

### （四）验证生效与后续配置

1. 等待 NS 记录全球同步（15 分钟 - 48 小时），可通过 https://www.whatsmydns.net/#NS/ 输入域名查询 NS 生效状态。
2. Cloudflare 显示 "Your site is now on Cloudflare"，即托管成功。
3. 在 Cloudflare "DNS" 页补充解析记录（如 A 记录、CNAME 记录等），并按需开启 SSL/TLS、Auto Minify 等功能。

## 二、核心原理详解

### （一）DNS 分层查询逻辑

- **根域名服务器**：仅存储顶级域名（如.com、.cn）对应的 NS 地址，不记录具体一级域名解析信息。
- **顶级域 NS**：.com 顶级域的 NS 服务器存储所有.com 后缀一级域名的 NS 映射关系。修改example.com的 NS 为 Cloudflare 地址，本质是让 NameCheap 将新映射同步到.com 顶级域的 NS 中。
- **递归查询流程**：本地 DNS 解析example.com时，先向根服务器查.com 的 NS，再访问.com 顶级域 NS 获取example.com对应的 Cloudflare NS，最后从 Cloudflare NS 获取 A/CNAME 等具体记录。

### （二）NS 生效延迟原因

- 顶级域 NS 同步新映射需要时间。
- 全球各地本地 DNS 缓存需更新，TTL（生存时间）到期后才会获取新记录，导致生效时间为 15 分钟 - 48 小时。

### （三）关键补充

- NS 记录至少配置 2 条，实现主备冗余，避免单服务器故障影响解析。
- Cloudflare 的 "橙云" 模式可隐藏源站 IP，提供 CDN 加速与 DDoS 防护；"灰云" 仅做 DNS 解析，不代理流量。

### （四）Cloudflare CNAME 配置限制

**跨账号 CNAME 限制**：Cloudflare 不支持在一个账号下的域名 A 通过 CNAME 指向另一个账号下的域名 B。只有当域名 A 和域名 B 托管在同一个 Cloudflare 账号下时，CNAME 记录才能正常工作。

**实践建议**：
- 如需跨域名 CNAME 配置，确保相关域名托管在同一 Cloudflare 账号下。
- 若必须跨账号配置，考虑使用 A 记录直接指向目标 IP，或将域名迁移至同一账号。

### （五）Cloudflare SSL/TLS 配置实践

Cloudflare 提供多种 SSL/TLS 加密模式，用于保护客户端到 Cloudflare 以及 Cloudflare 到源服务器之间的数据传输：

**1. Full 模式（推荐用于自签名证书）**
- **适用场景**：源服务器部署了 Cloudflare 提供的自签名证书或其他自签名证书。
- **加密方式**：客户端 ↔ Cloudflare（加密）+ Cloudflare ↔ 源服务器（加密）。
- **证书要求**：源服务器证书可以是自签名证书，Cloudflare 不验证证书的权威性。
- **配置路径**：Cloudflare Dashboard → SSL/TLS → Overview → 选择 "Full"。

**2. Full (Strict) 模式（推荐用于生产环境）**
- **适用场景**：源服务器部署了由权威 CA（如 Let's Encrypt、DigiCert）签发的有效证书。
- **加密方式**：客户端 ↔ Cloudflare（加密）+ Cloudflare ↔ 源服务器（加密且验证）。
- **证书要求**：源服务器必须部署权威 CA 签发的有效证书，Cloudflare 会验证证书的有效性、域名匹配性和信任链。
- **配置路径**：Cloudflare Dashboard → SSL/TLS → Overview → 选择 "Full (strict)"。

**3. 获取 Cloudflare 源服务器证书**
- 在 Cloudflare Dashboard → SSL/TLS → Origin Server → Create Certificate。
- 选择证书有效期（最长 15 年），生成后下载证书和私钥。
- 将证书部署到源服务器（如 Nginx、Apache），配合 Full 模式使用。

**实践建议**：
- **开发/测试环境**：使用 Full 模式 + Cloudflare 自签名证书，快速实现 HTTPS。
- **生产环境**：使用 Full (Strict) 模式 + 权威 CA 证书（如 Let's Encrypt 免费证书），确保端到端安全。
- **避免使用 Flexible 模式**：该模式下 Cloudflare 到源服务器的连接为明文 HTTP，存在安全风险。

## 三、验证命令与排障要点

### （一）验证 NS 生效（Linux/macOS 终端）

```bash
nslookup -type=NS example.com  # 应返回Cloudflare的NS地址
dig NS example.com +short      # 快速查看NS记录
```

### （二）常见问题排查

- **NS 未生效**：检查 NameCheap 中 NS 拼写，等待同步或联系 NameCheap 客服确认配置。
- **Cloudflare 提示 "Nameserver not found"**：延长等待时间，重新触发验证。
- **解析异常**：在 Cloudflare 核对 DNS 记录类型与值，确认 "云朵" 状态符合需求。
- **CNAME 跨账号不生效**：检查 CNAME 目标域名是否与当前域名在同一 Cloudflare 账号下，若不在则需迁移域名或改用 A 记录。
- **源服务器 SSL 证书错误**：
  - Full 模式下仍报错：检查源服务器是否正确部署证书和私钥，确认 HTTPS 端口（443）开放。
  - Full (Strict) 模式下报错：确认源服务器证书是否由权威 CA 签发，检查证书是否过期或域名不匹配。

## 四、操作总结表

| 阶段 | 核心操作 | 关键输出 |
|------|----------|----------|
| 域名购买 | NameCheap 搜索→结算→激活 | Active 状态的一级域名 |
| Cloudflare 配置 | 添加站点→选套餐→获取 NS | 2 条 Cloudflare 专属 NS 地址 |
| NS 替换 | NameCheap 选 Custom DNS→粘贴 NS | NS 记录保存成功 |
| 验证生效 | 第三方工具查询→Cloudflare 确认 | 域名托管成功，可配置解析 |