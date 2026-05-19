#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
校园活动管理平台 - 架构图渲染器
使用 Playwright 将 Mermaid HTML 渲染为 PNG 图片
"""

import subprocess
import sys
import os
import time

def install_playwright():
    """安装 Playwright"""
    try:
        import playwright
        print("Playwright 已安装")
    except ImportError:
        print("正在安装 Playwright...")
        subprocess.check_call([sys.executable, "-m", "pip", "install", "playwright"])
        subprocess.check_call([sys.executable, "-m", "playwright", "install", "chromium"])
        print("Playwright 安装完成")

def render_html_to_png(html_path, png_path, width=1920, height=1080):
    """使用 Playwright 将 HTML 渲染为 PNG"""
    from playwright.sync_api import sync_playwright

    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        page = browser.new_page(viewport={'width': width, 'height': height})

        # 加载 HTML 文件
        page.goto(f'file:///{html_path}', wait_until='networkidle')

        # 等待 Mermaid 渲染完成
        page.wait_for_timeout(2000)

        # 获取内容高度并调整视口
        content_height = page.evaluate('document.body.scrollHeight')
        if content_height > height:
            page.set_viewport_size({'width': width, 'height': content_height + 100})
            page.wait_for_timeout(500)

        # 截图
        page.screenshot(path=png_path, full_page=True)
        browser.close()
        print(f"已生成: {png_path}")

def main():
    output_dir = r"C:\Users\Administrator\Desktop\毕业设计\photos"

    # 确保输出目录存在
    os.makedirs(output_dir, exist_ok=True)

    # HTML 文件列表
    html_files = [
        ("系统架构图.html", "系统架构图.png"),
        ("功能模块图.html", "功能模块图.png"),
        ("数据库ER图.html", "数据库ER图.png"),
    ]

    # 安装 Playwright
    install_playwright()

    # 渲染每个 HTML 文件
    for html_name, png_name in html_files:
        html_path = os.path.join(output_dir, html_name)
        png_path = os.path.join(output_dir, png_name)

        if os.path.exists(html_path):
            print(f"正在渲染 {html_name}...")
            try:
                render_html_to_png(html_path, png_path)
            except Exception as e:
                print(f"渲染 {html_name} 时出错: {e}")
        else:
            print(f"未找到 {html_path}")

    print("\n所有架构图已生成完成！")
    print(f"输出目录: {output_dir}")

if __name__ == "__main__":
    main()
