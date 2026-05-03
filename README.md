# CampusActivity 校园活动管理平台

基于微信小程序 + Spring Boot 的校园活动管理平台。当前仓库包含前端小程序、后端 API、数据库增量脚本、设计文档与辅助工具，适合作为后续开发和维护的统一工作区。

## 项目概览

- 前端：微信小程序原生框架，Vant Weapp 组件库
- 后端：Spring Boot 3、MyBatis-Plus、MySQL 8
- 运行入口：
  - 小程序仍以仓库根目录作为工程根目录打开
  - 后端位于 `backend/`
- 对外 API：统一以 `/api/v1/**` 为准，旧版 `/api/users`、`/api/activities` 入口已移除

## 功能特性

### 学生功能
- 浏览和搜索校园活动
- 活动报名与取消报名
- 查看报名记录和电子票
- 收藏感兴趣的活动
- 个人资料管理

### 组织者功能
- 创建和编辑活动
- 上传活动封面图片
- 管理活动报名名单
- 活动签到管理（扫码签到、手动核销）
- 活动管理员权限分配

### 管理员功能
- 活动审核（通过/驳回）
- 审核记录查看
- 平台内容管理

### 公共功能
- 微信登录与注册
- 个人资料维护
- 头像上传与裁剪
- 密码修改

## 技术栈

### 前端技术栈
- **框架**：微信小程序原生框架
- **UI组件库**：Vant Weapp 1.11.7
- **包管理**：npm
- **代码规范**：ESLint

### 后端技术栈
- **框架**：Spring Boot 3.3.5
- **ORM**：MyBatis-Plus 3.5.7
- **数据库**：MySQL 8.x
- **Java版本**：JDK 17
- **构建工具**：Maven 3.9+
- **其他依赖**：
  - Lombok：简化Java代码
  - Google ZXing：二维码生成
  - Spring Validation：数据校验

## 架构说明

### 前端

- `pages/`：小程序页面路由与页面逻辑
- `utils/request.js`：统一请求封装，处理操作人上下文、业务码与错误提示
- `utils/api.js`：对页面暴露的聚合 API 入口
- `utils/api-modules/`：按领域拆分的 API 封装
  - `auth.js`：认证相关接口（登录、注册）
  - `activities.js`：活动相关接口（列表、详情、评论、收藏）
  - `registrations.js`：报名相关接口（报名、取消、记录）
  - `users.js`：用户相关接口（资料、头像、密码）
  - `organizer.js`：组织者相关接口（活动管理、签到、管理员）
  - `admin.js`：管理员相关接口（审核）
- `assets/`：原始设计资源与图标素材
- `static/`：小程序静态资源

### 后端

- `backend/src/main/java/com/campus/activity/controller/v1/`：唯一对外控制器入口
  - `V1AuthController`：认证控制器（登录、注册）
  - `V1ActivityController`：活动控制器（列表、详情、评论、收藏）
  - `V1RegistrationController`：报名控制器（报名、取消、记录）
  - `V1UserController`：用户控制器（资料、头像、密码）
  - `V1OrganizerActivityController`：组织者活动控制器（活动管理、签到）
  - `V1AdminReviewController`：管理员审核控制器
- `backend/src/main/java/com/campus/activity/service/v1/`：V1 业务服务接口
- `backend/src/main/java/com/campus/activity/service/impl/v1/`：V1 业务实现
- `backend/src/main/java/com/campus/activity/entity/`：数据库实体类
- `backend/src/main/java/com/campus/activity/dto/`：数据传输对象
- `backend/src/main/java/com/campus/activity/common/`：通用类（ApiResponse、ErrorCode等）
- `backend/src/main/java/com/campus/activity/config/`：配置类
- `backend/src/main/resources/mapper/`：MyBatis XML
- `backend/sql/`：数据库增量脚本
- `backend/uploads/`：运行时上传目录，仓库只保留空目录占位

## 数据库设计

### 主要数据表
- `user`：用户表（学生、组织者、管理员）
- `activity`：活动表（活动信息、状态、时间）
- `registration`：报名表（用户报名记录）
- `activity_manager`：活动管理员表（活动级权限）
- `activity_comment`：活动评论表
- `activity_favorite`：活动收藏表

### 数据库脚本
数据库增量脚本位于 `backend/sql/` 目录：
- `20260427_feature_scancode.sql`：扫码签到功能
- `20260428_feature_permission.sql`：权限管理功能
- `20260430_feature_comment.sql`：评论功能

## 目录结构

