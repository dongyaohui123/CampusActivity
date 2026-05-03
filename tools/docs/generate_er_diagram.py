# -*- coding: utf-8 -*-
"""
陈氏 E-R 图生成器 v7
线段去重：共享路径只绘制一次
"""

import matplotlib
matplotlib.use('Agg')
import matplotlib.pyplot as plt
import matplotlib.patches as patches
import matplotlib.font_manager as fm
import numpy as np
import glob, os

cache_dir = matplotlib.get_cachedir()
for f in glob.glob(os.path.join(cache_dir, 'fontlist-*.json')):
    os.remove(f)
fm._load_fontmanager(try_read_cache=False)
fm.fontManager.addfont(r'C:\Windows\Fonts\simhei.ttf')
plt.rcParams['font.family'] = 'sans-serif'
plt.rcParams['font.sans-serif'] = ['SimHei']
plt.rcParams['axes.unicode_minus'] = False

C_ENT='#1a5276'; C_EBG='#d6eaf8'; C_ATT='#2e86c1'; C_ABG='#eaf2f8'
C_PK='#b7950b';  C_PBG='#fef9e7'; C_REL='#c0392b'; C_RBG='#fadbd8'; C_LN='#566573'

W, H = 2600, 2000
fig, ax = plt.subplots(figsize=(W/100, H/100), dpi=100)
ax.set_xlim(0,W); ax.set_ylim(0,H); ax.set_aspect('equal'); ax.axis('off')
fig.patch.set_facecolor('white')

# ── 线段去重集合 ──
_drawn_lines = set()
def ln(x1,y1,x2,y2):
    key = (round(x1,1),round(y1,1),round(x2,1),round(y2,1))
    rkey = (round(x2,1),round(y2,1),round(x1,1),round(y1,1))
    if key not in _drawn_lines and rkey not in _drawn_lines:
        ax.plot([x1,x2],[y1,y2],'-',color=C_LN,lw=1.2,zorder=0)
        _drawn_lines.add(key)

def ent(x,y,w,h,n):
    ax.add_patch(patches.FancyBboxPatch((x-w/2,y-h/2),w,h,boxstyle="round,pad=0.02",lw=2.5,edgecolor=C_ENT,facecolor=C_EBG))
    ax.text(x,y,n,ha='center',va='center',fontsize=11,color=C_ENT)
def att(x,y,l,pk=False):
    tw,th=max(len(l)*7+18,48),24; ec,fc=(C_PK,C_PBG)if pk else(C_ATT,C_ABG)
    ax.add_patch(patches.Ellipse((x,y),tw,th,lw=1.2,edgecolor=ec,facecolor=fc))
    ax.text(x,y,l,ha='center',va='center',fontsize=8.5,color=ec,fontweight='bold'if pk else'normal')
def dia(x,y,l):
    ax.add_patch(patches.RegularPolygon((x,y),4,radius=34,orientation=np.pi/4,lw=1.8,edgecolor=C_REL,facecolor=C_RBG))
    ax.text(x,y,l,ha='center',va='center',fontsize=9.5,color=C_REL,fontweight='bold')
def lbl(x,y,t,ha='center'):
    ax.text(x,y,t,fontsize=9.5,color='#e74c3c',fontweight='bold',ha=ha,va='center',
            bbox=dict(boxstyle='round,pad=0.15',fc='white',ec='none',alpha=0.95))
def ept(ex,ey,ew,eh,tx,ty):
    hw,hh=ew/2+3,eh/2+3; vx,vy=tx-ex,ty-ey; d=max((vx**2+vy**2)**.5,1e-9)
    nx,ny=vx/d,vy/d; ts=[]
    if abs(nx)>1e-9:ts.append(hw/abs(nx))
    if abs(ny)>1e-9:ts.append(hh/abs(ny))
    t=min(ts)if ts else 0; return ex+nx*t,ey+ny*t

def ortho_path(pts):
    """绘制正交折线（去重）"""
    for i in range(len(pts)-1):
        ln(pts[i][0], pts[i][1], pts[i+1][0], pts[i+1][1])

def label_on_seg(p1, p2, text, offset=(12,0)):
    """在线段中点标注"""
    mx = (p1[0]+p2[0])/2 + offset[0]
    my = (p1[1]+p2[1])/2 + offset[1]
    lbl(mx, my, text)

# ═══════════════════════════════════════
# 布局
# ═══════════════════════════════════════
E = {
    '审核日志':(450,1700,150,48),
    '用户':   (1200,1700,150,48),
    '学生档案':(450,1100,170,48),
    '组织者档案':(1200,1100,180,48),
    '活动':   (2000,1100,150,48),
    '报名':   (450,500,150,48),
    '收藏':   (900,500,140,48),
    '关注':   (1400,500,140,48),
    '活动分类':(2000,500,160,48),
}
EN = ['AuditLog','User','StudentProfile','OrganizerProfile','Activity',
      'Registration','Favorite','Follow','Category']

