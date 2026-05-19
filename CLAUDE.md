# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目概述

校园活动管理平台，前端为微信/QQ 小程序原生框架 + Vant Weapp，后端为 Spring Boot 3 + MyBatis-Plus + MySQL 8。仓库根目录即小程序工程根目录，后端代码在 `backend/`。

## 常用命令

### 后端

```bash
cd backend
mvn clean package -DskipTests    # 打包
mvn spring-boot:run              # 启动（默认端口 8080）
mvn test                         # 运行全部测试
mvn test -Dtest=ClassName        # 运行单个测试类
mvn test -Dtest=ClassName#method # 运行单个测试方法
```

### 前端

前端无构建命令，用微信开发者工具直接打开仓库根目录。npm 依赖需在开发者工具中执行「工具 -> 构建 npm」。

### 环境切换

修改 `config.js` 中的 `RUN_ENV`：`local`（本机 127.0.0.1:8080）或 `lan`（局域网真机调试，需同步修改 IP）。

## 架构要点

### API 版本化

所有对外接口统一走 `/api/v1/**`，控制器在 `backend/src/main/java/com/campus/activity/controller/v1/`。旧版入口已移除。

### 前端请求链路

页面 → `utils/api.js`（聚合导出）→ `utils/api-modules/*.js`（按领域拆分）→ `utils/request.js`（统一请求封装）。`request.js` 自动处理：操作人上下文注入（写操作）、业务码判断（code !== 0 为失败）、错误提示。

新增接口优先加到 `utils/api-modules/` 对应模块，再由 `utils/api.js` 导出。

关键工具函数：
- `utils/operator-context.js`：管理操作人身份（operatorUserId + operatorRole），写操作自动拼接到 query 参数
- `utils/runtime-api.js`：多平台运行时 API 抽象层（微信/QQ小程序适配）
- `utils/feedback.js`：统一用户反馈（Toast/Modal）
- `utils/auth.js`：Token 管理与登录状态

### 后端分层

Controller → Service 接口（`service/v1/`）→ Service 实现（`service/impl/v1/`）→ Mapper（MyBatis-Plus）。响应统一用 `ApiResponse` 封装，业务异常用 `BusinessException` + `ErrorCode`。

ErrorCode 定义：`SUCCESS(0)`、`BAD_REQUEST(40000)`、`VALIDATION_ERROR(40001)`、`FORBIDDEN(40300)`、`NOT_FOUND(40400)`、`CONFLICT(40900)`、`DATA_INTEGRITY_ERROR(40901)`、`INTERNAL_ERROR(50000)`。

### 数据库

数据库名 `campus_activity_v2`，增量脚本在 `backend/sql/`。表结构可结合 `entity/` 实体类核对。MyBatis XML 映射在 `resources/mapper/`。

### 角色与权限

三种角色：STUDENT（学生）、ORGANIZER（组织者）、ADMIN（管理员）。活动级权限通过 `activity_manager` 表控制。操作人上下文通过前端 `operator-context.js` 在请求 query 中传递。

### 测试

后端测试在 `backend/src/test/java/com/campus/activity/`：
- `service/`：Service 层单元测试（Mock Mapper）
- `controller/`：Controller 集成测试（`V1ApiControllerIntegrationTest`）

## 开发规范

- 后端新增接口只放 `controller/v1/`，遵循 RESTful + `ApiResponse` 统一响应
- 前端页面文件结构：每个页面 `.js` / `.json` / `.wxml` / `.wxss` 四件套
- 小程序工程根目录（`app.*`、`pages/`、`static/`）不要挪到子目录
- `backend/uploads/` 只保留 `.gitkeep`，不提交实际上传文件
- 辅助脚本放 `tools/`，文档放 `docs/`
- 微信和 QQ 小程序共享同一代码库，通过 `runtime-api.js` 做平台适配

## 测试账号

密码均为 `123456`：学生 `student1`、组织者 `organizer1`、管理员 `admin1`。
