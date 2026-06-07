#!/usr/bin/env python3
# -*- coding: utf-8 -*-
# @file         : conftest.py
# @author       : test
# @date         : 2026/06/03
# @Description  : pytest配置文件

import pytest
import os
import shutil
import allure
from common.utils.sqlutil import SqlUtil


def reset_database():
    """
    重置数据库并加载元数据的公共函数
    """
    print("\n" + "-" * 60)
    print("正在重置数据库...")

    # 重置数据库
    SqlUtil.reset_database_from_init_sql()

    # 调用load_metadata重新加载元数据
    try:
        from libs.metadata.metadata_api import MetadataApi
        result = MetadataApi.load_metadata()
        if result.code == 200:
            print("✅ 数据库重置成功，元数据加载完成")
        else:
            print(f"⚠️  元数据加载失败: {result.code} - {result.message}")
    except Exception as e:
        print(f"⚠️  调用load_metadata失败（可能后端服务未启动）: {e}")

    print("-" * 60 + "\n")


def pytest_configure(config):
    """
    pytest 配置钩子
    """
    # 配置 allure 报告环境信息
    allure_dir = config.option.allure_report_dir
    if allure_dir:
        env_file = os.path.join(allure_dir, "environment.properties")
        os.makedirs(allure_dir, exist_ok=True)
        
        with open(env_file, "w", encoding="utf-8") as f:
            f.write("Project=Data Dashboard Backend API Test\n")
            f.write("TestType=API Test\n")


@pytest.hookimpl(tryfirst=True, hookwrapper=True)
def pytest_runtest_makereport(item, call):
    """
    钩子函数：获取测试用例执行结果
    """
    outcome = yield
    rep = outcome.get_result()
    
    # 设置测试用例执行结果的属性
    setattr(item, "rep_" + rep.when, rep)


@pytest.fixture(scope="function", autouse=True)
def allure_attachments(request):
    """
    自动添加测试信息到 allure 报告
    """
    # 添加测试用例的模块和类信息到 allure 报告
    module_name = request.module.__name__
    class_name = request.cls.__name__ if request.cls else ""
    test_name = request.function.__name__
    
    # 设置 allure 报告的 epic、feature、story 层级
    allure.dynamic.epic("API 测试")
    
    # 从模块名中提取功能模块名称
    if "tests." in module_name:
        feature_name = module_name.split("tests.")[-1].split(".")[0].capitalize()
        allure.dynamic.feature(feature_name)
    
    if class_name:
        allure.dynamic.story(class_name)
    
    allure.dynamic.title(test_name)
    
    yield
    
    # 测试结束后，如果失败则添加更多信息
    if hasattr(request.node, 'rep_call') and request.node.rep_call.failed:
        try:
            # 可以在这里添加截图等附件
            pass
        except Exception as e:
            print(f"添加 allure 附件失败: {str(e)}")


@pytest.fixture(scope="session", autouse=True)
def db_reset_session_start():
    """
    测试会话启动时重置数据库
    """
    print("\n" + "=" * 80)
    print("测试会话开始，初始化数据库...")
    print("=" * 80)
    reset_database()

    yield

    # 会话结束时也重置（通过pytest_sessionfinish钩子）


@pytest.fixture(scope="session")
def api_client():
    """
    API客户端fixture
    :return: None
    """
    # 确保报告目录存在
    report_dir = os.path.join(os.path.dirname(__file__), 'report')
    os.makedirs(report_dir, exist_ok=True)

    # 确保日志目录存在
    log_dir = os.path.join(os.path.dirname(__file__), 'logs')
    os.makedirs(log_dir, exist_ok=True)

    yield


@pytest.fixture(scope="function", autouse=True)
def db_reset_after_test():
    """
    每个测试用例执行完后重置数据库
    """
    yield

    # 测试用例结束后重置数据库
    print(f"\n测试用例执行完毕，重置数据库...")
    reset_database()


@pytest.fixture(autouse=False)
def db_reset():
    """
    数据库重置fixture（保留原接口，向后兼容）
    在测试前和测试后都执行重置
    """
    # 测试前重置
    print("\n使用 db_reset fixture，测试前重置数据库...")
    reset_database()

    yield

    # 测试后重置
    print("\n测试结束，重置数据库...")
    reset_database()


def pytest_sessionfinish(session, exitstatus):
    """
    pytest整个测试会话结束时的钩子函数
    在所有测试用例执行完毕后重置数据库，确保环境恢复初始状态
    """
    print("\n" + "=" * 80)
    print("测试会话结束，最终重置数据库至初始状态...")
    print("=" * 80)

    reset_database()

    print("=" * 80)
    print("✅ 所有测试完成，数据库已恢复初始状态")
    print("=" * 80 + "\n")
