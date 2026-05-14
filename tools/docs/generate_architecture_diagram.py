#!/usr/bin/env python3
"""生成校园活动管理平台项目架构图"""

import matplotlib
matplotlib.use('Agg')
import matplotlib.pyplot as plt
import matplotlib.patches as mpatches
from matplotlib.patches import FancyBboxPatch, FancyArrowPatch
import numpy as np

# ─── 全局配置 ──────────────────────────────────────────────────────────────────
fig, ax = plt.subplots(figsize=(28, 22))
ax.set_xlim(0, 28)
ax.set_ylim(0, 22)
ax.set_aspect('equal')
ax.axis('off')
fig.patch.set_facecolor('#F8FAFC')

# 颜色方案
COLORS = {
    'title_bg': '#1E3A5F',
    'frontend': '#E8F4FD',
    'frontend_border': '#3498DB',
    'frontend_header': '#2980B9',
    'utils': '#FFF3E0',
    'utils_border': '#F39C12',
    'utils_header': '#E67E22',
    'api': '#E8F8E8',
    'api_border': '#27AE60',
    'api_header': '#229954',
    'controller': '#FDE8F0',
    'controller_border': '#E74C8B',
    'controller_header': '#C0392B',
    'service': '#F3E8FD',
    'service_border': '#9B59B6',
    'service_header': '#8E44AD',
    'mapper': '#FDF8E8',
    'mapper_border': '#F1C40F',
    'mapper_header': '#D4AC0D',
    'db': '#E8FDF8',
    'db_border': '#1ABC9C',
    'db_header': '#16A085',
    'arrow': '#5D6D7E',
    'text_dark': '#2C3E50',
    'text_light': '#FFFFFF',
    'subtitle': '#7F8C8D',
    'flow_bg': '#FDF2F8',
    'flow_border': '#EC4899',
    'flow_header': '#DB2777',
}

def draw_rounded_box(x, y, w, h, color, border_color, linewidth=2, alpha=0.95, radius=0.15):
    box = FancyBboxPatch((x, y), w, h, boxstyle=f"round,pad={radius}",
                         facecolor=color, edgecolor=border_color,
                         linewidth=linewidth, alpha=alpha, zorder=2)
    ax.add_patch(box)

def draw_header_box(x, y, w, h, color, text, fontsize=11, fontcolor='white'):
    box = FancyBboxPatch((x, y), w, h, boxstyle="round,pad=0.08",
                         facecolor=color, edgecolor=color,
                         linewidth=0, alpha=0.95, zorder=3)
    ax.add_patch(box)
    ax.text(x + w/2, y + h/2, text, ha='center', va='center',
            fontsize=fontsize, fontweight='bold', color=fontcolor, zorder=4,
            fontfamily='Microsoft YaHei')

def draw_item_box(x, y, w, h, color, text, fontsize=8.5, fontcolor='#2C3E50', border_color=None):
    bc = border_color or color
    box = FancyBboxPatch((x, y), w, h, boxstyle="round,pad=0.06",
                         facecolor=color, edgecolor=bc,
                         linewidth=1.2, alpha=0.85, zorder=3)
    ax.add_patch(box)
    ax.text(x + w/2, y + h/2, text, ha='center', va='center',
            fontsize=fontsize, fontweight='normal', color=fontcolor, zorder=4,
            fontfamily='Microsoft YaHei', wrap=True)

def draw_arrow(x1, y1, x2, y2, color='#5D6D7E', style='->', lw=1.8):
    ax.annotate('', xy=(x2, y2), xytext=(x1, y1),
                arrowprops=dict(arrowstyle=style, color=color, lw=lw,
                                connectionstyle='arc3,rad=0.05'), zorder=5)

def draw_label_arrow(x1, y1, x2, y2, label, color='#5D6D7E', fontsize=7.5):
    draw_arrow(x1, y1, x2, y2, color=color)
    mx, my = (x1+x2)/2, (y1+y2)/2
    ax.text(mx, my + 0.15, label, ha='center', va='bottom',
            fontsize=fontsize, color=color, fontweight='bold',
            fontfamily='Microsoft YaHei', zorder=6,
            bbox=dict(boxstyle='round,pad=0.15', facecolor='white', edgecolor='none', alpha=0.85))

