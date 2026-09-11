# CollegeElectiveSystem · College Elective Course Management System

> A college elective course management system built with **Spring Boot 3 + Vue 3 + Redis**.
> It supports student course enrollment/withdrawal, teacher course & grade management, and
> administrative academic affairs management. **Redis + Lua scripts** are used for high-concurrency
> course selection to prevent over-enrollment, with automatic class-time conflict detection.

[![Java](https://img.shields.io/badge/Java-21-orange)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.5-brightgreen)](https://spring.io/projects/spring-boot)
[![Vue](https://img.shields.io/badge/Vue-3.5-42b883)](https://vuejs.org/)
[![Redis](https://img.shields.io/badge/Redis-7.x-dc382d)](https://redis.io/)
[![MySQL](https://img.shields.io/badge/MySQL-8.0-4479a1)](https://www.mysql.com/)
[![License](https://img.shields.io/badge/License-MIT-blue)](./LICENSE)

[中文文档](./README.md)

---

## Table of Contents

- [Introduction](#introduction)
- [Core Features](#core-features)
- [Tech Stack](#tech-stack)
- [Architecture](#architecture)
- [Quick Start](#quick-start)
- [Project Structure](#project-structure)
- [Database Design](#database-design)
- [API Documentation](#api-documentation)
- [Concurrency Strategy](#concurrency-strategy)
- [Demo Accounts](#demo-accounts)
- [FAQ](#faq)
- [Contributing](#contributing)

---

## Introduction

The system serves three roles — **Student / Teacher / Admin** — covering course enrollment,
scheduling, grading, notices and academic statistics. It focuses on solving the
**over-enrollment problem caused by high-concurrency course grabbing**, and provides
**automatic class-time conflict detection**.

| Role | Capabilities |
| --- | --- |
| **STUDENT** | Browse courses, enroll / withdraw, view personal timetable, view grades & GPA |
| **TEACHER** | View teaching courses, view student rosters, input / publish / revoke grades, view teaching timetable |
| **ADMIN** | Course & schedule management, student/teacher management, departments/majors/classrooms, semesters & selection windows, notices, operation logs, selection switch & cache preloading |

---

## Core Features

- **High-concurrency safe enrollment** — Redis + Lua performs duplicate-check, capacity check, decrement and record insertion atomically.
- **Automatic time-conflict detection** — four-dimension overlap check (day of week + sections + weeks + odd/even week).
- **Cache preloading & consistency fallback** — one-click course cache preloading plus a scheduled job reconciling DB counts with Redis.
- **Credit limit control** — enforces a per-semester total credit cap.
- **JWT stateless authentication** — tokens stored in a Redis whitelist for server-side revocation and sliding renewal.
- **Login protection** — consecutive failure lockout, bcrypt password hashing, forced re-login after password change.
- **Full academic administration** — departments, majors, classrooms, semesters, courses, notices and logs.
- **Operation log auditing** — annotation-driven `@OperationLog` with AOP and async persistence.
- **Unified response & exception handling** — global `Result` structure with `@RestControllerAdvice`.
- **One-command containerized deployment** — `docker-compose.yml` brings up MySQL, Redis, backend and frontend.

---

## Tech Stack

### Backend

| Component | Version | Description |
| --- | --- | --- |
| Java | 21 | LTS runtime |
| Spring Boot | 3.3.5 | Core framework |
| Spring Security | 6.x | Authentication & authorization |
| MyBatis-Plus | 3.5.7 | ORM, pagination, optimistic lock, logical delete |
| MySQL | 8.0 | Business data storage |
| Redis | 7.x | Atomic enrollment, cache, token whitelist |
| JJWT | 0.12.6 | JWT issuing & parsing |
| Knife4j | 4.5.0 | OpenAPI 3 documentation |
| Hutool | 5.8.32 | Utility library |
| Lombok | 1.18.34 | Boilerplate reduction |

### Frontend

| Component | Version | Description |
| --- | --- | --- |
| Vue | 3.5 | Composition API |
| Vite | 5.4 | Build tool |
| Element Plus | 2.8 | UI component library |
| Pinia | 2.2 | State management |
| Vue Router | 4.4 | Routing & route guards |
| Axios | 1.7 | HTTP client wrapper |
| ECharts | 5.5 | Data visualization |
| Day.js | 1.11 | Date utilities |

### Deployment

Docker, Docker Compose, Nginx

---

## Architecture

```
┌──────────────────────────────────────────────────────────────┐
│                       Browser / Client                        │
└──────────────────────────────┬───────────────────────────────┘
                               │ HTTP (JWT)
┌──────────────────────────────▼───────────────────────────────┐
│                  Vue 3 Frontend (served by Nginx)             │
│  Element Plus · Pinia · Vue Router · Axios · ECharts          │
└──────────────────────────────┬───────────────────────────────┘
                               │ /api/** reverse proxy
┌──────────────────────────────▼───────────────────────────────┐
│                    Spring Boot 3 Backend                      │
│ ┌────────────┬────────────┬────────────┬──────────────────┐  │
│ │ Controller │  Service   │   Mapper   │ Security / AOP   │  │
│ └────────────┴─────┬──────┴─────┬──────┴──────────────────┘  │
└──────────┬─────────┼────────────┼─────────────────────────────┘
           │         │            │
   ┌───────▼──────┐  │   ┌────────▼────────┐
   │  MySQL 8.0   │  └───│     Redis 7     │
   │  Persistence │      │ Atomic / Cache  │
   └──────────────┘      └─────────────────┘
```

### Backend Layers

| Layer | Package | Responsibility |
| --- | --- | --- |
| Controller | `controller` | Validation, role annotation, response assembly |
| Service | `service` / `service.impl` | Business orchestration, transactions, caching, concurrency |
| Mapper | `mapper` + `resources/mapper` | Data access (XML for complex queries) |
| Entity | `entity` | Database table mapping |
| Transport | `dto` / `vo` | Request & response isolation |
| Security | `security` | JWT, authentication filter, context utilities |
| Aspect | `aspect` | Operation logging, performance observation |
| Config | `config` | Security, Redis, MyBatis-Plus, async, docs |
| Task | `task` | Scheduled consistency reconciliation |

---

## Quick Start

### Requirements

| Dependency | Minimum Version |
| --- | --- |
| JDK | 21 |
| Maven | 3.8+ |
| Node.js | 18+ |
| MySQL | 8.0+ |
| Redis | 6.0+ |

### Option 1: Docker Compose (Recommended)

```bash
docker compose up -d --build
docker compose ps
docker compose logs -f backend
```

Access points:

| Service | URL |
| --- | --- |
| Frontend | http://localhost |
| Backend API | http://localhost:8080/api |
| API Docs | http://localhost:8080/api/doc.html |

> `schema.sql` and `data.sql` are executed automatically on first MySQL startup.

### Option 2: Local Development

#### 1. Initialize database

```bash
mysql -uroot -p < backend/src/main/resources/db/schema.sql
mysql -uroot -p < backend/src/main/resources/db/data.sql
```

#### 2. Start Redis

```bash
redis-server
```

#### 3. Start backend

Update the datasource credentials in `backend/src/main/resources/application-dev.yml`, then:

```bash
cd backend
mvn clean compile
mvn spring-boot:run
```

#### 4. Start frontend

```bash
cd frontend
npm install
npm run dev
```

The dev server runs at http://localhost:5173 and proxies `/api` to `http://localhost:8080`.

#### 5. Build for production

```bash
cd backend && mvn clean package -DskipTests   # -> backend/target/college-elective-system.jar
cd frontend && npm run build                  # -> frontend/dist
```

---

## Project Structure

```
CollegeElectiveSystem/
├── backend/                                 # Backend service
│   ├── Dockerfile
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/college/elective/
│       │   ├── CollegeElectiveApplication.java
│       │   ├── aspect/            # Operation log aspect & annotation
│       │   ├── common/            # Result, exceptions, constants, Redis keys
│       │   ├── config/            # Security / Redis / MyBatis-Plus / async / OpenAPI
│       │   ├── controller/        # REST endpoints
│       │   ├── dto/               # Request objects
│       │   ├── entity/            # DB entities
│       │   ├── interceptor/       # Request timing interceptor
│       │   ├── mapper/            # MyBatis-Plus mappers
│       │   ├── security/          # JWT & security context
│       │   ├── service/           # Business interfaces & implementations
│       │   ├── task/              # Scheduled tasks
│       │   └── vo/                # View objects
│       └── resources/
│           ├── application*.yml   # Multi-environment config
│           ├── logback-spring.xml
│           ├── db/                # schema.sql / data.sql
│           ├── lua/               # Enrollment Lua scripts
│           └── mapper/            # MyBatis XML
├── frontend/                                # Frontend project
│   ├── Dockerfile
│   ├── nginx.conf
│   ├── vite.config.js
│   └── src/
│       ├── api/            # API wrappers
│       ├── layout/         # Layout, sidebar, navbar
│       ├── router/         # Routes & guards
│       ├── store/          # Pinia stores
│       ├── styles/         # Global styles & variables
│       ├── utils/          # Request wrapper, dictionaries, token storage
│       └── views/          # Pages
├── docs/                                    # Documentation
├── docker-compose.yml
├── README.md
└── README.en.md
```

---

## Database Design

**14 tables** in total. See `backend/src/main/resources/db/schema.sql` for full DDL.

| Table | Description | Key Constraints |
| --- | --- | --- |
| `sys_user` | System users | `username` unique |
| `department` | Departments | `dept_code` unique |
| `major` | Majors | `major_code` unique |
| `student` | Student profile | `stu_no` unique, `user_id` unique |
| `teacher` | Teacher profile | `teacher_no` unique, `user_id` unique |
| `semester` | Semesters & selection windows | `semester_code` unique, `is_current` flag |
| `classroom` | Classrooms | `room_no` unique |
| `course` | Courses | `(course_code, semester_id)` unique, `version` optimistic lock |
| `course_schedule` | Course schedules | index `(semester_id, day_of_week)` |
| `course_selection` | Enrollment records | `(student_id, course_id, semester_id)` unique |
| `course_grade` | Grades | `selection_id` unique |
| `notice` | Notices | index `(status, publish_time)` |
| `sys_log` | Operation logs | time-based archiving |

### Notes

- **Logical delete** — all tables except `sys_log` use a `deleted` column handled by MyBatis-Plus.
- **Optimistic lock** — `course.version` guards concurrent course updates.
- **Grade formula** — `total_score = usual_score × 0.3 + exam_score × 0.7`, GPA on a 4.0 scale.

---

## API Documentation

Powered by **Knife4j**:

- Knife4j UI: http://localhost:8080/api/doc.html
- OpenAPI JSON: http://localhost:8080/api/v3/api-docs

Use the **Authorize** button and paste the `token` returned by the login API to test secured endpoints.

### Endpoint Groups

| No. | Group | Prefix | Access |
| --- | --- | --- | --- |
| 01 | Auth & Profile | `/auth/**` | Public / Authenticated |
| 02 | Student Enrollment | `/student/**` | STUDENT |
| 03 | Student Grades | `/student/grades/**` | STUDENT |
| 04 | Teacher Workbench | `/teacher/**` | TEACHER |
| 05 | Course Management | `/admin/courses/**` | ADMIN |
| 06 | Student Management | `/admin/students/**` | ADMIN |
| 07 | Teacher Management | `/admin/teachers/**` | ADMIN |
| 08 | Base Info | `/admin/departments`, `/admin/majors`, `/admin/classrooms` | ADMIN |
| 09 | Semester Management | `/admin/semesters/**` | ADMIN |
| 10 | Notice Management | `/admin/notices/**` | ADMIN |
| 11 | System Management | `/admin/system/**` | ADMIN |
| 12 | Common | `/common/**` | Authenticated |

### Unified Response

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {},
  "timestamp": "2026-09-11 10:30:00",
  "success": true
}
```

| Code | Meaning |
| --- | --- |
| 200 | Success |
| 400 | Validation failed |
| 401 | Unauthenticated / token expired |
| 403 | Forbidden |
| 2001-2009 | User & permission errors |
| 3001-3012 | Course & enrollment errors |
| 4001-4004 | Grade errors |
| 5001-5005 | System & data errors |

---

## Concurrency Strategy

### 1. Atomic Redis Reservation

`backend/src/main/resources/lua/select_course.lua`:

```lua
local capacity = redis.call('GET', KEYS[1])
if capacity == false then return 3 end                          -- cache not ready
if redis.call('SISMEMBER', KEYS[2], ARGV[1]) == 1 then
    return 1                                                     -- already selected
end
if tonumber(capacity) <= 0 then return 2 end                     -- full
redis.call('DECR', KEYS[1])
redis.call('SADD', KEYS[2], ARGV[1])
return 0                                                         -- reserved
```

Because Redis executes Lua scripts single-threaded, the duplicate check, capacity check,
decrement and record insertion become one atomic operation, eliminating over-enrollment.

| Redis Key | Type | Purpose |
| --- | --- | --- |
| `elective:course:capacity:{courseId}` | String | Remaining capacity |
| `elective:course:selected:{courseId}` | Set | Selected student IDs |
| `elective:selection:switch` | String | Global selection switch |
| `elective:auth:token:{userId}` | String | Token whitelist |

### 2. Enrollment Flow

```
① Check selection switch & time window
        ↓
② Check course status
        ↓
③ Check credit limit
        ↓
④ Pre-check time conflicts against selected courses
        ↓
⑤ Execute Lua script (atomic Redis reservation)
        ↓
⑥ Persist enrollment record (transaction)
        ↓
⑦ Sync database selected_count
        ↓
   Any failure → roll back the Redis reservation
```

### 3. Conflict Rules

Two schedules conflict only when ALL conditions hold:

1. Same day of week;
2. Section ranges overlap (`startA ≤ endB && endA ≥ startB`);
3. Week ranges overlap;
4. Week types compatible (weekly conflicts with anything; odd vs even only conflicts on shared parity weeks).

### 4. Preloading & Reconciliation

- **Preload** — `POST /admin/courses/cache/preload` writes capacity and selected sets into Redis.
- **Lazy load** — if the cache is missing, the service rebuilds it from the database under a lock.
- **Reconcile** — `SelectionSyncTask` runs every 5 minutes by default.

---

## Demo Accounts

Seeded via `data.sql`. **All initial passwords are `123456`.**

| Role | Username | Password |
| --- | --- | --- |
| ADMIN | `admin` | `123456` |
| TEACHER | `T2026001` | `123456` |
| TEACHER | `T2026002` | `123456` |
| TEACHER | `T2026003` | `123456` |
| TEACHER | `T2026004` | `123456` |
| TEACHER | `T2026005` | `123456` |
| STUDENT | `2026010101` | `123456` |
| STUDENT | `2026010102` | `123456` |
| STUDENT | `2026010201` | `123456` |
| STUDENT | `2026020101` | `123456` |
| STUDENT | `2026030101` | `123456` |

> Seed passwords use the `{noop}` prefix for quick onboarding.
> Accounts created via the admin console use **bcrypt**.
> **Change all default credentials and override `JWT_SECRET` in production.**

---

## FAQ

<details>
<summary><b>1. "Selection channel is closed" on enrolling?</b></summary>

Check three things:
- `elective.selection.enabled` is `true`;
- The Redis key `elective:selection:switch` is not `off`;
- The current semester's selection window covers the current time.
</details>

<details>
<summary><b>2. "Course cache not ready"?</b></summary>

The Redis capacity key is missing. The system normally rebuilds it automatically;
if the error persists, verify Redis connectivity and trigger cache preloading.
</details>

<details>
<summary><b>3. DB selected count differs from UI?</b></summary>

Redis holds the real-time capacity while the DB count is a fallback.
Auto-reconciliation runs every 5 minutes, or call `POST /admin/courses/selection/sync`.
</details>

<details>
<summary><b>4. Kicked back to login after refresh?</b></summary>

Tokens expire after 2 hours (`elective.jwt.expire-seconds`) and are stored in a Redis whitelist.
Clearing Redis or losing Redis data invalidates tokens. Also ensure clock synchronization.
</details>

<details>
<summary><b>5. Frontend dev server returns 404?</b></summary>

Verify `VITE_PROXY_TARGET` in `frontend/.env.development` points to the correct backend address,
and that `server.servlet.context-path` is `/api`.
</details>

<details>
<summary><b>6. Maven "cannot find symbol" for Lombok-generated methods?</b></summary>

Ensure `annotationProcessorPaths` includes Lombok in `pom.xml` and that annotation processing
is enabled in your IDE.
</details>

---

## Contributing

1. Fork the repository
2. Create a `Feat_xxx` branch
3. Commit your changes
4. Open a Pull Request

---

## License

Released under the [MIT License](./LICENSE).

## Related Documents

- [Pending Features](./docs/待实现功能.md)
- [Architecture Design](./docs/架构设计.md)
- [Database Design](./docs/数据库设计.md)
- [API Reference](./docs/接口文档.md)
- [Deployment Guide](./docs/部署运维.md)
- [Development Guide](./docs/开发指南.md)
- [Changelog](./docs/更新日志.md)
- [中文文档](./README.md)
