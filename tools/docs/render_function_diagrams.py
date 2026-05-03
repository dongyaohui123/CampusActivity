from __future__ import annotations

from dataclasses import dataclass, field
from pathlib import Path
from typing import Iterable

from PIL import Image, ImageDraw, ImageFont


REPO_ROOT = Path(__file__).resolve().parents[1]
OUTPUT_DIR = REPO_ROOT / "docs" / "images"

FONT_REGULAR = Path(r"C:\Windows\Fonts\simsun.ttc")
FONT_BOLD = Path(r"C:\Windows\Fonts\simsun.ttc")

NODE_FONT_SIZE = 23

PADDING_X = 14
PADDING_Y = 10
TEXT_LINE_GAP = 4
VERTICAL_CHAR_GAP = 4
SUBTREE_V_GAP = 22
SUBTREE_H_GAP = 86
CANVAS_PADDING_X = 56
CANVAS_PADDING_Y = 54
CONNECTOR_STROKE = 1
BOX_STROKE = 2
MIN_BOX_HEIGHT = 48
ROOT_BOX_WIDTH = 70
BRANCH_INSET = 24

BORDER = "#38485D"
CONNECTOR = "#7A8899"
TEXT = "#243447"
BACKGROUND = "#FFFFFF"

ROOT_FILL = "#DAE7F6"
COMMON_FILL = "#E9EEF6"
STUDENT_FILL = "#EAF3FB"
ORGANIZER_FILL = "#EEF5FA"
ADMIN_FILL = "#F4F2FB"
GROUP_FILL = "#F1F5F9"
MODULE_FILL = "#F7FAFC"
LEAF_FILL = "#FFFFFF"

MAX_BOX_WIDTH = {
    0: 110,
    1: 168,
    2: 176,
    3: 188,
    4: 188,
}


@dataclass
class Node:
    label: str
    children: list["Node"] = field(default_factory=list)
    fill: str | None = None
    vertical: bool = False
    width: int = 0
    height: int = 0
    subtree_width: int = 0
    subtree_height: int = 0
    lines: list[str] = field(default_factory=list)


def font(size: int, bold: bool = False) -> ImageFont.FreeTypeFont:
    return ImageFont.truetype(str(FONT_BOLD if bold else FONT_REGULAR), size)


NODE_FONT = font(NODE_FONT_SIZE)


def node(label: str, *children: Node, fill: str | None = None, vertical: bool = False) -> Node:
    return Node(label=label, children=list(children), fill=fill, vertical=vertical)


def wrap_text(text: str, max_width: int, draw: ImageDraw.ImageDraw, font_obj: ImageFont.FreeTypeFont) -> list[str]:
    text = str(text or "").strip()
    if not text:
        return [""]

    lines: list[str] = []
    current = ""
    for ch in text:
        candidate = current + ch
        bbox = draw.textbbox((0, 0), candidate, font=font_obj)
        width = bbox[2] - bbox[0]
        if current and width > max_width:
            lines.append(current)
            current = ch
        else:
            current = candidate
    if current:
        lines.append(current)
    return lines or [text]


def measure_tree(root: Node, draw: ImageDraw.ImageDraw, depth: int = 0) -> None:
    if root.vertical:
        root.lines = [ch for ch in str(root.label or "").strip()]
        char_widths = []
        char_heights = []
        for char in root.lines:
            bbox = draw.textbbox((0, 0), char, font=NODE_FONT)
            char_widths.append(bbox[2] - bbox[0])
            char_heights.append(bbox[3] - bbox[1])
        root.width = max(ROOT_BOX_WIDTH, max(char_widths, default=0) + PADDING_X * 2)
        root.height = sum(char_heights) + max(0, len(char_heights) - 1) * VERTICAL_CHAR_GAP + PADDING_Y * 2
    else:
        target_width = MAX_BOX_WIDTH.get(depth, 196)
        root.lines = wrap_text(root.label, max(10, target_width - PADDING_X * 2), draw, NODE_FONT)
        text_heights = []
        max_text_width = 0
        for line in root.lines:
            bbox = draw.textbbox((0, 0), line, font=NODE_FONT)
            max_text_width = max(max_text_width, bbox[2] - bbox[0])
            text_heights.append(bbox[3] - bbox[1])

        text_height = sum(text_heights) + max(0, len(root.lines) - 1) * TEXT_LINE_GAP
        root.width = max(target_width, max_text_width + PADDING_X * 2)
        root.height = max(MIN_BOX_HEIGHT, text_height + PADDING_Y * 2)

    if not root.children:
        root.subtree_width = root.width
        root.subtree_height = root.height
        return

    for child in root.children:
        measure_tree(child, draw, depth + 1)

    children_height = sum(child.subtree_height for child in root.children)
    children_height += SUBTREE_V_GAP * max(0, len(root.children) - 1)

    root.subtree_width = root.width + SUBTREE_H_GAP + max(child.subtree_width for child in root.children)
    root.subtree_height = max(root.height, children_height)


