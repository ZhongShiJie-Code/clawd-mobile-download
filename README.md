# Clawd Mobile - Custom Android Build

这是 ZhongShiJie-Code 维护的 Clawd Mobile 手机桌宠版本。仓库同时保存 Android 源码与下载 Release；旧 APK 和旧 Release 保留，不覆盖、不删除。

## 下载

- [全部安装包与版本说明](https://github.com/ZhongShiJie-Code/clawd-mobile-download/releases)
- v0.11.7 是连接诊断与额度新鲜度测试版，尚未完成真实手机安装验收。
- v0.11.8 是保守省电候选版，发布状态以 Release 页面为准，尚未做真机耗电验收。
- 正式测试安装包沿用 v0.11.6 的签名证书，可覆盖同签名的旧版本；不要拿 Actions 的 CI APK 覆盖安装。

## v0.11.7 改动

- 设备页和设置页显示直连（局域网或域名）/中继各自的状态、故障原因、上次成功时间与重试入口。
- 区分认证、域名解析、证书、超时、服务故障及中继电脑离线。
- Codex 显示最多两组额度窗口；额度卡片显示更新时间、离线数据和超过 30 分钟的过期提示。
- 保留 v0.11.6 的柔和卡片、明暗主题、悬浮桌宠和重连修复。
- 关于页显示源码提交号，检查更新指向本仓库。
- APK 内嵌源码提交；打包脚本验证版本、提交、签名和 SHA-256。

## 源码与构建

v0.11.8 的范围和验收限制见 [省电测试版说明](docs/releases/v0.11.8.md)：熄屏停止悬浮渲染、稳定闲置 SVG 动画 20 帧采样、无网络暂停重试、工具输出合并刷新。工作/完成/授权事件即时处理，手机额度继续使用推送；唤醒锁与心跳不变。

Android 工程位于 `android/`。需要 JDK 17、Android SDK、Node.js 和 Git。

```sh
bash scripts/build-mobile.sh
node scripts/package-release.mjs
```

发布前先提交精确源码，再构建。打包脚本拒绝脏工作目录、错误版本、错误提交和不兼容签名。输出在 `release-artifacts/`，包括 APK、`SHA256SUMS` 和 `release-manifest.json`，这些产物不进入源码提交。

GitHub Actions 自动跑动画策略断言、Lint、82 项定向回归测试和 Debug 构建。CI Debug 签名是临时的，只用于验收；不自动发布，也不包含本地签名密钥。

## 验收限制

v0.11.8 本地构建、Lint、82 项定向测试和 11 项动画策略断言通过。网关 WebSocket/401 冒烟在 v0.11.7 已通过，本次不修改运行网关。完整单元测试套件的既有 `ApprovalViewModelTest` 使用真实时钟与虚拟协程调度，导致倒计时测试持续循环；目前不声称全量测试通过。模拟器系统镜像不可启动，真实手机、动画观感和 Wi-Fi/5G 切换仍待验收，不能声称已测得省电比例。

网关时间字段修复见 `gateway/mobile-gateway-freshness.patch`。补丁不包含配对凭证或机器专属信任配置；Android 不重新估算账单金额。

## 来源与许可

手机端基于 [Bynlk/clawd-on-mobile](https://github.com/Bynlk/clawd-on-mobile) 及其 Clawd on Desk 上游。保留原作者署名与 AGPL-3.0 许可。迁入源码的本地基线提交：`30602ccd`。后续以本仓库的 Release 标签和安装包内嵌提交号为准。