# ─── 标题 ──────────────────────────────────────────────────────────────────────
title_box = FancyBboxPatch((6, 21.0), 16, 0.8, boxstyle="round,pad=0.1",
                           facecolor=COLORS['title_bg'], edgecolor=COLORS['title_bg'],
                           linewidth=0, zorder=3)
ax.add_patch(title_box)
ax.text(14, 21.4, '校园活动管理平台 — 系统架构图', ha='center', va='center',
        fontsize=20, fontweight='bold', color='white', zorder=4,
        fontfamily='Microsoft YaHei')
ax.text(14, 21.05, 'CampusActivity Management Platform — Architecture Overview',
        ha='center', va='center', fontsize=9, color='#A0B4C8', zorder=4,
        fontfamily='Microsoft YaHei', style='italic')

# ─── 第1层：前端展示层 ─────────────────────────────────────────────────────────
y1 = 18.8
draw_rounded_box(0.5, y1, 27, 2.6, COLORS['frontend'], COLORS['frontend_border'], linewidth=2.5)
draw_header_box(0.5, y1 + 2.1, 27, 0.5, COLORS['frontend_header'], '[前端展示层] 微信/QQ小程序 (WeChat/QQ Mini Program)', fontsize=12)

# 前端页面分组
page_groups = [
    ('学生端', ['首页 index', '活动列表', '活动详情', '个人中心', '我的报名', '电子票', '我的收藏']),
    ('组织者端', ['活动管理', '签到核验', '权限管理', '受管活动', '受管报名']),
    ('管理端', ['审核面板']),
    ('通用', ['登录注册', '头像裁剪', '修改密码']),
]

gx_positions = [1.0, 8.5, 16.5, 20.5]
group_widths = [7.0, 7.5, 3.5, 6.5]

for i, (gname, pages) in enumerate(page_groups):
    gx = gx_positions[i]
    gw = group_widths[i]
    draw_item_box(gx, y1 + 1.35, gw, 0.55, '#D6EAF8', gname,
                  fontsize=9, fontcolor=COLORS['frontend_header'], border_color=COLORS['frontend_border'])
    for j, page in enumerate(pages):
        px = gx + 0.1 + j * (gw - 0.2) / len(pages)
        pw = (gw - 0.2) / len(pages) - 0.1
        draw_item_box(px, y1 + 0.15, pw, 0.5, '#EBF5FB', page,
                      fontsize=6.5, fontcolor=COLORS['text_dark'])

# ─── 工具层 ────────────────────────────────────────────────────────────────────
y2 = 17.0
draw_rounded_box(0.5, y2, 27, 1.5, COLORS['utils'], COLORS['utils_border'], linewidth=2)
draw_header_box(0.5, y2 + 1.0, 27, 0.5, COLORS['utils_header'], '[工具与抽象层] utils/', fontsize=11)

utils_items = [
    'request.js\n统一请求封装',
    'api.js\n聚合导出',
    'auth.js\nToken管理',
    'operator-context.js\n操作人上下文',
    'runtime-api.js\n多平台适配',
    'feedback.js\n用户反馈',
]
for i, item in enumerate(utils_items):
    ux = 1.2 + i * 4.4
    draw_item_box(ux, y2 + 0.1, 3.8, 0.75, '#FEF5E7', item,
                  fontsize=7.5, fontcolor=COLORS['text_dark'], border_color=COLORS['utils_border'])

# ─── API模块层 ─────────────────────────────────────────────────────────────────
y3 = 15.2
draw_rounded_box(0.5, y3, 27, 1.5, COLORS['api'], COLORS['api_border'], linewidth=2)
draw_header_box(0.5, y3 + 1.0, 27, 0.5, COLORS['api_header'], '[API接口模块层] utils/api-modules/', fontsize=11)