def draw_multiline_text(
    draw: ImageDraw.ImageDraw,
    box: tuple[int, int, int, int],
    lines: Iterable[str],
    vertical: bool = False,
) -> None:
    x0, y0, x1, y1 = box
    lines = list(lines)
    line_heights = []
    for line in lines:
        bbox = draw.textbbox((0, 0), line, font=NODE_FONT)
        line_heights.append(bbox[3] - bbox[1])
    gap = VERTICAL_CHAR_GAP if vertical else TEXT_LINE_GAP
    total_height = sum(line_heights) + max(0, len(lines) - 1) * gap
    current_y = y0 + (y1 - y0 - total_height) / 2
    for line, line_height in zip(lines, line_heights):
        bbox = draw.textbbox((0, 0), line, font=NODE_FONT)
        line_width = bbox[2] - bbox[0]
        current_x = x0 + (x1 - x0 - line_width) / 2
        draw.text((current_x, current_y), line, fill=TEXT, font=NODE_FONT)
        current_y += line_height + gap


def render_tree(draw: ImageDraw.ImageDraw, root: Node, x: int, y: int) -> None:
    node_center_y = y + root.subtree_height / 2
    node_x0 = x
    node_y0 = node_center_y - root.height / 2
    node_x1 = node_x0 + root.width
    node_y1 = node_y0 + root.height

    draw.rectangle((node_x0, node_y0, node_x1, node_y1), outline=BORDER, width=BOX_STROKE, fill=root.fill or LEAF_FILL)
    draw_multiline_text(draw, (int(node_x0), int(node_y0), int(node_x1), int(node_y1)), root.lines, vertical=root.vertical)

    if not root.children:
        return

    children_total_height = sum(child.subtree_height for child in root.children)
    children_total_height += SUBTREE_V_GAP * max(0, len(root.children) - 1)
    child_y = y + (root.subtree_height - children_total_height) / 2
    child_x = x + root.width + SUBTREE_H_GAP
    branch_x = child_x - BRANCH_INSET
    child_centers: list[float] = []

    for child in root.children:
        child_center = child_y + child.subtree_height / 2
        child_centers.append(child_center)
        render_tree(draw, child, int(child_x), int(child_y))
        child_y += child.subtree_height + SUBTREE_V_GAP

    draw.line((node_x1, node_center_y, branch_x, node_center_y), fill=CONNECTOR, width=CONNECTOR_STROKE)
    draw.line((branch_x, child_centers[0], branch_x, child_centers[-1]), fill=CONNECTOR, width=CONNECTOR_STROKE)
    for child_center in child_centers:
        draw.line((branch_x, child_center, child_x, child_center), fill=CONNECTOR, width=CONNECTOR_STROKE)


def create_image(root: Node, output_name: str) -> Path:
    scratch = Image.new("RGB", (9000, 9000), BACKGROUND)
    scratch_draw = ImageDraw.Draw(scratch)
    measure_tree(root, scratch_draw)

    canvas_width = int(root.subtree_width + CANVAS_PADDING_X * 2)
    canvas_height = int(root.subtree_height + CANVAS_PADDING_Y * 2)
    image = Image.new("RGB", (canvas_width, canvas_height), BACKGROUND)
    draw = ImageDraw.Draw(image)

    tree_x = CANVAS_PADDING_X
    tree_y = CANVAS_PADDING_Y
    render_tree(draw, root, tree_x, tree_y)

    OUTPUT_DIR.mkdir(parents=True, exist_ok=True)
    output_path = OUTPUT_DIR / output_name
    image.save(output_path, format="PNG")
    return output_path


def common_tree() -> Node:
    return node(
        "公共功能",
        node(
            "公共服务能力",
            node(
                "认证与账户能力",
                node(
                    "认证与账户模块",
                    node("用户名密码登录", fill=LEAF_FILL),
                    node("新用户注册", fill=LEAF_FILL),
                    node("微信登录", fill=LEAF_FILL),
                    fill=MODULE_FILL,
                ),
                fill=GROUP_FILL,
            ),
            node(
                "门户访问能力",
                node(
                    "门户访问模块",
                    node("首页推荐与快捷入口", fill=LEAF_FILL),
                    node("轮播推荐展示", fill=LEAF_FILL),
                    node("快捷入口访问", fill=LEAF_FILL),
                    node("活动搜索筛选", fill=LEAF_FILL),
                    node("活动详情查看", fill=LEAF_FILL),
                    fill=MODULE_FILL,
                ),
                fill=GROUP_FILL,
            ),
            node(
                "个人资料能力",
                node(
                    "个人资料模块",
                    node("个人资料维护", fill=LEAF_FILL),
                    node("头像上传", fill=LEAF_FILL),
                    node("头像裁剪", fill=LEAF_FILL),
                    node("密码修改", fill=LEAF_FILL),
                    node("角色面板/统计查看", fill=LEAF_FILL),
                    fill=MODULE_FILL,
                ),
                fill=GROUP_FILL,
            ),
            fill=COMMON_FILL,
        ),
        fill=ROOT_FILL,
        vertical=True,
    )


