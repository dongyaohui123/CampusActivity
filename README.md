# CampusActivity 校园活动管理平台

一个基于微信小程序的校园活动管理平台，支持学生浏览/报名活动、组织者创建/管理活动、管理员审核活动。

## 技术栈

| 层级 | 技术 | 版本 |
|------|------|------|
| 前端 | 微信小程序原生框架 | — |
| UI 组件 | [Vant Weapp](https://vant-ui.github.io/vant-weapp/) | 1.11.7 |
| 后端 | Spring Boot | 3.3.5 |
| ORM | MyBatis-Plus | 3.5.7 |
| 数据库 | MySQL | 8.x |
| JDK | Java | 17+ |
| 构建 | Maven | 3.x |

## 功能模块

### 学生端
- 活动浏览：按分类、时间、地点、状态筛选活动列表
- 活动报名：报名/取消报名，查看报名记录
- 个人中心：编辑资料、上传头像、修改密码、查看统计数据

### 组织者端
- 活动管理：创建、编辑、发布活动
- 审核提交：将活动提交管理员审核
- 报名管理：查看活动报名用户列表

### 管理员端
- 审核面板：审核待审批活动，通过或驳回（附理由）
- 操作审计：所有审核操作记录在 `activity_audit_logs` 表中

### 认证
- 用户名 + 密码登录/注册
- 登录后自主修改密码
- 微信小程序 OAuth 一键登录（`wx.login` → `openid`）

## 项目结构

```
CampusActivity/
├── app.js                    # 小程序入口
├── app.json                  # 全局配置（页面路由、Vant 组件注册）
├── app.wxss                  # 全局样式
├── config.js                 # 环境配置（local / lan 切换后端地址）
├── package.json              # 前端依赖
├── project.config.json       # 微信开发者工具配置
│
├── pages/                    # 小程序页面
│   ├── index/                #   首页（轮播、快捷入口、推荐活动）
│   ├── activity-list/        #   活动搜索/筛选列表
│   ├── activity-detail/      #   活动详情与报名
│   ├── avatar-crop/          #   头像裁剪
│   ├── auth/                 #   登录/注册
│   ├── change-password/      #   修改密码
│   ├── mine/                 #   个人中心（资料、统计、角色面板）
│   ├── my-registrations/     #   我的报名
│   ├── organizer-activities/ #   组织者活动管理
│   └── admin-review/         #   管理员审核面板
│
├── utils/                    # 前端工具模块
│   ├── api.js                #   API 调用封装
│   ├── request.js            #   统一请求封装（自动注入操作者上下文）
│   ├── auth.js               #   登录态管理
│   └── operator-context.js   #   操作者上下文（userId + role）
│
└── backend/                  # Spring Boot 后端
    ├── pom.xml
    └── src/main/
        ├── java/com/campus/activity/
        │   ├── CampusActivityBackendApplication.java
        │   ├── common/               # ApiResponse、ErrorCode、PageResponse
        │   ├── config/               # MyBatis-Plus、静态资源配置
        │   ├── controller/v1/        # 6 个 REST 控制器
        │   ├── dto/                  # 请求 DTO
        │   ├── entity/               # 13 个实体类
        │   ├── entity/view/          # 4 个视图投影
        │   ├── enums/                # 12 个枚举类型
        │   ├── exception/            # 业务异常 + 全局异常处理
        │   ├── mapper/               # 13 个 MyBatis-Plus Mapper
        │   ├── service/              # 服务接口
        │   ├── service/impl/v1/      # V1 服务实现
        │   └── view/v1/              # 5 个响应视图
        └── resources/
            ├── application.yml       # 主配置
            └── mapper/               # 13 个 XML 映射文件
```

## 快速开始

### 环境要求

- JDK 17+
- Maven 3.x
- MySQL 8.x
- [微信开发者工具](https://developers.weixin.qq.com/miniprogram/dev/devtools/download.html)

### 1. 数据库

创建 MySQL 数据库：

```sql
CREATE DATABASE campus_activity_v2 CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

> 注意：项目不包含 DDL 脚本，请联系项目管理员获取数据库初始化脚本。实体类位于 `backend/src/main/java/com/campus/activity/entity/`，可作为建表参考。

### 2. 后端

```bash
cd backend

# 配置环境变量（可选，覆盖 application.yml 默认值）
export DB_URL="jdbc:mysql://localhost:3306/campus_activity_v2?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true"
export DB_USERNAME="root"
export DB_PASSWORD="your_password"
export WECHAT_MINIAPP_APPID="your_appid"
export WECHAT_MINIAPP_SECRET="your_secret"

# 编译
mvn clean package -DskipTests

# 启动（默认端口 8080）
mvn spring-boot:run
```

### 3. 前端（微信小程序）

1. 用微信开发者工具打开项目根目录
2. 编辑 `config.js` 切换环境：
   - `"local"`：本机调试，后端地址 `http://127.0.0.1:8080`
   - `"lan"`：局域网真机调试，后端地址 `http://192.168.5.32:8080`
3. 在微信开发者工具中执行：`工具 → 构建 npm`
4. 点击编译运行

## API 文档

所有 API 以 `/api/v1/` 为前缀。响应格式统一为：

```json
{
  "code": 0,
  "message": "success",
  "data": {}
}
```

### 认证 `POST /api/v1/auth`

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/login` | 用户名密码登录 |
| POST | `/register` | 新用户注册 |
| POST | `/wechat-login` | 微信小程序登录 |

### 公开活动 `GET /api/v1/activities`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/` | 活动列表（支持 keyword、category、time、status 筛选） |
| GET | `/{id}` | 活动详情 |

### 报名 `POST|PUT /api/v1/registrations`

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/` | 报名活动 |
| PUT | `/{id}/cancel` | 取消报名 |

### 组织者 `* /api/v1/organizer/activities`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/` | 我的活动列表 |
| POST | `/` | 创建活动 |
| PUT | `/{id}` | 更新活动 |
| POST | `/{id}/submit-review` | 提交审核 |
| GET | `/{id}/registrations` | 查看报名用户 |

### 用户 `GET|PUT /api/v1/user(s)`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/users/{id}` | 获取用户资料 |
| PUT | `/users/{id}/profile` | 更新资料 |
| POST | `/users/{id}/avatar` | 上传头像（multipart） |
| PUT | `/users/{id}/password` | 修改密码 |
| GET | `/users/{id}/registrations` | 我的报名记录 |

### 管理员 `* /api/v1/admin/reviews`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/pending` | 待审核列表 |
| POST | `/{id}/approve` | 通过审核 |
| POST | `/{id}/reject` | 驳回审核 |

## 安全模型

API 使用**操作者参数**模式进行认证授权。所有写操作和部分读操作需要在请求中携带 `operatorUserId` 和 `operatorRole` 查询参数。前端 `utils/request.js` 和 `utils/operator-context.js` 在登录后自动管理这些参数。

## 数据库核心表

| 表名 | 用途 |
|------|------|
| `users` | 用户账户（用户名、密码、openid、角色、状态） |
| `activities` | 活动（标题、内容、时间、地点、状态、可见性） |
| `activity_registrations` | 报名记录（已报名/已取消/已签到） |
| `activity_reviews` | 审核记录（待审核/已通过/已驳回） |
| `activity_categories` | 活动分类（树形结构） |
| `activity_category_rel` | 活动-分类多对多关联 |
| `activity_favorites` | 用户收藏 |
| `activity_audit_logs` | 审核操作审计日志（不可变） |
| `organizer_profiles` | 组织者资料扩展 |
| `student_profiles` | 学生资料扩展 |
| `organizer_follows` | 用户关注组织者 |
| `auth_tokens` | 认证令牌 |
| `login_logs` | 登录日志 |

## 配置说明

### 后端 `application.yml`

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `spring.datasource.url` | `jdbc:mysql://localhost:3306/campus_activity_v2` | 数据库连接 |
| `spring.datasource.username` | `root` | 数据库用户 |
| `spring.datasource.password` | `root` | 数据库密码 |
| `wechat.miniapp.appid` | `wx89bee6c990f52064` | 微信小程序 AppID |
| `wechat.miniapp.secret` | 已在仓库配置 | 微信小程序 Secret |
| `app.upload.avatar-dir` | `./uploads/avatars` | 头像上传目录 |
| `app.upload.avatar-url-prefix` | `/static/avatars` | 头像静态访问前缀 |
| `app.upload.avatar-max-size-kb` | `1024` | 头像最大上传大小 |
| `app.upload.public-base-url` | `http://127.0.0.1:8080` | 头像公开访问基地址 |

以上配置均支持通过环境变量覆盖（`DB_URL`、`DB_USERNAME`、`DB_PASSWORD` 等）。

### 前端 `config.js`

```js
const RUN_ENV = "local";  // "local" | "lan"
```

- `local`：后端地址 `http://127.0.0.1:8080`
- `lan`：后端地址 `http://192.168.5.32:8080`