api_modules = [
    'auth.js\n登录/注册/微信/QQ',
    'activities.js\n活动/评论/收藏',
    'registrations.js\n报名/票据/二维码',
    'users.js\n资料/头像/密码',
    'organizer.js\n活动CRUD/签到/权限',
    'admin.js\n审核/批准/驳回',
]
for i, mod in enumerate(api_modules):
    mx = 1.2 + i * 4.4
    draw_item_box(mx, y3 + 0.1, 3.8, 0.75, '#E8F8E8', mod,
                  fontsize=7.5, fontcolor=COLORS['text_dark'], border_color=COLORS['api_border'])

# 前端到工具层箭头
draw_arrow(14, y1, 14, y2 + 1.5, color=COLORS['frontend_border'])
ax.text(14.3, (y1 + y2 + 1.5)/2, '页面调用', fontsize=7, color=COLORS['frontend_border'],
        fontfamily='Microsoft YaHei', zorder=6)

# 工具层到API层箭头
draw_arrow(14, y2, 14, y3 + 1.5, color=COLORS['utils_border'])
ax.text(14.3, (y2 + y3 + 1.5)/2, '请求转发', fontsize=7, color=COLORS['utils_border'],
        fontfamily='Microsoft YaHei', zorder=6)

# ─── HTTP 分割线 ──────────────────────────────────────────────────────────────
y_http = 14.6
ax.plot([1, 27], [y_http, y_http], color='#E74C3C', linewidth=2, linestyle='--', zorder=5, alpha=0.6)
http_box = FancyBboxPatch((10.5, y_http - 0.22), 7, 0.44, boxstyle="round,pad=0.08",
                          facecolor='#FDEDEC', edgecolor='#E74C3C', linewidth=1.5, zorder=5)
ax.add_patch(http_box)
ax.text(14, y_http, 'HTTP/HTTPS  —  RESTful API  /api/v1/**',
        ha='center', va='center', fontsize=9, fontweight='bold',
        color='#C0392B', zorder=6, fontfamily='Microsoft YaHei')

# API层到HTTP层箭头
draw_arrow(14, y3, 14, y_http + 0.22, color=COLORS['api_border'])

# ─── 第2层：后端控制层 ─────────────────────────────────────────────────────────
y4 = 12.8
draw_rounded_box(0.5, y4, 27, 1.6, COLORS['controller'], COLORS['controller_border'], linewidth=2.5)
draw_header_box(0.5, y4 + 1.1, 27, 0.5, COLORS['controller_header'], '[控制器层 Controller] controller/v1/', fontsize=11)

controllers = [
    'V1AuthController\n/api/v1/auth',
    'V1ActivityController\n/api/v1/activities',
    'V1RegistrationController\n/api/v1/registrations',
    'V1UserController\n/api/v1/users',
    'V1OrganizerActivity\nController\n/api/v1/organizer/*',
    'V1AdminReviewController\n/api/v1/admin/reviews',
]
for i, ctrl in enumerate(controllers):
    cx = 1.0 + i * 4.5
    draw_item_box(cx, y4 + 0.08, 4.0, 0.85, '#FDE8F0', ctrl,
                  fontsize=7.5, fontcolor=COLORS['text_dark'], border_color=COLORS['controller_border'])

# HTTP到Controller箭头
draw_arrow(14, y_http - 0.22, 14, y4 + 1.1, color='#C0392B')

# ─── 第3层：服务层 ─────────────────────────────────────────────────────────────
y5 = 10.5
draw_rounded_box(0.5, y5, 27, 2.0, COLORS['service'], COLORS['service_border'], linewidth=2.5)
draw_header_box(0.5, y5 + 1.5, 27, 0.5, COLORS['service_header'], '[服务层 Service] service/v1/ + service/impl/v1/', fontsize=11)

# 核心服务
core_services = [
    'V1AuthService\n认证服务',
    'V1PublicActivityService\n活动公共服务',
    'V1RegistrationService\n报名服务',
    'V1UserService\n用户服务',
    'V1OrganizerActivity\nService\n组织者活动服务',
    'V1AdminReviewService\n审核服务',
]

