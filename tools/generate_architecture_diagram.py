#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
校园活动管理平台 - 分层架构图生成器
使用 matplotlib 生成专业分层架构图，保存为 PNG 图片
"""

import matplotlib
matplotlib.use('Agg')
import matplotlib.pyplot as plt
import matplotlib.patches as mpatches
from matplotlib.patches import FancyBboxPatch
import os
import sys

# ============ 字体配置 ============
plt.rcParams['font.sans-serif'] = ['Microsoft YaHei', 'SimHei', 'DejaVu Sans']
plt.rcParams['axes.unicode_minus'] = False

# ============ 颜色主题 ============
C = {
    'bg': '#FFFFFF',
    'title': '#1A237E',
    # 各层背景色
    'pres': '#E3F2FD', 'pres_b': '#1565C0',
    'gate': '#FFF3E0', 'gate_b': '#E65100',
    'biz':  '#E8F5E9', 'biz_b':  '#2E7D32',
    'data': '#F3E5F5', 'data_b': '#6A1B9A',
    'fnd':  '#FFFDE7', 'fnd_b':  '#F57F17',
    'infra':'#ECEFF1', 'infra_b':'#37474F',
    'net':  '#E0F7FA', 'net_b':  '#00695C',
    'per':  '#FCE4EC', 'per_b':  '#AD1457',
    # 组件色
    'w': '#FFFFFF',
    'blue': '#BBDEFB', 'orange': '#FFE0B2', 'green': '#C8E6C9',
    'purple': '#E1BEE7', 'yellow': '#FFF9C4', 'gray': '#CFD8DC',
    'cyan': '#B2EBF2', 'pink': '#F8BBD0', 'red': '#FFCDD2',
}

# ============ 画布 ============
fig, ax = plt.subplots(1, 1, figsize=(24, 17), dpi=150)
fig.patch.set_facecolor(C['bg'])
ax.set_xlim(0, 24)
ax.set_ylim(0, 17)
ax.set_aspect('equal')
ax.axis('off')
fig.subplots_adjust(left=0.01, right=0.99, top=0.97, bottom=0.01)


def box(x, y, w, h, fc, ec, alpha=0.4, lw=1.5, zorder=1):
    r = FancyBboxPatch((x, y), w, h, boxstyle="round,pad=0.12",
                       facecolor=fc, edgecolor=ec, linewidth=lw, alpha=alpha, zorder=zorder)
    ax.add_patch(r)


def comp(x, y, w, h, txt, fc, ec=None, fs=8, color='#333', bold=False, zorder=6):
    if ec is None:
        ec = fc
    r = FancyBboxPatch((x, y), w, h, boxstyle="round,pad=0.06",
                       facecolor=fc, edgecolor=ec, linewidth=0.8, alpha=0.95, zorder=zorder)
    ax.add_patch(r)
    fw = 'bold' if bold else 'normal'
    ax.text(x + w/2, y + h/2, txt, fontsize=fs, fontweight=fw, color=color,
            ha='center', va='center', zorder=zorder+1)


def grp(x, y, w, h, title, items, bg, bc, ic, tc='#333', ifs=7.5):
    box(x, y, w, h, bg, bc, alpha=0.5, lw=1.2, zorder=2)
    ax.text(x + w/2, y + h - 0.16, title, fontsize=8.5, fontweight='bold',
            color=tc, ha='center', va='top', zorder=6)
    n = len(items)
    if n == 0:
        return
    cols = min(n, 4)
    rows = (n + cols - 1) // cols
    px, py_ = 0.1, 0.32
    iw = (w - px * 2) / cols - 0.04
    ih = (h - py_ - 0.18) / rows - 0.05
    for i, item in enumerate(items):
        c = i % cols
        r = i // cols
        ix = x + px + c * (iw + 0.04)
        iy = y + h - py_ - (r + 1) * (ih + 0.05)
        comp(ix, iy, iw, ih, item, ic, bc, ifs)


def layer_title(y, h, txt, color):
    ax.text(0.6, y + h/2, txt, fontsize=11.5, fontweight='bold', color=color,
            ha='center', va='center', rotation=90,
            bbox=dict(boxstyle='round,pad=0.3', facecolor='white',
                      edgecolor=color, linewidth=1.5, alpha=0.95),
            zorder=10)


# ============ 标题 ============
ax.text(12, 16.65, '校园活动管理平台 — 系统架构图', fontsize=22, fontweight='bold',
        color=C['title'], ha='center', va='center', zorder=10)
ax.plot([4, 20], [16.35, 16.35], color=C['pres_b'], linewidth=2.5, zorder=5)

# ============ 层定义 ============
LX, LW, LH = 1.5, 21.5, 1.65
GAP = 0.1
layers_info = [
    ('展示层',   C['pres'], C['pres_b'],  14.55),
    ('网关层',   C['gate'], C['gate_b'],  12.78),
    ('业务能力层', C['biz'],  C['biz_b'],   10.73),
    ('数据能力层', C['data'], C['data_b'],  8.96),
    ('基础能力层', C['fnd'],  C['fnd_b'],   7.19),
    ('基础设施层', C['infra'],C['infra_b'], 5.14),
    ('网络层',   C['net'],  C['net_b'],   3.37),
    ('感知层',   C['per'],  C['per_b'],   1.60),
]

for name, bg, bc, y in layers_info:
    box(LX, y, LW, LH, bg, bc)
    layer_title(y, LH, name, bc)

# ================================================================
# 1. 展示层 y=14.55
# ================================================================
py = 14.55
# 用户角色
roles = [('学生用户', C['blue']), ('组织者', C['green']), ('管理员', C['pink'])]
for i, (t, c) in enumerate(roles):
    comp(2.0 + i*2.3, py+0.3, 2.0, 1.05, t, c, C['pres_b'], 9, bold=True)

# 客户端平台
plats = [('微信小程序', C['w']), ('QQ小程序', C['w']), ('Web管理后台', C['w'])]
for i, (t, c) in enumerate(plats):
    comp(9.2 + i*2.8, py+0.3, 2.5, 1.05, t, c, C['pres_b'], 9.5, bold=True)

# 前端技术栈
ax.text(18.5, py+1.3, '前端技术栈', fontsize=9, fontweight='bold',
        color=C['pres_b'], ha='center', zorder=6)
fe = [('原生框架', C['w']), ('Vant Weapp', C['w']), ('runtime-api', C['w'])]
for i, (t, c) in enumerate(fe):
    comp(17.3 + (i%2)*1.8, py+0.2 + (1-i//2)*0.55, 1.6, 0.45, t, c, C['pres_b'], 7)

# ================================================================
# 2. 网关层 y=12.78
# ================================================================
gy = 12.78
grp(2.0, gy+0.12, 5.8, 1.4, 'API 网关',
    ['路由转发', '请求过滤', '版本管理(/api/v1)', '操作人上下文注入'],
    C['orange'], C['gate_b'], C['orange'], C['gate_b'])

grp(8.2, gy+0.12, 4.8, 1.4, '认证鉴权',
    ['微信登录', 'QQ登录', 'Token校验', '角色权限(STUDENT/ORGANIZER/ADMIN)'],
    C['orange'], C['gate_b'], C['orange'], C['gate_b'])

grp(13.4, gy+0.12, 5.0, 1.4, '统一响应处理',
    ['ApiResponse封装', 'BusinessException', 'GlobalExceptionHandler', 'ErrorCode'],
    C['orange'], C['gate_b'], C['orange'], C['gate_b'])

ax.text(20.5, gy+1.0, '外部API', fontsize=9, fontweight='bold',
        color=C['gate_b'], ha='center', zorder=6)
for i, t in enumerate(['微信API', 'QQ API']):
    comp(19.3 + i*2.2, gy+0.15, 1.9, 0.5, t, C['w'], C['gate_b'], 7.5)

# ================================================================
# 3. 业务能力层 y=10.73
# ================================================================
by_ = 10.73
core = [('认证服务\nV1Auth', C['green']), ('用户服务\nV1User', C['green']),
        ('活动服务\nV1Activity', C['green']), ('报名服务\nV1Registration', C['green'])]
for i, (t, c) in enumerate(core):
    comp(2.0 + i*2.7, by_+0.2, 2.4, 1.2, t, c, C['biz_b'], 8.5, bold=True)

ext = [('收藏服务\nV1Favorite', C['green']), ('评论服务\nV1Comment', C['green']),
       ('审核服务\nV1Review', C['green'])]
for i, (t, c) in enumerate(ext):
    comp(13.0 + i*2.7, by_+0.2, 2.4, 1.2, t, c, C['biz_b'], 8.5, bold=True)

ax.text(20.5, by_+1.15, '辅助服务', fontsize=9, fontweight='bold',
        color=C['biz_b'], ha='center', zorder=6)
aux = ['头像管理', '权限校验', '阶段解析']
for i, a in enumerate(aux):
    comp(19.3 + (i%2)*2.0, by_+0.1 + (1-i//2)*0.55, 1.7, 0.42, a, C['w'], C['biz_b'], 7)

# ================================================================
# 4. 数据能力层 y=8.96
# ================================================================
dy = 8.96
mappers = ['UserMapper', 'ActivityMapper', 'RegistrationMapper',
           'ReviewMapper', 'FavoriteMapper', 'CommentMapper']
for i, m in enumerate(mappers):
    comp(2.0 + i*2.8, dy+0.85, 2.5, 0.55, m, C['purple'], C['data_b'], 7)

dto = [('Request DTO', C['purple']), ('Response View', C['purple']),
       ('PageResponse', C['purple'])]
for i, (t, c) in enumerate(dto):
    comp(2.0 + i*3.2, dy+0.15, 2.9, 0.55, t, c, C['data_b'], 7.5)

ax.text(17.8, dy+1.15, '数据工具', fontsize=9, fontweight='bold',
        color=C['data_b'], ha='center', zorder=6)
dt = ['MyBatis-Plus', 'XML映射', '分页查询', '关联查询']
for i, d in enumerate(dt):
    comp(16.5 + (i%2)*2.4, dy+0.1 + (1-i//2)*0.55, 2.1, 0.42, d, C['w'], C['data_b'], 7)

# ================================================================
# 5. 基础能力层 y=7.19
# ================================================================
fy = 7.19
fws = [('Spring Boot 3', C['yellow']), ('MyBatis-Plus', C['yellow']),
       ('MySQL 8', C['yellow']), ('Redis', C['yellow'])]
for i, (t, c) in enumerate(fws):
    comp(2.0 + i*2.9, fy+0.2, 2.6, 1.2, t, c, C['fnd_b'], 9.5, bold=True)

mid = [('文件存储\n(avatar/cover)', C['yellow']), ('异常处理体系', C['yellow']),
       ('日志管理\n(LoginLog)', C['yellow'])]
for i, (t, c) in enumerate(mid):
    comp(13.8 + i*2.7, fy+0.2, 2.4, 1.2, t, c, C['fnd_b'], 8.5)

ax.text(20.5, fy+1.1, '配置管理', fontsize=9, fontweight='bold',
        color=C['fnd_b'], ha='center', zorder=6)
cfgs = ['application.yml', 'MybatisPlusConfig', 'StaticResourceConfig']
for i, c_ in enumerate(cfgs):
    comp(19.3 + (i%2)*1.8, fy+0.1 + (1-i//2)*0.5, 1.6, 0.42, c_, C['w'], C['fnd_b'], 6)

# ================================================================
# 6. 基础设施层 y=5.14
# ================================================================
iy = 5.14
infra_l = [('Docker', C['gray']), ('Nginx', C['gray']),
           ('Linux服务器', C['gray']), ('Maven构建', C['gray'])]
for i, (t, c) in enumerate(infra_l):
    comp(2.0 + i*2.7, iy+0.2, 2.4, 1.2, t, c, C['infra_b'], 9, bold=True)

infra_r = [('Git版本控制', C['gray']), ('微信开发者工具', C['gray']),
           ('VS Code', C['gray']), ('IDEA', C['gray'])]
for i, (t, c) in enumerate(infra_r):
    comp(13.0 + i*2.7, iy+0.2, 2.4, 1.2, t, c, C['infra_b'], 8.5)

ax.text(20.5, iy+1.1, '部署方式', fontsize=9, fontweight='bold',
        color=C['infra_b'], ha='center', zorder=6)
for i, d in enumerate(['jar部署', '端口8080']):
    comp(19.3 + i*2.0, iy+0.15, 1.7, 0.5, d, C['w'], C['infra_b'], 7)

# ================================================================
# 7. 网络层 y=3.37
# ================================================================
ny = 3.37
nets = [('HTTP/HTTPS', C['cyan']), ('WebSocket', C['cyan']),
        ('微信API网络', C['cyan']), ('QQ API网络', C['cyan']),
        ('局域网调试', C['cyan'])]
for i, (t, c) in enumerate(nets):
    comp(2.0 + i*2.7, ny+0.2, 2.4, 1.2, t, c, C['net_b'], 9, bold=True)

ax.text(18.0, ny+1.05, '通信协议', fontsize=9, fontweight='bold',
        color=C['net_b'], ha='center', zorder=6)
for i, p in enumerate(['RESTful API', 'JSON数据格式', 'Token认证']):
    comp(16.5 + i*2.2, ny+0.1, 1.9, 0.5, p, C['w'], C['net_b'], 7)

# ================================================================
# 8. 感知层 y=1.60
# ================================================================
pery = 1.60
pers = [('位置感知', C['pink']), ('图片上传', C['pink']),
        ('消息通知', C['pink']), ('数据采集', C['pink']),
        ('身份识别', C['pink'])]
for i, (t, c) in enumerate(pers):
    comp(2.0 + i*2.7, pery+0.15, 2.4, 1.15, t, c, C['per_b'], 9, bold=True)

ax.text(18.0, pery+1.0, '终端设备', fontsize=9, fontweight='bold',
        color=C['per_b'], ha='center', zorder=6)
for i, d in enumerate(['智能手机', '平板电脑', 'PC浏览器']):
    comp(16.5 + i*2.2, pery+0.1, 1.9, 0.5, d, C['w'], C['per_b'], 7)

# ============ 层间箭头 ============
ac = '#B0BEC5'
for i in range(len(layers_info) - 1):
    _, _, _, yt = layers_info[i]
    _, _, _, yb = layers_info[i+1]
    for dx in [-4, 0, 4]:
        ax.annotate('', xy=(12+dx, yb + LH), xytext=(12+dx, yt),
                    arrowprops=dict(arrowstyle='->', color=ac, lw=1.0, alpha=0.45),
                    zorder=3)

# ============ 右侧图例 ============
lx, ly_ = 22.5, 16.0
ax.text(lx, ly_, '图例', fontsize=10, fontweight='bold', color=C['title'],
        ha='center', zorder=10)
legend = [
    ('展示层', C['pres'], C['pres_b']),
    ('网关层', C['gate'], C['gate_b']),
    ('业务能力层', C['biz'], C['biz_b']),
    ('数据能力层', C['data'], C['data_b']),
    ('基础能力层', C['fnd'], C['fnd_b']),
    ('基础设施层', C['infra'], C['infra_b']),
    ('网络层', C['net'], C['net_b']),
    ('感知层', C['per'], C['per_b']),
]
for i, (lb, bg, bc) in enumerate(legend):
    lly = ly_ - 0.35 - i * 0.36
    r = FancyBboxPatch((lx - 1.1, lly - 0.1), 0.28, 0.2,
                       boxstyle="round,pad=0.02", facecolor=bg, edgecolor=bc,
                       linewidth=1, alpha=0.7, zorder=5)
    ax.add_patch(r)
    ax.text(lx - 0.65, lly, lb, fontsize=7, color=C['title'],
            ha='center', va='center', zorder=6)

# ============ 保存 ============
output_dir = r'C:\Users\Administrator\Desktop\毕业设计\photos'
os.makedirs(output_dir, exist_ok=True)
output_path = os.path.join(output_dir, 'architecture_diagram.png')
fig.savefig(output_path, dpi=150, bbox_inches='tight', facecolor=C['bg'], edgecolor='none')
plt.close(fig)
print(f'架构图已保存至: {output_path}')