```text
CampusActivity/
├── app.js                          # 小程序入口文件，全局逻辑
├── app.json                        # 小程序配置文件（页面路由、组件注册、窗口样式）
├── app.wxss                        # 小程序全局样式
├── config.js                       # 运行环境配置（local/lan 环境切换）
├── package.json                    # npm 依赖配置
├── sitemap.json                    # 小程序搜索配置
│
├── assets/                         # 原始设计资源与图标素材
│   └── icons/                      # 图标文件（SVG/PNG）
│
├── static/                         # 小程序静态资源（编译时复制到小程序包）
│   ├── css/                        # 公共样式文件
│   │   └── icon.wxss               # 图标字体样式
│   ├── home-banners/               # 首页轮播图
│   │   ├── banner-01.png
│   │   ├── banner-02.png
│   │   └── banner-03.png
│   ├── placeholders/               # 占位图
│   │   └── avatar-default.png      # 默认头像
│   ├── activity-placeholders/      # 活动占位图
│   └── generated-activity-covers/  # 生成的活动封面（运行时）
│
├── pages/                          # 小程序页面目录（每个页面包含 .js/.json/.wxml/.wxss）
│   ├── index/                      # 首页（推荐活动、轮播图、快捷入口）
│   ├── activity-list/              # 活动列表页（筛选、搜索、分页）
│   ├── activity-detail/            # 活动详情页（报名、收藏、评论、分享）
│   ├── auth/                       # 登录/注册页
│   ├── mine/                       # 个人中心页
│   ├── my-registrations/           # 我的报名记录页
│   ├── my-favorites/               # 我的收藏页
│   ├── ticket/                     # 电子票详情页（二维码展示）
│   ├── avatar-crop/                # 头像裁剪页
│   ├── change-password/            # 修改密码页
│   ├── organizer-activities/       # 组织者活动管理页
│   ├── activity-managers/          # 活动管理员设置页
│   ├── managed-activities/         # 被管理的活动列表页
│   ├── managed-registrations/      # 被管理活动的报名名单页
│   ├── organizer-checkin/          # 签到管理页（扫码/手动核销）
│   └── admin-review/               # 管理员审核页
│
├── utils/                          # 工具函数目录
│   ├── api.js                      # API 聚合入口（统一导出所有接口）
│   ├── api-modules/                # 按领域拆分的 API 模块
│   │   ├── auth.js                 # 认证接口（登录、注册）
│   │   ├── activities.js           # 活动接口（列表、详情、评论、收藏）
│   │   ├── registrations.js        # 报名接口（报名、取消、记录）
│   │   ├── users.js                # 用户接口（资料、头像、密码）
│   │   ├── organizer.js            # 组织者接口（活动管理、签到、管理员）
│   │   ├── admin.js                # 管理员接口（审核）
│   │   └── shared.js               # 公共接口（配置、枚举）
│   ├── request.js                  # 统一请求封装（拦截器、错误处理、操作人上下文）
│   ├── auth.js                     # 认证工具（Token 管理、登录状态）
│   ├── avatar-url.js               # 头像 URL 处理工具
│   ├── feedback.js                 # 用户反馈工具（Toast、Modal）
│   ├── operator-context.js         # 操作人上下文工具
│   └── image-fallbacks.js          # 图片加载失败兜底处理
│
├── backend/                        # Spring Boot 后端项目
│   ├── pom.xml                     # Maven 依赖配置
│   ├── sql/                        # 数据库增量脚本
│   │   ├── 20260427_feature_scancode.sql    # 扫码签到功能
│   │   ├── 20260428_feature_permission.sql  # 权限管理功能
│   │   └── 20260430_feature_comment.sql     # 评论功能
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/campus/activity/
│   │   │   │   ├── CampusActivityBackendApplication.java  # Spring Boot 启动类
│   │   │   │   ├── common/                 # 通用类
│   │   │   │   │   ├── ApiResponse.java    # 统一响应封装
│   │   │   │   │   ├── ErrorCode.java      # 错误码定义
│   │   │   │   │   └── PageResponse.java   # 分页响应封装
│   │   │   │   ├── config/                 # 配置类
│   │   │   │   │   ├── DatabaseSchemaInitializer.java  # 数据库初始化
│   │   │   │   │   ├── MybatisPlusConfig.java          # MyBatis-Plus 配置
│   │   │   │   │   ├── StaticResourceConfig.java       # 静态资源配置
│   │   │   │   │   └── UploadProperties.java           # 上传配置属性
│   │   │   │   ├── controller/             # 控制器层
│   │   │   │   │   └── v1/                 # V1 版本控制器
│   │   │   │   │       ├── V1AuthController.java               # 认证控制器
│   │   │   │   │       ├── V1ActivityController.java           # 活动控制器
│   │   │   │   │       ├── V1RegistrationController.java       # 报名控制器
│   │   │   │   │       ├── V1UserController.java               # 用户控制器
│   │   │   │   │       ├── V1OrganizerActivityController.java  # 组织者活动控制器
│   │   │   │   │       └── V1AdminReviewController.java        # 管理员审核控制器
│   │   │   │   ├── dto/                    # 数据传输对象
│   │   │   │   │   ├── activity/           # 活动相关 DTO
│   │   │   │   │   ├── user/               # 用户相关 DTO
│   │   │   │   │   └── v1/                 # V1 版本 DTO
│   │   │   │   │       ├── activity/       # 活动 DTO
│   │   │   │   │       ├── auth/           # 认证 DTO
│   │   │   │   │       ├── registration/   # 报名 DTO
│   │   │   │   │       ├── review/         # 审核 DTO
│   │   │   │   │       └── user/           # 用户 DTO
│   │   │   │   ├── entity/                 # 数据库实体类
│   │   │   │   │   ├── User.java           # 用户实体
│   │   │   │   │   ├── Activity.java       # 活动实体
│   │   │   │   │   ├── ActivityRegistration.java  # 报名实体
│   │   │   │   │   ├── ActivityComment.java       # 评论实体
│   │   │   │   │   ├── ActivityFavorite.java      # 收藏实体
│   │   │   │   │   ├── ActivityManagerPermissionGrant.java  # 活动管理员权限
│   │   │   │   │   └── view/               # 视图实体（查询结果映射）
│   │   │   │   ├── enums/                  # 枚举类
│   │   │   │   │   ├── UserRole.java       # 用户角色（STUDENT/ORGANIZER/ADMIN）
│   │   │   │   │   ├── ActivityStatus.java # 活动状态
│   │   │   │   │   ├── RegistrationStatus.java  # 报名状态
│   │   │   │   │   └── ...                 # 其他枚举
│   │   │   │   ├── exception/              # 异常处理
│   │   │   │   │   ├── BusinessException.java       # 业务异常
│   │   │   │   │   └── GlobalExceptionHandler.java  # 全局异常处理器
│   │   │   │   ├── mapper/                 # MyBatis Mapper 接口
│   │   │   │   ├── service/                # 服务层
│   │   │   │   │   ├── v1/                 # V1 版本服务接口
│   │   │   │   │   └── impl/               # 服务实现
│   │   │   │   │       └── v1/             # V1 版本服务实现
│   │   │   │   └── view/                   # 视图对象（响应数据）
│   │   │   │       └── v1/                 # V1 版本视图对象
│   │   │   └── resources/
│   │   │       ├── application.yml          # Spring Boot 配置文件
│   │   │       └── mapper/                  # MyBatis XML 映射文件
│   │   └── test/                            # 测试代码
│   │       └── java/com/campus/activity/
│   │           ├── controller/              # 控制器测试
│   │           ├── service/                 # 服务测试
│   │           └── config/                  # 配置测试
│   └── uploads/                             # 运行时上传目录（仓库只保留 .gitkeep）
│       ├── activity-covers/.gitkeep         # 活动封面上传目录
│       └── avatars/.gitkeep                 # 头像上传目录
│
├── docs/                           # 项目文档目录
│   ├── README.md                   # 文档索引
│   ├── system-function-module-diagram.md  # 系统功能模块设计图（Mermaid）
│   ├── images/                     # 文档图片
│   │   ├── campus-activity-function-admin.png      # 管理员功能图
│   │   ├── campus-activity-function-common.png     # 公共功能图
│   │   ├── campus-activity-function-organizer.png  # 组织者功能图
│   │   └── campus-activity-function-student.png    # 学生功能图
│   └── archive/                    # 历史归档
│       └── mimo/                   # Mimo 参考产物
│           ├── README.md           # 说明文档
│           ├── mimo_claude_doc.html
│           ├── mimo_main.js
│           └── mimo_search_chunk.js
│
└── tools/                          # 辅助工具目录
    ├── backend/                    # 后端工具
    │   └── generate_sql_mapping_doc.ps1  # SQL 映射文档生成脚本
    └── docs/                       # 文档工具
        ├── generate_er_diagram.py  # ER 图生成脚本
        └── render_function_diagrams.py  # 功能图渲染脚本
```