def student_tree() -> Node:
    return node(
        "学生端",
        node(
            "活动参与能力",
            node(
                "活动参与模块",
                node("活动报名", fill=LEAF_FILL),
                node("取消报名", fill=LEAF_FILL),
                node("报名记录查看", fill=LEAF_FILL),
                node("报名状态查看", fill=LEAF_FILL),
                fill=MODULE_FILL,
            ),
            node(
                "个人活动资产模块",
                node("收藏状态查询", fill=LEAF_FILL),
                node("收藏活动", fill=LEAF_FILL),
                node("取消收藏", fill=LEAF_FILL),
                node("收藏列表查看", fill=LEAF_FILL),
                node("电子票详情", fill=LEAF_FILL),
                node("电子票二维码", fill=LEAF_FILL),
                fill=MODULE_FILL,
            ),
            fill=GROUP_FILL,
        ),
        node(
            "活动协同能力",
            node(
                "活动管理员功能模块",
                node("可管理活动查看", fill=LEAF_FILL),
                node("查看报名名单", fill=LEAF_FILL),
                node("扫码签到", fill=LEAF_FILL),
                node("手动票码核销", fill=LEAF_FILL),
                fill=MODULE_FILL,
            ),
            fill=GROUP_FILL,
        ),
        fill=STUDENT_FILL,
        vertical=True,
    )


def organizer_tree() -> Node:
    return node(
        "组织者端",
        node(
            "活动发布能力",
            node(
                "活动发布模块",
                node("活动创建", fill=LEAF_FILL),
                node("活动编辑", fill=LEAF_FILL),
                node("活动封面上传", fill=LEAF_FILL),
                node("发布选项获取", fill=LEAF_FILL),
                node("提交审核", fill=LEAF_FILL),
                fill=MODULE_FILL,
            ),
            fill=GROUP_FILL,
        ),
        node(
            "活动运营能力",
            node(
                "活动运营模块",
                node("活动列表查看", fill=LEAF_FILL),
                node("报名名单查看", fill=LEAF_FILL),
                node("按状态筛选报名名单", fill=LEAF_FILL),
                fill=MODULE_FILL,
            ),
            node(
                "活动授权模块",
                node("活动管理员列表", fill=LEAF_FILL),
                node("添加活动管理员", fill=LEAF_FILL),
                node("移除活动管理员", fill=LEAF_FILL),
                fill=MODULE_FILL,
            ),
            fill=GROUP_FILL,
        ),
        node(
            "现场服务能力",
            node(
                "签到服务模块",
                node("扫码签到", fill=LEAF_FILL),
                node("手动票码核销", fill=LEAF_FILL),
                fill=MODULE_FILL,
            ),
            fill=GROUP_FILL,
        ),
        fill=ORGANIZER_FILL,
        vertical=True,
    )


def admin_tree() -> Node:
    return node(
        "管理员端",
        node(
            "审核业务能力",
            node(
                "审核管理模块",
                node("待审核活动列表", fill=LEAF_FILL),
                node("审核通过", fill=LEAF_FILL),
                node("审核驳回", fill=LEAF_FILL),
                node("审核理由填写", fill=LEAF_FILL),
                node("审核任务处理", fill=LEAF_FILL),
                fill=MODULE_FILL,
            ),
            node(
                "审核记录模块",
                node("审核意见记录", fill=LEAF_FILL),
                node("审计记录", fill=LEAF_FILL),
                fill=MODULE_FILL,
            ),
            fill=GROUP_FILL,
        ),
        fill=ADMIN_FILL,
        vertical=True,
    )


def main() -> None:
    outputs = [
        create_image(common_tree(), "campus-activity-function-common.png"),
        create_image(student_tree(), "campus-activity-function-student.png"),
        create_image(organizer_tree(), "campus-activity-function-organizer.png"),
        create_image(admin_tree(), "campus-activity-function-admin.png"),
    ]

    for path in outputs:
        print(path)


if __name__ == "__main__":
    main()
