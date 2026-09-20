# CollegeElectiveSystem · 高校选修课管理系统

> 基于 **Spring Boot 3 + Vue 3 + Redis** 的高校选修课管理系统。
> 支持学生选课退课、教师课程管理与成绩录入、管理员教务管理；
> 采用 **Redis + Lua 脚本** 实现选课高并发控制，有效防止超选，并自动校验上课时间冲突。

[![Java](https://img.shields.io/badge/Java-21-orange)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.5-brightgreen)](https://spring.io/projects/spring-boot)
[![Vue](https://img.shields.io/badge/Vue-3.5-42b883)](https://vuejs.org/)
[![Redis](https://img.shields.io/badge/Redis-7.x-dc382d)](https://redis.io/)
[![MySQL](https://img.shields.io/badge/MySQL-8.0-4479a1)](https://www.mysql.com/)
[![License](https://img.shields.io/badge/License-MIT-blue)](./LICENSE)

---

## 目录

- [项目简介](#项目简介)
- [当前完成度说明](#当前完成度说明)
- [核心特性](#核心特性)
- [技术栈](#技术栈)
- [系统架构](#系统架构)
- [快速开始](#快速开始)
- [目录结构](#目录结构)
- [学生端小程序](#学生端小程序)
- [数据库设计](#数据库设计)
- [接口文档](#接口文档)
- [选课并发方案](#选课并发方案)
- [账号说明](#账号说明)
- [常见问题](#常见问题)
- [开发规范](#开发规范)
- [参与贡献](#参与贡献)

---

## ⚠️ 当前完成度说明

本项目已完成**框架搭建 + 基础设施 + 大部分业务模块**。**选课模块**与**成绩模块**的
Service 实现**部分完成**（查询类方法已可用，核心的选课/退课、成绩录入/发布待实现），
接口契约、数据模型、前端页面与基础设施均已就绪，待开发者补充实现。
详见 [待实现功能文档](./docs/待实现功能.md#21-方法清单)。

| 模块 | 状态 | 说明 |
| --- | --- | --- |
| 认证与个人中心 | ✅ 可用 | JWT 登录、权限、密码管理 |
| 课程管理 | ✅ 可用 | 课程 CRUD、排课、冲突校验、统计 |
| 学生 / 教师管理 | ✅ 可用 | 信息维护、账号管理 |
| 基础信息 / 学期 / 公告 / 日志 | ✅ 可用 | 教务管理全套能力 |
| **选课模块** | 🚧 **部分可用** | 浏览课程、时间冲突预检、我的选课、课程缓存预热已可用；选课、退课、人数同步待实现 |
| **成绩模块** | ⏳ **待实现** | 查询能力可用（成绩分页、录入单、成绩单）；成绩录入、发布待实现 |
| **教学评价模块** | ✅ **可用** | 学生提交评价、教师查看评价、管理员审核与统计（前后端均已完成） |
| **学生端小程序** | ✅ **可用** | 教学评价、课表、成绩查询（原生微信小程序，复用后端接口） |

> 前端页面、API 封装、Controller 接口签名与 Swagger 注解均已完整编写，
> 完成 Service 实现后即可直接联调。
>
> **教学评价模块**（学生评教）已完整实现（学生提交 / 教师查看 / 管理员审核统计），
> 详见 [待实现功能文档第 4 节](./docs/待实现功能.md#4-教学评价模块)
> 与 [接口文档第 13-15 节](./docs/接口文档.md#13-教学评价)。
>
> 另提供**学生端微信小程序**（`miniprogram/`），支持教学评价、课表与成绩查询，
> 复用后端既有接口，无需额外部署。详见 [小程序说明](./miniprogram/README.md)。

👉 **实现指引请查阅 [待实现功能文档](./docs/待实现功能.md)**
（包含业务规则、Redis 方案、算法参考、验收清单与实现顺序建议）

---

## 项目简介

本系统面向高校教务场景，覆盖 **学生 / 教师 / 管理员** 三类角色，提供选课、排课、成绩、
公告与教务统计的全流程能力。系统重点解决传统选课系统在开放选课瞬间的
**高并发抢课导致超选** 问题，并内置 **上课时间冲突自动检测**。

### 角色与职责

| 角色 | 主要能力 |
| --- | --- |
| **学生 STUDENT** | 浏览可选课程、选课 / 退课、查看个人课表、查询成绩单与绩点、提交教学评价 |
| **教师 TEACHER** | 查看授课课程、查看学生名单、录入 / 发布 / 撤回成绩、查看教学课表、查看本人课程评价 |
| **管理员 ADMIN** | 课程与排课管理、学生管理、教师管理、院系 / 专业 / 教室维护、学期与选课窗口设置、公告发布、操作日志审计、选课开关与缓存预热、评价审核与统计 |

> 其中「选课 / 退课」「成绩录入 / 发布」属于待实现功能，详见 [待实现功能文档](./docs/待实现功能.md)；
> 「教学评价」已完整实现。

---

## 核心特性

- **表结构自动初始化**：开发环境启动时自动检测并创建缺失的数据表，无需手工执行建表脚本；只建表不删数据，重复启动无副作用。
- **高并发选课安全**（设计方案已就绪，待实现）：Redis + Lua 脚本原子化完成「查重 → 余量校验 → 扣减 → 记录已选」四步操作，杜绝超选与重复选课。
- **时间冲突自动校验**（设计方案已就绪，待实现）：基于「星期 + 节次 + 周次 + 单双周」四维重叠判定，选课前预检与落库前复检双重保障。
- **缓存预热与一致性兜底**：管理员可一键预热课程缓存；定时任务周期校正「已选人数」与 Redis 余量，保证最终一致。
- **学分上限控制**：按学期限制学生可选总学分，超额自动拦截。
- **JWT 无状态认证**：令牌写入 Redis 白名单，支持服务端主动注销；临近过期自动滑动续期。
- **登录安全防护**：连续登录失败锁定、密码 bcrypt 加密、修改密码后强制重新登录。
- **完整的教务管理能力**：院系 / 专业 / 教室 / 学期 / 课程 / 公告 / 日志全覆盖。
- **操作日志审计**：注解式 `@OperationLog` + AOP 异步落库，记录操作人、耗时、参数与异常。
- **统一响应与异常处理**：全局 `Result` 结构 + `@RestControllerAdvice`，前后端约定清晰。
- **一键容器化部署**：提供 `docker-compose.yml`，MySQL、Redis、后端、前端一并拉起。

---

## 技术栈

### 后端

| 组件 | 版本 | 说明 |
| --- | --- | --- |
| Java | 21 | LTS，使用 Text Block、Record 等特性 |
| Spring Boot | 3.3.5 | 基础框架 |
| Spring Security | 6.x | 认证与鉴权 |
| MyBatis-Plus | 3.5.7 | ORM、分页、乐观锁、逻辑删除 |
| MySQL | 8.0 | 业务数据存储 |
| Redis | 7.x | 选课原子操作、缓存、令牌白名单 |
| JJWT | 0.12.6 | JWT 令牌签发与解析 |
| Knife4j | 4.5.0 | OpenAPI 3 接口文档 |
| Hutool | 5.8.32 | 通用工具类 |
| Lombok | 1.18.34 | 简化样板代码 |

### 前端

| 组件 | 版本 | 说明 |
| --- | --- | --- |
| Vue | 3.5 | 组合式 API |
| Vite | 5.4 | 构建工具 |
| Element Plus | 2.8 | UI 组件库 |
| Pinia | 2.2 | 状态管理 |
| Vue Router | 4.4 | 路由与权限守卫 |
| Axios | 1.7 | HTTP 请求封装 |
| ECharts | 5.5 | 数据可视化 |
| Day.js | 1.11 | 时间处理 |

### 小程序

| 组件 | 说明 |
| --- | --- |
| 原生微信小程序 | WXML / WXSS / JS，无构建依赖 |
| 基础库 | 3.5.5+ |

### 部署

Docker、Docker Compose、Nginx

---

## 系统架构

```
┌──────────────────────────────────────────────────────────────┐
│                         浏览器 / 客户端                        │
└──────────────────────────────┬───────────────────────────────┘
                               │ HTTP (JWT)
┌──────────────────────────────▼───────────────────────────────┐
│                    Vue 3 前端（Nginx 托管）                    │
│  Element Plus · Pinia · Vue Router · Axios · ECharts          │
└──────────────────────────────┬───────────────────────────────┘
                               │ /api/** 反向代理
┌──────────────────────────────▼───────────────────────────────┐
│                     Spring Boot 3 后端                        │
│ ┌────────────┬────────────┬────────────┬──────────────────┐  │
│ │ Controller │  Service   │   Mapper   │ Security / AOP   │  │
│ └────────────┴─────┬──────┴─────┬──────┴──────────────────┘  │
└──────────┬─────────┼────────────┼─────────────────────────────┘
           │         │            │
           │         │            └──────────────┐
           │         │                           │
   ┌───────▼──────┐  │                  ┌────────▼────────┐
   │  MySQL 8.0   │  │                  │     Redis 7     │
   │  业务数据持久化 │  │                  │ 选课原子操作/缓存 │
   └──────────────┘  └──────────────────┴─────────────────┘
```

### 后端分层

| 层次 | 包路径 | 职责 |
| --- | --- | --- |
| 接口层 | `controller` | 参数校验、权限标注、组装响应 |
| 业务层 | `service` / `service.impl` | 业务编排、事务边界、缓存与并发控制 |
| 持久层 | `mapper` + `resources/mapper` | 数据访问，复杂查询使用 XML |
| 实体层 | `entity` | 数据库表映射 |
| 传输层 | `dto` / `vo` | 入参与出参对象隔离 |
| 安全层 | `security` | JWT 签发解析、认证过滤器、上下文工具 |
| 切面层 | `aspect` | 操作日志、性能观测 |
| 配置层 | `config` | 安全、Redis、MyBatis-Plus、异步、文档等 |
| 任务层 | `task` | 选课数据一致性兜底定时任务 |

---

## 快速开始

### 环境要求

| 依赖 | 最低版本 | 说明 |
| --- | --- | --- |
| JDK | 21 | 后端运行环境 |
| Maven | 3.8+ | 后端构建 |
| Node.js | 18+ | 前端构建 |
| MySQL | 8.0+ | 业务数据库 |
| Redis | 6.0+ | 缓存与选课并发控制 |

### 方式一：Docker Compose 一键启动（推荐）

```bash
# 在项目根目录执行
docker compose up -d --build

# 查看服务状态
docker compose ps

# 查看后端日志
docker compose logs -f backend
```

启动完成后访问：

| 服务 | 地址 |
| --- | --- |
| 前端页面 | http://localhost |
| 后端接口 | http://localhost:8080/api |
| 接口文档 | http://localhost:8080/api/doc.html |

> MySQL 与 Redis 首次启动会自动执行 `schema.sql` 与 `data.sql` 完成建表与初始化数据。

### 方式二：本地开发启动

#### 1. 初始化数据库

**表结构**：开发环境启动应用时会**自动创建缺失的数据表**，无需手工建表。

**数据导入**：项目内置**演示数据**（选课记录、成绩、日志等），导入后各页面立即有内容可展示：

```bash
# 一键初始化（基础数据 + 演示数据）
mysql -uroot -p < scripts/init-db.sql

# 或分步执行
mysql -uroot -p < backend/src/main/resources/db/data.sql
mysql -uroot -p < backend/src/main/resources/db/demo-data.sql
```

> 演示数据规模：20 名学生、8 名教师、17 门课程、80 条选课记录、40 条成绩记录，
> 全部选课组合已通过时间冲突校验。详见 [演示数据说明](./docs/演示数据说明.md)。
>
> 手工建表（可选）：如需手动创建表结构，执行 `backend/src/main/resources/db/schema.sql`。
> 注意该脚本含 `DROP TABLE`，会清空已有数据。

#### 2. 启动 Redis

```bash
# 无密码（默认开发配置）
redis-server

# 若设置了密码，请同步修改 application-dev.yml 中的 spring.data.redis.password
```

#### 3. 启动后端

修改 `backend/src/main/resources/application-dev.yml` 中的数据库账号密码，然后：

```bash
cd backend
mvn clean compile
mvn spring-boot:run
```

后端启动成功后，控制台会打印访问地址与接口文档地址。

#### 4. 启动前端

```bash
cd frontend
npm install
npm run dev
```

前端默认运行在 http://localhost:5174 ，已配置 `/api` 代理到 `http://localhost:8080`。

#### 5. 打包部署

```bash
# 后端打包（产物：backend/target/college-elective-system.jar）
cd backend && mvn clean package -DskipTests

# 前端打包（产物：frontend/dist）
cd frontend && npm run build
```

---

## 目录结构

```
CollegeElectiveSystem/
├── backend/                                 # 后端服务
│   ├── Dockerfile
│   ├── pom.xml
│   └── src/
│       ├── main/
│       │   ├── java/com/college/elective/
│       │   │   ├── CollegeElectiveApplication.java   # 启动类
│       │   │   ├── aspect/            # 操作日志切面与注解
│       │   │   ├── common/            # 统一响应、异常、常量、Redis Key
│       │   │   ├── config/            # 安全/Redis/MyBatis-Plus/异步/文档配置
│       │   │   ├── controller/        # REST 接口
│       │   │   ├── dto/               # 请求参数对象
│       │   │   ├── entity/            # 数据库实体
│       │   │   ├── interceptor/       # 请求耗时拦截器
│       │   │   ├── mapper/            # MyBatis-Plus Mapper
│       │   │   ├── security/          # JWT 与安全上下文
│       │   │   ├── service/           # 业务接口与实现
│       │   │   ├── task/              # 定时任务
│       │   │   └── vo/                # 响应视图对象
│       │   └── resources/
│       │       ├── application*.yml   # 多环境配置
│       │       ├── logback-spring.xml # 日志配置
│       │       ├── db/                # schema.sql / data.sql
│       │       ├── lua/               # 选课/退课原子脚本
│       │       └── mapper/            # MyBatis XML
│       └── test/java/                 # 单元测试
├── frontend/                                # 前端工程
│   ├── Dockerfile
│   ├── nginx.conf
│   ├── vite.config.js
│   └── src/
│       ├── api/            # 接口封装（按模块拆分）
│       ├── layout/         # 主布局、侧边栏、顶栏
│       ├── router/         # 路由表与权限守卫
│       ├── store/          # Pinia 状态（user / app）
│       ├── styles/         # 全局样式与主题变量
│       ├── utils/          # 请求封装、字典、令牌存储
│       └── views/          # 页面（login / dashboard / student / teacher / admin）
├── miniprogram/                             # 学生端微信小程序（原生，无构建依赖）
│   ├── README.md                           # 运行说明
│   ├── app.js / app.json / app.wxss        # 入口、页面注册、全局样式
│   ├── config/index.js                     # baseUrl 等配置（部署前必改）
│   ├── utils/                              # request 封装、dict 字典
│   ├── api/                                # auth / common / review / student
│   └── pages/                              # login、review(4页)、timetable、grade、mine
├── docs/                                    # 项目文档
├── docker-compose.yml
├── README.md
└── README.en.md
```

---

## 学生端小程序

`miniprogram/` 是面向**学生**的原生微信小程序，复用后端既有接口，无需额外部署服务。

| 页面 | 功能 |
| --- | --- |
| 待评价课程（首页） | 卡片列表、已评计数、学期筛选 |
| 填写评价 | 四维星级（支持半星）、快捷标签、匿名开关、二次确认 |
| 我的评价 | 已提交评价的修改与撤回 |
| 课程评价 | 汇总平均分、四维得分条、匿名明细（触底加载） |
| 我的课表 | 按星期分组，含节次时间、周次与地点 |
| 我的成绩 | 总学分 / 已获学分 / 平均分 / 平均绩点 + 明细 |
| 个人中心 | 用户信息、功能入口、退出登录 |

**运行方式**：用微信开发者工具导入 `miniprogram` 目录即可，
AppID 选「测试号」，并在本地设置中勾选「不校验合法域名」。
接口地址在 `miniprogram/config/index.js` 中配置。

> 完整说明见 [小程序 README](./miniprogram/README.md)。
> 微信一键登录、订阅消息等需后端支持的能力见
> [待实现功能文档 4.7.1 节](./docs/待实现功能.md#471-需要后端支持的增强功能待实现)。

---

## 数据库设计

共 **14 张表**，完整 DDL 见 `backend/src/main/resources/db/schema.sql`。

| 表名 | 说明 | 关键约束 |
| --- | --- | --- |
| `sys_user` | 系统用户（统一身份认证） | `username` 唯一；`role` 区分角色 |
| `department` | 院系 | `dept_code` 唯一 |
| `major` | 专业 | `major_code` 唯一 |
| `student` | 学生扩展信息 | `stu_no` 唯一；`user_id` 唯一 |
| `teacher` | 教师扩展信息 | `teacher_no` 唯一；`user_id` 唯一 |
| `semester` | 学期与选课窗口 | `semester_code` 唯一；`is_current` 标记当前学期 |
| `classroom` | 教室 | `room_no` 唯一 |
| `course` | 课程与开课信息 | `(course_code, semester_id)` 唯一；`version` 乐观锁 |
| `course_schedule` | 课程排课（一课程多时段） | 索引 `(semester_id, day_of_week)` |
| `course_selection` | 选课记录 | `(student_id, course_id, semester_id)` 唯一 |
| `course_grade` | 课程成绩 | `selection_id` 唯一；`status` 控制发布状态 |
| `notice` | 系统公告 | `(status, publish_time)` 索引 |
| `sys_log` | 操作日志 | 无逻辑删除，按时间归档 |
| `course_review` | 课程评价（教学评价模块） | `selection_id` 唯一（一选课一评价）；**全库唯一不含 `deleted` 字段的业务表** |

### 关键字段说明

- **逻辑删除**：除 `sys_log` 与 `course_review` 外，所有表使用 `deleted` 字段（0 未删除 / 1 已删除），由 MyBatis-Plus 自动处理。
  `sys_log` 按时间物理归档；`course_review` 因 `selection_id` 唯一键必须物理删除（详见 [数据库设计 3.14](./docs/数据库设计.md#314-course_review-课程评价)）。
- **乐观锁**：`course.version` 用于并发更新课程信息时的冲突检测。
- **选课唯一约束**：`course_selection` 上的唯一索引是防重复选课的最后一道数据库级保障。
- **成绩计算**：`total_score = usual_score × 0.3 + exam_score × 0.7`，绩点采用 4.0 制换算。

---

## 接口文档

系统集成 **Knife4j**，启动后端后访问：

- Knife4j 增强文档：http://localhost:8080/api/doc.html
- OpenAPI 原始文档：http://localhost:8080/api/v3/api-docs

> 在文档页面右上角「Authorize」处填入登录接口返回的 `token`，即可直接调试需鉴权的接口。

### 接口分组

| 编号 | 分组 | 路径前缀 | 权限 |
| --- | --- | --- | --- |
| 01 | 认证与个人中心 | `/auth/**` | 公开 / 登录用户 |
| 02 | 学生选课 | `/student/**` | 学生 |
| 03 | 学生成绩 | `/student/grades/**` | 学生 |
| 04 | 教师工作台 | `/teacher/**` | 教师 |
| 05 | 课程管理 | `/admin/courses/**` | 管理员 |
| 06 | 学生管理 | `/admin/students/**` | 管理员 |
| 07 | 教师管理 | `/admin/teachers/**` | 管理员 |
| 08 | 基础信息 | `/admin/departments`、`/admin/majors`、`/admin/classrooms` | 管理员 |
| 09 | 学期管理 | `/admin/semesters/**` | 管理员 |
| 10 | 公告管理 | `/admin/notices/**` | 管理员 |
| 11 | 系统管理 | `/admin/system/**` | 管理员 |
| 12 | 公共接口 | `/common/**` | 登录用户 |
| 13 | 学生教学评价 | `/student/reviews/**`、`/student/courses/{id}/review-summary` | 学生 |
| 14 | 教师教学评价 | `/teacher/reviews/**`、`/teacher/courses/{id}/reviews` | 教师 |
| 15 | 评价管理 | `/admin/reviews/**` | 管理员 |

### 统一响应结构

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {},
  "timestamp": "2026-09-11 10:30:00",
  "success": true
}
```

| 状态码 | 含义 |
| --- | --- |
| 200 | 成功 |
| 400 | 参数校验失败 |
| 401 | 未登录或登录已过期 |
| 403 | 无操作权限 |
| 2001-2009 | 用户与权限相关业务错误 |
| 3001-3012 | 课程与选课相关业务错误 |
| 4001-4004 | 成绩相关业务错误 |
| 5001-5006 | 系统与数据相关业务错误（5005 当前学期未设置 / 5006 功能尚未实现） |
| 6001-6005 | 教学评价相关业务错误 |

---

## 选课并发方案

选课是本系统最核心的高并发场景，设计方案如下。

### 1. Redis 原子预占（核心）

`backend/src/main/resources/lua/select_course.lua`：

```lua
local capacity = redis.call('GET', KEYS[1])
if capacity == false then return 3 end                          -- 缓存未就绪
if redis.call('SISMEMBER', KEYS[2], ARGV[1]) == 1 then
    return 1                                                     -- 已选过
end
if tonumber(capacity) <= 0 then return 2 end                     -- 余量不足
redis.call('DECR', KEYS[1])
redis.call('SADD', KEYS[2], ARGV[1])
-- 两个 Key 同步续期，保持生命周期一致（ARGV[2] 为 TTL 秒数）
local ttl = tonumber(ARGV[2])
if ttl and ttl > 0 then
    redis.call('EXPIRE', KEYS[1], ttl)
    redis.call('EXPIRE', KEYS[2], ttl)
end
return 0                                                         -- 预占成功
```

借助 Redis 单线程执行 Lua 脚本的特性，将「查重 + 余量判断 + 扣减 + 记录」合并为一个原子操作，
从根本上避免并发场景下的超选问题。

> **TTL 约定**：`SADD` 会自动创建 Set 且不带过期时间。若只给容量 Key 设 TTL，
> 容量 Key 过期回源重建后，残留的永久 Set 会让 `SISMEMBER` 命中已退课的学生，
> 从而误判为「已选过」。因此脚本对两个 Key 同步续期，调用方传入
> `RedisKeys.DEFAULT_CACHE_SECONDS`。

| Redis Key | 类型 | 说明 |
| --- | --- | --- |
| `elective:course:capacity:{courseId}` | String | 课程剩余容量（30 分钟过期） |
| `elective:course:selected:{courseId}` | Set | 已选学生 ID 集合（30 分钟过期） |
| `elective:selection:switch` | String | 选课总开关（on / off） |
| `elective:auth:token:{userId}` | String | 令牌白名单 |

### 2. 完整选课流程

```
① 校验选课开关与选课时间窗口
        ↓
② 校验课程状态（是否下架 / 结课）
        ↓
③ 校验学分上限（选课后不得超过配置上限）
        ↓
④ 时间冲突预检（与已选课程逐条比对）
        ↓
⑤ 执行 Lua 脚本完成 Redis 原子预占
        ↓
⑥ 预占成功 → 写入选课记录（事务）
        ↓
⑦ 同步数据库 selected_count
        ↓
   任一步失败 → 回滚 Redis 预占（归还余量 + 移除记录）
```

### 3. 时间冲突判定规则

两条排课记录被判定为冲突，需同时满足：

1. **同一天**：`day_of_week` 相同；
2. **节次重叠**：`startA ≤ endB && endA ≥ startB`；
3. **周次重叠**：`[startWeekA, endWeekA]` 与 `[startWeekB, endWeekB]` 有交集；
4. **周类型兼容**：若一方为「每周」则冲突；若为「单周 / 双周」则需存在同为奇偶数的公共周次。

### 4. 缓存预热与一致性兜底

- **预热**：管理员在开放选课前调用 `POST /admin/courses/cache/preload`，将课程余量与已选集合写入 Redis。
- **懒加载**：选课时若发现缓存缺失，自动按数据库数据回源并加锁重建。
- **兜底同步**：`SelectionSyncTask` 默认每 5 分钟校正一次课程已选人数与 Redis 余量。

---

## 账号说明

初始化脚本 `data.sql` 已内置演示数据，**所有账号初始密码均为 `123456`**。

| 角色 | 账号 | 密码 | 说明 |
| --- | --- | --- | --- |
| 管理员 | `admin` | `123456` | 系统管理员 |
| 教师 | `T2026001` | `123456` | 张明远 · 教授（计算机学院） |
| 教师 | `T2026002` | `123456` | 李承阳 · 副教授（电信学院） |
| 教师 | `T2026003` | `123456` | 王雅琴 · 教授（经管学院） |
| 教师 | `T2026004` | `123456` | 陈静怡 · 讲师（外语学院） |
| 教师 | `T2026005` | `123456` | 赵一鸣 · 副教授（艺术学院） |
| 学生 | `2026010101` | `123456` | 刘思远 · 计科2601 |
| 学生 | `2026010102` | `123456` | 孙嘉怡 · 计科2601 |
| 学生 | `2026010201` | `123456` | 周子昂 · 软工2601 |
| 学生 | `2026020101` | `123456` | 吴雨桐 · 电信2601 |
| 学生 | `2026030101` | `123456` | 郑宇轩 · 工商2601 |

> 初始化数据中的密码使用 `{noop}` 前缀表示明文，便于快速体验；
> 通过管理端新建用户或修改密码时，系统会自动使用 **bcrypt** 加密存储。
> **生产环境请务必修改所有默认密码，并通过环境变量覆盖 `JWT_SECRET`。**

---

## 常见问题

<details>
<summary><b>1. 选课提示「选课通道已关闭」？</b></summary>

检查三处配置：
- `elective.selection.enabled` 是否为 `true`（`application.yml`）；
- Redis 中 `elective:selection:switch` 是否为 `off`（管理员可在系统管理中切换）；
- 当前学期是否已设置选课开放时间，且当前时间处于窗口内。
</details>

<details>
<summary><b>2. 选课提示「课程缓存尚未就绪」？</b></summary>

说明 Redis 中缺少该课程的余量键。正常情况系统会自动回源重建；
若持续报错，请确认 Redis 连接正常，并尝试管理员执行 **缓存预热**。
</details>

<details>
<summary><b>3. 数据库已选人数与页面显示不一致？</b></summary>

Redis 承担实时余量，数据库计数为兜底值。系统默认每 5 分钟自动同步一次，
也可手动调用 `POST /admin/courses/selection/sync` 立即校正。
</details>

<details>
<summary><b>4. 登录后刷新页面被踢回登录页？</b></summary>

令牌默认有效期为 2 小时（`elective.jwt.expire-seconds`），
且令牌以「白名单」形式存于 Redis。若 Redis 被清空或后端重启导致 Redis 数据丢失，令牌将失效。
另需确认前后端时间同步，避免 JWT 校验因时间偏差失败。
</details>

<details>
<summary><b>5. 前端开发环境接口 404？</b></summary>

确认 `frontend/.env.development` 中 `VITE_PROXY_TARGET` 指向的后端地址与端口正确，
且后端 `server.servlet.context-path` 为 `/api`（前端请求前缀已包含 `/api`）。
</details>

<details>
<summary><b>6. Maven 编译报 Lombok 相关「找不到符号」？</b></summary>

确认 `pom.xml` 的 `maven-compiler-plugin` 中已配置 `annotationProcessorPaths` 引入 Lombok，
且 IDE 已启用注解处理（IDEA：Settings → Build → Compiler → Annotation Processors）。
</details>

---

## 开发规范

### 代码规范

- **分层清晰**：Controller 只做参数校验与响应组装，业务逻辑集中在 Service。
- **对象隔离**：入参使用 `DTO`，出参使用 `VO`，避免直接暴露实体。
- **异常统一**：业务异常抛出 `BusinessException`，由全局处理器转换为统一响应。
- **常量集中**：业务枚举与状态值统一维护在 `Constants`，Redis Key 统一维护在 `RedisKeys`。
- **事务边界**：写操作标注 `@Transactional(rollbackFor = Exception.class)`。
- **日志规范**：关键业务节点打印 INFO，异常打印 ERROR 并携带上下文参数。

### 命名规范

| 类型 | 规范 | 示例 |
| --- | --- | --- |
| 类 | 大驼峰 | `CourseSelectionService` |
| 方法 / 变量 | 小驼峰 | `selectCourse` |
| 常量 | 大写下划线 | `COURSE_STATUS_NORMAL` |
| 数据库表 / 字段 | 小写下划线 | `course_selection.student_id` |
| 前端组件 | 小写中划线 | `course-card.vue` |
| 前端变量 | 小驼峰 | `courseList` |

### 分支与提交

```
main / master     生产分支，保持可发布状态
develop           开发主干
feature/xxx       功能分支
hotfix/xxx        紧急修复分支
```

提交信息建议遵循 Conventional Commits：

```
feat: 新增选课时间冲突自动校验
fix: 修复并发选课下余量缓存不同步问题
docs: 完善 README 部署说明
refactor: 重构成绩计算逻辑
test: 补充排课冲突算法单元测试
```

---

## 参与贡献

1. Fork 本仓库
2. 新建 `Feat_xxx` 分支
3. 提交代码
4. 新建 Pull Request

---

## 许可证

本项目基于 [MIT](./LICENSE) 许可证开源。

## 相关文档

- [环境安装指南](./docs/环境安装指南.md) —— MySQL 与 Redis 安装配置
- [演示数据说明](./docs/演示数据说明.md) —— 内置演示数据与页面效果
- [**待实现功能**](./docs/待实现功能.md) —— 选课与成绩模块实现指引，教学评价模块实现说明
- [小程序说明](./miniprogram/README.md) —— 学生端微信小程序的运行与配置
- [架构设计](./docs/架构设计.md)
- [数据库设计](./docs/数据库设计.md)
- [接口文档](./docs/接口文档.md)
- [部署运维](./docs/部署运维.md)
- [开发指南](./docs/开发指南.md)
- [更新日志](./docs/更新日志.md)
- [文档索引](./docs/文档索引.md)
- [English README](./README.en.md)
