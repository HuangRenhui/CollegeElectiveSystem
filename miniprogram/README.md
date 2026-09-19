# 学生端微信小程序

高校选修课管理系统的移动端，面向**学生**开放教学评价、课表查询与成绩查询。

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
├── utils/
│   ├── request.js          # 请求封装（鉴权、错误提示、401 跳登录）
│   └── dict.js             # 字典与工具方法（与 Web 端语义一致）
├── api/
│   ├── auth.js             # 登录 / 退出 / 用户信息
│   ├── common.js           # 学期、公告
│   ├── review.js           # 教学评价（7 个接口）
│   └── student.js          # 课表、成绩
└── pages/
    ├── login/              # 登录
    ├── review/
    │   ├── list/           # 待评价课程（tabBar 首页）
    │   ├── form/           # 评价表单（四维星级 + 快捷标签 + 匿名开关）
    │   ├── mine/           # 我的评价（修改 / 撤回）
    │   └── detail/         # 课程评价汇总与匿名明细
    ├── timetable/          # 我的课表（按星期分组，tabBar）
    ├── grade/              # 我的成绩（tabBar）
    └── mine/               # 个人中心（tabBar）
```

---

## 与网页端的差异

| 方面 | 网页端 | 小程序端 |
| --- | --- | --- |
| 面向角色 | 学生 / 教师 / 管理员 | **仅学生** |
| 组件库 | Element Plus | 原生组件 + 自定义 `app.wxss` |
| 星级评分 | `el-rate` 组件 | 自绘星级，支持半星（点击左半区得 .5 分） |
| 课表展示 | 周次网格 | 按星期分组的列表（窄屏可读性更好） |
| 文字评价 | 纯文本输入 | 额外提供**快捷标签**，点选自动拼接 |

**共用的设计约定**（与 Web 端保持一致，切勿单侧修改）：

- 后端返回 `code === 200` 视为成功，数据在 `data` 字段；
- 401 与业务码 2003 均视为登录失效；
- 综合评分 = 四维平均保留 1 位小数，**由后端计算**，前端只做预览；
- 评价状态：1-已提交 2-已公开 3-已隐藏；已隐藏评价不可修改或撤回。

---

## 接口对接

全部复用后端既有接口，**无需为小程序新增任何接口**：

| 模块 | 接口数 | 说明 |
| --- | --- | --- |
| 认证 | 4 | `/auth/login`、`/auth/logout`、`/auth/info`、`/auth/password` |
| 公共 | 3 | `/common/semesters`、`/common/semesters/current`、`/common/notices` |
| 教学评价 | 7 | `/student/reviews/**`、`/student/courses/{id}/review-summary` |
| 课表成绩 | 4 | `/student/timetable`、`/student/selections`、`/student/grades/**` |

---

## 已知限制

1. **未做微信授权登录**：当前使用学号密码登录，与网页端一致。
   如需「微信一键登录」，需后端新增 `openid` 绑定能力（见
   [待实现功能文档 · 4.7 节](../docs/待实现功能.md)）。
2. **未做分享与订阅消息**：如「评价开放提醒」「成绩发布提醒」需接入
   微信订阅消息，同样依赖后端新增推送能力。
3. **本地开发用 HTTP**：正式发布需 HTTPS 域名并在小程序后台配置
   `request` 合法域名。