# 支撑服务
support_services = [
    'V1ActivityComment\nService\n评论服务',
    'V1ActivityFavorite\nService\n收藏服务',
    'OperatorPermission\nService\n权限验证',
    'ActivityPhaseResolver\n活动阶段解析',
    'AvatarStorageService\n头像存储',
    'ActivityCoverStorage\nService\n封面存储',
]

for i, svc in enumerate(core_services):
    sx = 1.0 + i * 4.5
    draw_item_box(sx, y5 + 0.8, 4.0, 0.55, '#F3E8FD', svc,
                  fontsize=7, fontcolor=COLORS['text_dark'], border_color=COLORS['service_border'])

for i, svc in enumerate(support_services):
    sx = 1.0 + i * 4.5
    draw_item_box(sx, y5 + 0.08, 4.0, 0.55, '#F5EEF8', svc,
                  fontsize=6.5, fontcolor='#7D3C98', border_color=COLORS['service_border'])

# Controller到Service箭头
draw_arrow(14, y4, 14, y5 + 1.5, color=COLORS['controller_border'])
ax.text(14.3, (y4 + y5 + 1.5)/2, '业务调用', fontsize=7, color=COLORS['controller_border'],
        fontfamily='Microsoft YaHei', zorder=6)

# ─── 第4层：数据访问层 ─────────────────────────────────────────────────────────
y6 = 8.3
draw_rounded_box(0.5, y6, 27, 1.9, COLORS['mapper'], COLORS['mapper_border'], linewidth=2.5)
draw_header_box(0.5, y6 + 1.4, 27, 0.5, COLORS['mapper_header'], '[数据访问层 Mapper] mapper/ + resources/mapper/', fontsize=11)

mappers_group1 = [
    'UserMapper',
    'ActivityMapper',
    'ActivityRegistration\nMapper',
    'ActivityReviewMapper',
    'ActivityAuditLog\nMapper',
    'ActivityCategory\nMapper',
]

mappers_group2 = [
    'ActivityCategory\nRelMapper',
    'ActivityComment\nMapper',
    'ActivityFavorite\nMapper',
    'ActivityManagerPermission\nGrantMapper',
    'AuthTokenMapper',
    'LoginLogMapper',
    'OrganizerProfile\nMapper',
    'OrganizerFollowMapper',
    'StudentProfileMapper',
    'LocationCampusMapping\nMapper',
]

for i, mapper in enumerate(mappers_group1):
    mx = 1.0 + i * 4.5
    draw_item_box(mx, y6 + 0.72, 4.0, 0.52, '#FDF8E8', mapper,
                  fontsize=7, fontcolor=COLORS['text_dark'], border_color=COLORS['mapper_border'])

# 第二行 mappers 用更小的间距
for i, mapper in enumerate(mappers_group2):
    mx = 1.0 + i * 1.7
    draw_item_box(mx, y6 + 0.05, 1.55, 0.52, '#FEF9E7', mapper,
                  fontsize=5.5, fontcolor='#7D6608', border_color=COLORS['mapper_border'])

# Service到Mapper箭头
draw_arrow(14, y5, 14, y6 + 1.4, color=COLORS['service_border'])
ax.text(14.3, (y5 + y6 + 1.4)/2, '数据操作', fontsize=7, color=COLORS['service_border'],
        fontfamily='Microsoft YaHei', zorder=6)

# ─── 第5层：数据库层 ───────────────────────────────────────────────────────────
y7 = 5.5
draw_rounded_box(0.5, y7, 27, 2.5, COLORS['db'], COLORS['db_border'], linewidth=2.5)
draw_header_box(0.5, y7 + 2.0, 27, 0.5, COLORS['db_header'], '[数据库层] MySQL 8.0  campus_activity_v2', fontsize=11)

# 用户相关表
user_tables = ['users\n用户表', 'student_profiles\n学生档案', 'organizer_profiles\n组织者档案',
               'auth_tokens\n认证令牌', 'login_logs\n登录日志']

# 活动相关表
activity_tables = ['activities\n活动主表', 'activity_categories\n活动分类',
                   'activity_category_rel\n分类关联', 'activity_reviews\n审核记录',
                   'activity_audit_logs\n审计日志']

