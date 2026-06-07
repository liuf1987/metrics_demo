#!/usr/bin/env python3
# -*- coding: utf-8 -*-
# @file         : log_config.py
# @author       : test
# @date         : 2026/06/03
# @Description  : 日志配置

import logging
import os
from logging.handlers import RotatingFileHandler


def get_logger(name):
    """
    获取日志记录器
    :param name: 日志名称
    :return: logger对象
    """
    logger = logging.getLogger(name)
    logger.setLevel(logging.DEBUG)

    # 避免重复添加handler
    if logger.handlers:
        return logger

    # 创建日志目录
    log_dir = os.path.join(os.path.dirname(os.path.dirname(__file__)), 'logs')
    os.makedirs(log_dir, exist_ok=True)

    # 控制台handler
    console_handler = logging.StreamHandler()
    console_handler.setLevel(logging.DEBUG)

    # 文件handler（轮转）
    file_handler = RotatingFileHandler(
        os.path.join(log_dir, 'api-test.log'),
        maxBytes=1024 * 1024 * 50,  # 50MB
        backupCount=5,
        encoding='utf-8'
    )
    file_handler.setLevel(logging.INFO)

    # 格式化器
    formatter = logging.Formatter(
        '%(asctime)s - %(name)s - %(levelname)s - %(message)s'
    )
    console_handler.setFormatter(formatter)
    file_handler.setFormatter(formatter)

    logger.addHandler(console_handler)
    logger.addHandler(file_handler)

    return logger
