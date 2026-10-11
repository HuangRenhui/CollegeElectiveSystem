# 学生端微信小程序

高校选修课管理系统的移动端，面向**学生**开放教学评价、课表查询、成绩查询与公告查看。

> ℹ️ 完整教务管理（课程、排课、学生/教师维护、成绩录入、评价审核等）请使用网页端。

---

## 快速开始

### 1. 启动后端

小程序依赖后端接口，请先确保后端服务已启动：

```bash
cd backend
mvn spring-boot:run
```

默认监听 `http://localhost:8080`。

### 2. 修改接口地址

编辑 `config/index.js`：

```js
baseUrl: 'http://localhost:8080/api'
```

> ⚠️ **真机调试时不能写 `localhost`**，需改为电脑的局域网 IP，
> 例如 `http://192.168.1.100:8080/api`，并确保手机与电脑在同一网络。

### 3. 用微信开发者工具打开

1. 打开**微信开发者工具** → 导入项目；
2. 目录选择本 `miniprogram` 文件夹（不是仓库根目录）；
3. AppID 选择「测试号」即可（`project.config.json` 中已配置 `touristappid`）；
4. 在**详情 → 本地设置**中勾选 **「不校验合法域名、web-view、TLS 版本以及 HTTPS 证书」**
   —— 本地开发用 HTTP 必须勾选此项。

### 4. 登录

小程序与网页端共用同一套账号体系，直接在登录页输入学号与密码即可。
点击「学生」可一键填充演示账号。

---

## 目录结构

```
miniprogram/
├── app.js                  # 入口：登录态校验、全局用户信息
├── app.json                # 页面注册、tabBar、窗口配置
├── app.wxss                # 全局样式与 CSS 变量（对齐 Element Plus 配色）
├── project.config.json     # 开发者工具项目配置
├── config/
│   └── index.js            # baseUrl、缓存键名、超时（部署前必改 baseUrl）
├── styles/
│   └── skeleton.wxss       # 骨架屏样式（app.wxss 中 @import，全局可用）
├── utils/
│   ├── request.js          # 请求封装（鉴权、错误提示、401 跳登录）
│   ├── dict.js             # 字典与工具方法（与 Web 端语义一致）
│   ├── share.js            # 分享卡片配置（标题/路径/封面）
│   └── subscribe.js        # 订阅消息授权与续期引导
├── api/
│   ├── auth.js             # 登录 / 退出 / 用户信息
│   ├── common.js           # 学期、公告列表与公告详情
│   ├── review.js           # 教学评价（含评价趋势）
│   ├── student.js          # 课表、成绩
│   └── wx.js               # 微信登录、账号绑定、订阅消息
└── pages/
    ├── login/              # 登录（支持微信一键登录 + 首次绑定）
    ├── review/
    │   ├── list/           # 待评价课程（tabBar 首页，含趋势入口）
    │   ├── form/           # 评价表单（四维星级 + 快捷标签 + 匿名开关）
    │   ├── mine/           # 我的评价（修改 / 撤回）
    │   ├── detail/         # 课程评价汇总与匿名明细
    │   └── trend/          # 评价趋势（Canvas 雷达图 + 学期折线）
    ├── timetable/          # 我的课表（按星期分组，tabBar）
    ├── grade/              # 我的成绩（tabBar）
    ├── notice/             # 公共信息（公告列表，tabBar）
    └── mine/               # 个人中心（tabBar）
```

> 底部 tabBar 共 5 项：**待评价 / 课表 / 成绩 / 公共信息 / 我的**。

---

## 与网页端的差异

| 方面 | 网页端 | 小程序端 |
| --- | --- | --- |
| 面向角色 | 学生 / 教师 / 管理员 | **仅学生** |
| 组件库 | Element Plus | 原生组件 + 自定义 `app.wxss` |
| 登录方式 | 账号密码 | 账号密码 + **微信一键登录** |
| 星级评分 | `el-rate` 组件 | 自绘星级，支持半星（点击左半区得 .5 分） |
| 课表展示 | 周次网格 | 按星期分组的列表（窄屏可读性更好） |
| 文字评价 | 纯文本输入 | 额外提供**快捷标签**，点选自动拼接 |
| 评价可视化 | ECharts 图表 | Canvas 2D 手绘雷达图与折线图（无第三方依赖） |
| 加载态 | `v-loading` 指令 | 自绘**骨架屏**（`styles/skeleton.wxss`） |
| 消息通知 | 无 | **微信订阅消息**（成绩发布 / 评价开放） |

**共用的设计约定**（与 Web 端保持一致，切勿单侧修改）：