# 报名与互动表
reg_tables = ['activity_registrations\n报名记录', 'activity_favorites\n收藏记录',
              'activity_comments\n评论记录', 'activity_manager_permissions\n管理权限',
              'organizer_follows\n关注关系', 'location_campus_mapping\n校区映射']

for i, t in enumerate(user_tables):
    tx = 1.0 + i * 2.0
    draw_item_box(tx, y7 + 1.2, 1.8, 0.6, '#E8FDF8', t,
                  fontsize=6, fontcolor=COLORS['text_dark'], border_color=COLORS['db_border'])

for i, t in enumerate(activity_tables):
    tx = 1.0 + i * 2.0
    draw_item_box(tx, y7 + 0.5, 1.8, 0.6, '#D1F2EB', t,
                  fontsize=6, fontcolor=COLORS['text_dark'], border_color=COLORS['db_border'])

for i, t in enumerate(reg_tables):
    tx = 1.0 + i * 2.0
    if tx + 1.8 > 27.5:
        tx = 1.0 + (i - 6) * 2.0
        draw_item_box(tx, y7 + 0.05, 1.8, 0.35, '#A3E4D7', t,
                      fontsize=5.5, fontcolor=COLORS['text_dark'], border_color=COLORS['db_border'])
    else:
        draw_item_box(tx, y7 + 0.05, 1.8, 0.35, '#A3E4D7', t,
                      fontsize=5.5, fontcolor=COLORS['text_dark'], border_color=COLORS['db_border'])

# Mapper到Database箭头
draw_arrow(14, y6, 14, y7 + 2.0, color=COLORS['mapper_border'])
ax.text(14.3, (y6 + y7 + 2.0)/2, 'SQL/ORM', fontsize=7, color=COLORS['mapper_border'],
        fontfamily='Microsoft YaHei', zorder=6)

# ─── 右侧：公共与配置 ──────────────────────────────────────────────────────────
common_x = 22.5
common_y = 12.8
draw_rounded_box(common_x, common_y, 5, 2.3, '#F0F0F0', '#BDC3C7', linewidth=1.5)
draw_header_box(common_x, common_y + 1.85, 5, 0.45, '#7F8C8D', '[公共模块 Common]', fontsize=9)

common_items = ['ApiResponse<T>\n统一响应封装', 'ErrorCode\n错误码定义',
                'BusinessException\n业务异常', 'GlobalExceptionHandler\n全局异常处理',
                'PageResponse\n分页响应']
for i, item in enumerate(common_items):
    draw_item_box(common_x + 0.15, common_y + 1.35 - i * 0.3, 4.7, 0.28, '#FAFAFA', item,
                  fontsize=5.5, fontcolor='#566573')

# ─── 右侧：配置层 ──────────────────────────────────────────────────────────────
config_x = 22.5
config_y = 10.2
draw_rounded_box(config_x, config_y, 5, 2.0, '#FFF8E1', '#FFB300', linewidth=1.5)
draw_header_box(config_x, config_y + 1.55, 5, 0.45, '#F57F17', '[配置层 Config]', fontsize=9)

config_items = ['MybatisPlusConfig\nMyBatis-Plus配置', 'StaticResourceConfig\n静态资源映射',
                'UploadProperties\n上传参数配置', 'DatabaseSchemaInitializer\n数据库初始化']
for i, item in enumerate(config_items):
    draw_item_box(config_x + 0.15, config_y + 1.05 - i * 0.32, 4.7, 0.28, '#FFFDE7', item,
                  fontsize=6, fontcolor='#566573')

# ─── 右侧：枚举定义 ────────────────────────────────────────────────────────────
enum_x = 22.5
enum_y = 7.8
draw_rounded_box(enum_x, enum_y, 5, 2.0, '#E8EAF6', '#5C6BC0', linewidth=1.5)
draw_header_box(enum_x, enum_y + 1.55, 5, 0.45, '#3949AB', '[枚举定义 Enums]', fontsize=9)

enum_items = ['UserRole 角色 (学生/组织者/管理员)', 'ActivityStatus 活动状态',
              'ReviewStatus 审核状态', 'RegistrationStatus 报名状态',
              'AuditAction 审计操作', '... 共13个枚举']