A = [
    ('审核日志','id',260,1700,True),('审核日志','activity_id',260,1600,False),
    ('审核日志','operator_id',260,1500,False),('审核日志','action',450,1560,False),
    ('用户','id',1050,1850,True),('用户','username',1200,1850,True),('用户','nickname',1350,1850,False),
    ('用户','role',1050,1550,False),('用户','gender',1200,1550,False),('用户','openid',1350,1550,False),
    ('学生档案','user_id',450,1240,True),('学生档案','student_no',260,1100,False),
    ('学生档案','real_name',260,1000,False),('学生档案','college',260,900,False),
    ('学生档案','major',450,960,False),('学生档案','grade',640,960,False),
    ('组织者档案','user_id',1200,1240,True),('组织者档案','organizer_name',1390,1100,False),
    ('组织者档案','organizer_type',1390,1000,False),('组织者档案','contact_name',1390,900,False),
    ('组织者档案','certification_status',1200,960,False),('组织者档案','intro',1010,960,False),
    ('活动','id',1820,1100,True),('活动','title',1820,1000,False),('活动','location',1820,900,False),
    ('活动','status',2000,1240,False),('活动','start_time',2000,960,False),
    ('活动','end_time',2180,960,False),('活动','max_participants',2180,1100,False),
    ('活动','visibility',2180,1000,False),
    ('报名','id',260,500,True),('报名','activity_id',260,400,False),('报名','user_id',450,360,False),
    ('报名','status',640,360,False),('报名','registered_at',450,640,False),('报名','checkin_at',640,640,False),
    ('收藏','id',750,360,True),('收藏','activity_id',900,360,False),('收藏','user_id',1050,360,False),
    ('收藏','created_at',900,640,False),
    ('关注','id',1250,360,True),('关注','organizer_id',1400,360,False),('关注','user_id',1550,360,False),
    ('关注','created_at',1400,640,False),
    ('活动分类','id',2190,500,True),('活动分类','name',2190,400,False),
    ('活动分类','sort_no',2000,360,False),('活动分类','parent_id',1810,360,False),
]

# 联系: (标签,dx,dy,e1,e2,基数)
R = [
    ('属于',750,1400,'用户','学生档案','1:1'),
    ('属于',1600,1400,'用户','组织者档案','1:1'),
    ('创建',1600,1100,'组织者档案','活动','1:N'),
    ('发布',1600,800,'活动','用户','1:N'),
    ('报名',450,800,'用户','活动','M:N'),
    ('收藏',900,800,'用户','活动','M:N'),
    ('关注',1400,800,'用户','活动','M:N'),
    ('包含',2000,800,'活动','活动分类','M:N'),
    ('审核',450,1400,'用户','活动','1:N'),
]

# ═══════════════════════════════════════
# 绘制
# ═══════════════════════════════════════

# 实体
for i,(n,(x,y,w,h)) in enumerate(E.items()):
    ent(x,y,w,h,f'{n}\n{EN[i]}')

# 属性 + 属性连线
for ek,l,ax_,ay_,pk in A:
    att(ax_,ay_,l,pk)
    if ek in E:
        ex,ey,ew,eh=E[ek]; ex_,ey_=ept(ex,ey,ew,eh,ax_,ay_)
        ln(ex_,ey_,ax_,ay_)

# ── 关键坐标 ──
ub = 1700-48/2-3   # 用户底边 1674
ab = 1100-48/2-3   # 活动底边 1074
ob = 1100-48/2-3   # 组织者底边 1074
sb = 1100-48/2-3   # 学生底边 1074
rb = 500+48/2+3    # 报名顶边 527

# ── 共享横线（只画一次）──
ln(450, 1400, 1600, 1400)   # 中层横线：审核→属于2
ln(450, 800, 1400, 800)     # 底层横线：报名→关注

# ── 各联系的独立线段 ──

# 1. 审核日志 → 审核菱形(450,1400)
a = ept(*E['审核日志'], 450, 1400)
ortho_path([(a[0],a[1]), (450,a[1]), (450,1400)])

# 2. 用户 → 中层横线(竖线)
ln(1200, ub, 1200, 1400)

# 3. 学生档案 → 属于1菱形(750,1400)
s = ept(*E['学生档案'], 750, 1400)
ortho_path([(s[0],s[1]), (s[0],1400), (750,1400)])

# 4. 组织者档案 → 属于2菱形(1600,1400)
o1 = ept(*E['组织者档案'], 1600, 1400)
ortho_path([(o1[0],o1[1]), (o1[0],1400), (1600,1400)])

# 5. 组织者档案 → 创建菱形(1600,1100)
o2 = ept(*E['组织者档案'], 1600, 1100)
ortho_path([(o2[0],o2[1]), (1600,o2[1]), (1600,1100)])

# 6. 活动 → 发布菱形(1600,800)
a1 = ept(*E['活动'], 1600, 800)
ortho_path([(a1[0],a1[1]), (1600,a1[1]), (1600,800)])