## 快速启动

### 环境要求

- Node.js 18+ 或微信开发者工具默认环境
- JDK 17+
- Maven 3.9+
- MySQL 8.x
- [微信开发者工具](https://developers.weixin.qq.com/miniprogram/dev/devtools/download.html)

### 1. 初始化数据库

创建数据库：

```sql
CREATE DATABASE campus_activity_v2
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;
```

说明：

- 当前仓库保留的是增量脚本，位于 `backend/sql/`
- 表结构可结合 `backend/src/main/java/com/campus/activity/entity/` 里的实体类核对

### 2. 启动后端

```bash
cd backend
mvn clean package -DskipTests
mvn spring-boot:run
```

默认端口为 `8080`。

### 3. 打开小程序

1. 用微信开发者工具直接打开仓库根目录
2. 检查 `config.js` 中的运行环境：
   - `local`：本机联调，默认 `http://127.0.0.1:8080`
   - `lan`：局域网真机调试
3. 在微信开发者工具执行 `工具 -> 构建 npm`
4. 编译并运行

## API 接口文档

### 认证接口 (`/api/v1/auth`)
- `POST /api/v1/auth/login`：用户登录
- `POST /api/v1/auth/register`：用户注册

### 活动接口 (`/api/v1/activities`)
- `GET /api/v1/activities`：获取活动列表
- `GET /api/v1/activities/{id}`：获取活动详情
- `GET /api/v1/activities/manageable`：获取可管理的活动列表
- `POST /api/v1/activities/{id}/comments`：添加活动评论
- `GET /api/v1/activities/{id}/comments`：获取活动评论列表
- `DELETE /api/v1/activities/{id}/comments/{commentId}`：删除活动评论
- `GET /api/v1/activities/{id}/favorite`：获取收藏状态
- `POST /api/v1/activities/{id}/favorite`：收藏活动
- `DELETE /api/v1/activities/{id}/favorite`：取消收藏

### 报名接口 (`/api/v1/registrations`)
- `POST /api/v1/registrations`：活动报名
- `DELETE /api/v1/registrations/{id}`：取消报名
- `GET /api/v1/registrations/my`：获取我的报名记录

### 用户接口 (`/api/v1/users`)
- `GET /api/v1/users/me`：获取当前用户信息
- `PUT /api/v1/users/me`：更新用户信息
- `POST /api/v1/users/me/avatar`：上传头像
- `PUT /api/v1/users/me/password`：修改密码

### 组织者接口 (`/api/v1/organizer/activities`)
- `POST /api/v1/organizer/activities`：创建活动
- `PUT /api/v1/organizer/activities/{id}`：更新活动
- `DELETE /api/v1/organizer/activities/{id}`：删除活动
- `GET /api/v1/organizer/activities/{id}/registrations`：获取活动报名名单
- `POST /api/v1/organizer/activities/{id}/checkin`：签到
- `GET /api/v1/organizer/activities/{id}/managers`：获取活动管理员列表
- `POST /api/v1/organizer/activities/{id}/managers`：添加活动管理员
- `DELETE /api/v1/organizer/activities/{id}/managers/{userId}`：移除活动管理员

### 管理员接口 (`/api/v1/admin/reviews`)
- `GET /api/v1/admin/reviews/pending`：获取待审核活动列表
- `POST /api/v1/admin/reviews/{id}/approve`：审核通过
- `POST /api/v1/admin/reviews/{id}/reject`：审核驳回

## 环境变量

后端支持用环境变量覆盖默认配置：

| 变量名 | 说明 | 默认值 |
| --- | --- | --- |
| `DB_URL` | MySQL JDBC 地址 | `jdbc:mysql://localhost:3306/campus_activity_v2...` |
| `DB_USERNAME` | 数据库用户名 | `root` |
| `DB_PASSWORD` | 数据库密码 | `root` |
| `WECHAT_MINIAPP_APPID` | 小程序 AppID | `application.yml` 中的默认值 |
| `WECHAT_MINIAPP_SECRET` | 小程序 Secret | `application.yml` 中的默认值 |
| `APP_UPLOAD_AVATAR_DIR` | 头像上传目录 | `./uploads/avatars` |
| `APP_UPLOAD_AVATAR_URL_PREFIX` | 头像静态访问前缀 | `/static/avatars` |
| `APP_UPLOAD_ACTIVITY_COVER_DIR` | 活动封面上传目录 | `./uploads/activity-covers` |
| `APP_UPLOAD_ACTIVITY_COVER_URL_PREFIX` | 活动封面静态访问前缀 | `/static/activity-covers` |
| `APP_UPLOAD_PUBLIC_BASE_URL` | 上传文件公网基地址 | `http://127.0.0.1:8080` |

## 开发约定

### 前端开发规范
- 页面继续通过 `require("../../utils/api")` 使用 API；如需新增接口，优先加到 `utils/api-modules/` 中，再由 `utils/api.js` 聚合导出
- 小程序工程根目录保持不变，不要把 `app.*`、`pages/`、`static/` 挪到子目录
- 使用 Vant Weapp 组件库，保持 UI 风格一致
- 页面文件结构：每个页面包含 `.js`、`.json`、`.wxml`、`.wxss` 四个文件

### 后端开发规范
- 后端新增接口统一放在 `backend/src/main/java/com/campus/activity/controller/v1/`
- 遵循 RESTful API 设计规范
- 使用 MyBatis-Plus 进行数据库操作
- 使用 Lombok 简化 Java 代码
- 统一使用 `ApiResponse` 封装响应结果

### 代码提交规范
- 运行产物不要提交：
  - `backend/uploads/` 下的实际上传文件
  - `backend/*.log`
  - `.idea/`、`.vscode/`、`.claude/`、`.playwright-cli/`、`tmp/` 等本地目录
- 如需补充文档生成或一次性脚本，统一放到 `tools/`
- 提交信息应简洁明了，说明改动内容

## 测试与验证

### 后端测试

```bash
cd backend
mvn test
```

### 前端联调建议

至少验证以下路径：

- 首页加载
- 登录 / 注册
- 活动列表与详情
- 报名 / 取消报名
- 个人中心资料修改
- 组织者活动管理与签到
- 管理员审核

### 测试账号
系统预置了以下测试账号（密码均为 `123456`）：
- 学生账号：`student1`
- 组织者账号：`organizer1`
- 管理员账号：`admin1`

## 部署说明

### 开发环境部署
1. 克隆项目到本地
2. 初始化数据库（参考快速启动章节）
3. 启动后端服务
4. 使用微信开发者工具打开小程序项目
5. 配置 `config.js` 中的环境变量

### 生产环境部署
1. 配置生产环境数据库
2. 设置环境变量（参考环境变量章节）
3. 打包后端应用：`mvn clean package -DskipTests`
4. 部署后端 JAR 文件
5. 配置小程序生产环境域名
6. 提交小程序审核

## 文档索引

- [文档目录](./docs/README.md)
- [系统功能模块设计图](./docs/system-function-module-diagram.md)
- [Mimo 参考产物归档](./docs/archive/mimo/README.md)

## 贡献指南

### 如何贡献
1. Fork 本项目
2. 创建功能分支：`git checkout -b feature/your-feature`
3. 提交更改：`git commit -m 'Add some feature'`
4. 推送到分支：`git push origin feature/your-feature`
5. 创建 Pull Request

### 代码规范
- 前端遵循微信小程序开发规范
- 后端遵循 Java 开发规范
- 提交信息应简洁明了

### 问题反馈
- 使用 GitHub Issues 报告问题
- 提供详细的问题描述和复现步骤
- 包含相关日志和截图

## 常见问题

### 1. 微信开发者工具打开后提示找不到页面？

请确认打开的是仓库根目录，而不是 `backend/` 或其他子目录。

### 2. 图片上传后访问不到？

先确认后端已启动，并检查：

- `APP_UPLOAD_*_DIR` 是否指向实际存在且可写的目录
- `APP_UPLOAD_*_URL_PREFIX` 是否与 `StaticResourceConfig` 的静态映射一致
- 小程序访问域名是否已加入微信开发者工具白名单

### 3. 为什么仓库里只有 `.gitkeep`，没有示例上传图片？

`backend/uploads/` 属于运行时目录。仓库只保留空目录结构，真实上传文件应由本地或部署环境动态生成。

### 4. 为什么根目录没有历史抓取脚本了？

历史参考产物已经归档到 `docs/archive/mimo/`，辅助脚本统一收拢到 `tools/`，避免污染工程主路径。

## 许可证

本项目采用 MIT 许可证 - 详见 [LICENSE](LICENSE) 文件

## 联系方式

- 项目维护者：[dongyaohui123](https://github.com/dongyaohui123)
- 项目地址：[CampusActivity](https://github.com/dongyaohui123/CampusActivity)