for i, item in enumerate(enum_items):
    draw_item_box(enum_x + 0.15, enum_y + 1.05 - i * 0.25, 4.7, 0.22, '#E8EAF6', item,
                  fontsize=5.5, fontcolor='#283593')

# ─── 底部：核心流程说明 ─────────────────────────────────────────────────────────
flow_y = 0.5
draw_rounded_box(0.5, flow_y, 27, 4.5, COLORS['flow_bg'], COLORS['flow_border'], linewidth=2)
draw_header_box(0.5, flow_y + 4.0, 27, 0.5, COLORS['flow_header'], '[核心业务流程]', fontsize=12)

# 流程1: 活动生命周期
flow1_x = 1.5
draw_item_box(flow1_x, flow_y + 3.0, 5.5, 0.7, '#FCE4EC', '活动生命周期',
              fontsize=8, fontcolor=COLORS['flow_header'], border_color=COLORS['flow_border'])
flow1_steps = ['草稿 DRAFT', '已提交 SUBMITTED', '审核中 PENDING', '已批准 APPROVED',
               '报名中 OPEN', '进行中 ONGOING', '已结束 FINISHED']
for i, step in enumerate(flow1_steps):
    sx = flow1_x + i * 3.6
    draw_item_box(sx, flow_y + 2.15, 3.2, 0.55, '#FCE4EC', step,
                  fontsize=6.5, fontcolor='#880E4F', border_color='#F48FB1')
    if i < len(flow1_steps) - 1:
        draw_arrow(sx + 3.2, flow_y + 2.42, sx + 3.6, flow_y + 2.42, color='#E91E63', lw=1.2)

# 流程2: 用户报名流程
flow2_y = flow_y + 0.3
draw_item_box(1.5, flow2_y + 0.9, 4.0, 0.7, '#E3F2FD', '学生报名流程',
              fontsize=8, fontcolor='#1565C0', border_color='#42A5F5')
reg_steps = ['浏览活动', '查看详情', '提交报名', '获取电子票', '现场签到']
for i, step in enumerate(reg_steps):
    sx = 6.0 + i * 4.2
    draw_item_box(sx, flow2_y + 0.9, 3.8, 0.7, '#E3F2FD', step,
                  fontsize=7, fontcolor='#1565C0', border_color='#42A5F5')
    if i < len(reg_steps) - 1:
        draw_arrow(sx + 3.8, flow2_y + 1.25, sx + 4.2, flow2_y + 1.25, color='#1E88E5', lw=1.2)

# 流程3: 技术栈标注
draw_item_box(1.5, flow_y + 0.15, 5.5, 0.5, '#FFF3E0', '前端: 微信/QQ小程序 + Vant Weapp',
              fontsize=7, fontcolor='#E65100', border_color='#FFB74D')
draw_item_box(7.5, flow_y + 0.15, 5.5, 0.5, '#F3E5F5', '后端: Spring Boot 3 + MyBatis-Plus',
              fontsize=7, fontcolor='#6A1B9A', border_color='#CE93D8')
draw_item_box(13.5, flow_y + 0.15, 5.5, 0.5, '#E0F7FA', '数据库: MySQL 8.0',
              fontsize=7, fontcolor='#00695C', border_color='#80CBC4')
draw_item_box(19.5, flow_y + 0.15, 5.5, 0.5, '#FBE9E7', '认证: Token + 微信/QQ OAuth',
              fontsize=7, fontcolor='#BF360C', border_color='#FFAB91')

# ─── 左侧：角色说明 ────────────────────────────────────────────────────────────
role_x = 0.8
role_y = 0.5
# 已经在底部流程区域中了，不需要额外绘制

# ─── 保存 ──────────────────────────────────────────────────────────────────────
plt.tight_layout(pad=0.5)
output_path = r'C:\Users\Administrator\Desktop\毕业设计\photos\项目架构图.png'
plt.savefig(output_path, dpi=200, bbox_inches='tight',
            facecolor=fig.get_facecolor(), edgecolor='none')
plt.close()
print(f'架构图已保存到: {output_path}')
