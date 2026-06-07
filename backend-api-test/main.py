#!/usr/bin/env python3
# -*- coding: utf-8 -*-
# @file         : main.py
# @author       : test
# @date         : 2026/06/07
# @Description  : pytest全局测试执行入口

import pytest
import sys
import os
import subprocess


def generate_allure_report():
    """
    生成 Allure 报告
    """
    allure_results_dir = "./report/allure"
    allure_report_dir = "./report/allure-report"
    
    print("\n" + "=" * 60)
    print("正在生成 Allure 报告...")
    print("=" * 60)
    
    try:
        # 检查 allure 命令是否可用
        subprocess.run(["allure", "--version"], check=True, capture_output=True, text=True)
        
        # 生成报告
        cmd = [
            "allure", "generate", 
            allure_results_dir, 
            "-o", allure_report_dir, 
            "--clean"
        ]
        subprocess.run(cmd, check=True)
        
        print(f"\n✅ Allure 报告已生成: {os.path.abspath(allure_report_dir)}")
        print(f"📂 报告入口: {os.path.abspath(os.path.join(allure_report_dir, 'index.html'))}")
        print("\n💡 提示: 使用以下命令打开报告:")
        print(f"   allure open {allure_report_dir}")
        print("=" * 60 + "\n")
        
        return True
    except FileNotFoundError:
        print("\n⚠️  Allure 命令行工具未安装或未添加到 PATH")
        print("💡 请先安装 Allure: https://docs.qameta.io/allure/")
        print("=" * 60 + "\n")
        return False
    except subprocess.CalledProcessError as e:
        print(f"\n❌ 生成 Allure 报告失败: {e}")
        print("=" * 60 + "\n")
        return False


def open_allure_report():
    """
    打开 Allure 报告
    """
    allure_report_dir = "./report/allure-report"
    
    if not os.path.exists(allure_report_dir):
        print("\n⚠️  Allure 报告目录不存在，请先生成报告")
        return False
    
    try:
        print("\n正在打开 Allure 报告...")
        subprocess.run(["allure", "open", allure_report_dir], check=True)
        return True
    except Exception as e:
        print(f"\n❌ 打开 Allure 报告失败: {e}")
        return False


if __name__ == '__main__':
    """
    pytest全局测试执行入口
    
    命令行参数：
    - 无参数：运行所有测试并生成 pytest-html 报告
    --allure：运行所有测试并自动生成 Allure 报告
    --open-allure：运行所有测试，生成并自动打开 Allure 报告
    --gen-report：仅从现有结果生成 Allure 报告
    --open-report：仅打开已有的 Allure 报告
    
    其他 pytest 参数：参见 pytest --help
    """
    args = sys.argv[1:]
    run_allure = False
    open_report = False
    only_gen_report = False
    only_open_report = False
    
    # 处理自定义参数
    filtered_args = []
    for arg in args:
        if arg == "--allure":
            run_allure = True
        elif arg == "--open-allure":
            run_allure = True
            open_report = True
        elif arg == "--gen-report":
            only_gen_report = True
        elif arg == "--open-report":
            only_open_report = True
        else:
            filtered_args.append(arg)
    
    # 仅生成报告模式
    if only_gen_report:
        generate_allure_report()
        sys.exit(0)
    
    # 仅打开报告模式
    if only_open_report:
        open_allure_report()
        sys.exit(0)
    
    # 运行测试
    exit_code = pytest.main(filtered_args)
    
    # 生成 Allure 报告
    if run_allure:
        if exit_code == 0 or exit_code == 1:  # 0=全部通过, 1=有测试失败
            generate_allure_report()
            if open_report:
                open_allure_report()
    
    sys.exit(exit_code)