- 后端返回 `code === 200` 视为成功，数据在 `data` 字段；
- 401 与业务码 2003 均视为登录失效；
- 综合评分 = 四维平均保留 1 位小数，**由后端计算**，前端只做预览；
- 评价状态：1-已提交 2-已公开 3-已隐藏；已隐藏评价不可修改或撤回。

---

## 接口对接

教学评价、课表、成绩等能力复用后端既有接口；
微信相关能力使用新增的 `/wx/**` 接口：

| 模块 | 接口数 | 说明 |
| --- | --- | --- |
| 认证 | 4 | `/auth/login`、`/auth/logout`、`/auth/info`、`/auth/password` |
| 公共 | 5 | `/common/semesters`、`/common/semesters/current`、`/common/server-time`、`/common/notices`、`/common/notices/{id}` |
| 教学评价 | 8 | `/student/reviews/**`（含 `/reviews/trend`）、`/student/courses/{id}/review-summary` |
| 课表成绩 | 4 | `/student/timetable`、`/student/selections`、`/student/grades/**` |
| 微信 | 6 | `/wx/enabled`、`/wx/login`、`/wx/bind`、`/wx/subscribe/**` |

> 公告接口由后端按登录角色过滤（`target_role = ALL` 或本角色），
> 小程序端无需额外处理：学生只会拿到面向 `ALL` 与 `STUDENT` 的公告。

---

## 微信能力（可选）

微信一键登录与订阅消息**依赖后端配置**，未配置时自动禁用，不影响其余功能。

### 配置方式

> ⚠️ **申请位置**：以下凭证均在**微信公众平台（mp.weixin.qq.com）→ 小程序后台**获取，
> **不是公众号**。本项目调用的三类接口——`sns/jscode2session`（登录换 OpenID）、
> `cgi-bin/message/subscribe/send`（订阅消息）、`wx.requestSubscribeMessage`（小程序端授权）——
> 全部为**小程序专属**，用公众号的 AppID 调用会返回 `errcode 40013 (invalid appid)`。
>
> 具体路径：AppID/AppSecret 见「开发管理 → 开发设置」；
> 模板 ID 见「功能 → 订阅消息」（从公共模板库选用或申请自定义模板）。

在后端通过**环境变量**注入（切勿写入配置文件提交到仓库）：

```bash
WX_APP_ID=你的小程序AppID
WX_APP_SECRET=你的小程序AppSecret
WX_TPL_REVIEW_OPEN=评价开放提醒的模板ID
WX_TPL_GRADE_PUBLISH=成绩发布提醒的模板ID
```

未配置 `WX_APP_ID` / `WX_APP_SECRET` 时，`GET /wx/enabled` 返回 `false`，
小程序登录页会自动隐藏「微信一键登录」按钮。

> 📌 **测试号的限制**：`project.config.json` 中的 `touristappid` 为**测试号**，
> 仅可用于本地调试**账号密码登录**；测试号**不支持订阅消息**
> （`requestSubscribeMessage` 无法使用），也无法正常打通微信一键登录。
> 要完整验证本节的微信能力，需注册正式小程序账号并完成主体认证。

### 微信一键登录

首次使用需输入学号与密码完成绑定（**这是安全底线，不可省略**）：
未绑定的微信身份不会签发令牌，否则任意微信用户都能进入系统。

绑定关系存于 `user_wx_bind` 表，以 `(wx_appid, open_id)` 唯一索引兜底，
一个微信身份只能绑定一个系统账号。

### 订阅消息

微信限制：**用户每次授权只能收到一条消息**，用完即失效。因此：

- 不在启动时弹窗索权（会被习惯性拒绝）；
- 只在用户**明确需要通知**的时刻引导：提交评价成功后、查看成绩单时；
- `utils/subscribe.js#guideSubscribe` 会先查剩余次数，已订阅时静默跳过；
- 剩余次数为 0 时会再次引导，避免消息发不出去才发现。

---

## 已知限制

1. **本地开发用 HTTP**：正式发布需 HTTPS 域名并在小程序后台配置
   `request` 合法域名。
2. **分享封面图未提供**：未放置时微信自动截取当前页面作为封面，
   功能正常但观感一般。如需定制，将 5:4 比例的图片放入
   `miniprogram/static/images/` 并在 `utils/share.js` 中填写路径。
3. **微信能力需自行申请**：`AppID`、`AppSecret` 与订阅消息模板 ID
   均需在**微信公众平台 · 小程序后台**申请（非公众号），
   申请通过后按上述方式配置即可启用；
   测试号（`touristappid`）不支持订阅消息，无法完整验证微信能力。