# 7. 发布菱形 → 用户（经横线到共享竖线）
ln(1600, 800, 1200, 800)
ln(1200, 800, 1200, 1400)  # 注意：这段与#2部分重合，但 ln() 会去重

# 8. 用户 → 报名菱形(450,800)（经中层横线下延）
ln(1200, 1400, 450, 1400)  # 已有共享横线覆盖（去重）
ln(450, 1400, 450, 800)    # 竖线下延

# 9. 活动 → 报名菱形
a2 = ept(*E['活动'], 450, 800)
ortho_path([(a2[0],a2[1]), (450,a2[1]), (450,800)])

# 10. 用户 → 收藏菱形(900,800)
ln(1200, 1400, 900, 1400)  # 中层横线段
ln(900, 1400, 900, 800)    # 竖线下延

# 11. 活动 → 收藏菱形
a3 = ept(*E['活动'], 900, 800)
ortho_path([(a3[0],a3[1]), (900,a3[1]), (900,800)])

# 12. 用户 → 关注菱形(1400,800)
# 中层横线到1400已有，但需要竖线
ln(1400, 1400, 1400, 800)

# 13. 活动 → 关注菱形
a4 = ept(*E['活动'], 1400, 800)
ortho_path([(a4[0],a4[1]), (1400,a4[1]), (1400,800)])

# 14. 活动 → 包含菱形(2000,800)
a5 = ept(*E['活动'], 2000, 800)
ln(a5[0], a5[1], 2000, a5[1])
ln(2000, a5[1], 2000, 800)

# 15. 活动分类 → 包含菱形
c = ept(*E['活动分类'], 2000, 800)
ortho_path([(c[0],c[1]), (c[0],800), (2000,800)])

# 16. 关注实体 → 底层横线
fw = ept(*E['关注'], 1400, 800)
ln(1400, fw[1], 1400, 800)

# ── 基数标注 ──
lbl(1210, (ub+1400)/2, '1')       # 用户→属于/审核
lbl(740, 1412, '1')               # 属于1 学生侧
lbl(460, 1412, '1')               # 审核 用户侧
lbl(1610, 1412, '1')              # 属于2 组织者侧
lbl(1610, (1100+ab)/2, 'N')       # 创建 活动侧
lbl(1590, (800+ab)/2, '1')        # 发布 活动侧
lbl(1210, (ub+800)/2, 'N')        # 发布 用户侧
lbl(460, (1400+800)/2, 'N')       # 报名 用户侧
lbl(910, (1400+800)/2, 'N')       # 收藏 用户侧
lbl(1410, (1400+800)/2, 'N')      # 关注 用户侧
lbl(440, (800+ab)/2, 'M')         # 报名 活动侧
lbl(890, (800+ab)/2, 'M')         # 收藏 活动侧
lbl(1390, (800+ab)/2, 'M')        # 关注 活动侧
lbl(2010, (800+ab)/2, 'M')        # 包含 活动侧
lbl(1990, (800+500-48/2-3)/2, 'N')# 包含 分类侧
lbl(1390, (800+ob)/2, 'M')        # 关注 组织者侧

# ═══════════════════════════════════════
# 图例 + 标题 + 边框
# ═══════════════════════════════════════
lx,ly=50,50
ax.add_patch(patches.FancyBboxPatch((lx,ly+80),50,25,boxstyle="round,pad=0.02",lw=1.8,edgecolor=C_ENT,facecolor=C_EBG))
ax.text(lx+68,ly+92,'实体 Entity',fontsize=10,va='center')
ax.add_patch(patches.Ellipse((lx+25,ly+45),50,22,lw=1.2,edgecolor=C_ATT,facecolor=C_ABG))
ax.text(lx+68,ly+45,'属性 Attribute',fontsize=10,va='center')
ax.add_patch(patches.RegularPolygon((lx+25,ly+8),4,radius=16,orientation=np.pi/4,lw=1.8,edgecolor=C_REL,facecolor=C_RBG))
ax.text(lx+68,ly+8,'联系 Relationship',fontsize=10,va='center')
ax.text(lx+68,ly-28,'主键(PK)加下划线',fontsize=10,va='center',color=C_PK,fontweight='bold',
        bbox=dict(boxstyle='round,pad=0.15',fc=C_PBG,ec='none'))
ax.text(W/2,H-35,'图3-1  校园活动管理系统 E-R 图（陈氏表示法）',
        ha='center',va='center',fontsize=16,fontweight='bold',color='#2c3e50')
ax.add_patch(patches.Rectangle((10,10),W-20,H-20,lw=1.5,edgecolor='#bdc3c7',facecolor='none'))

# 保存
od=r'C:\Users\Administrator\Desktop\论文正文'; os.makedirs(od,exist_ok=True)
op=os.path.join(od,'陈氏E-R图.png')
plt.tight_layout(pad=1.0)
fig.savefig(op,dpi=200,bbox_inches='tight',facecolor='white',edgecolor='none')
plt.close()
print(f'OK: {op}')
