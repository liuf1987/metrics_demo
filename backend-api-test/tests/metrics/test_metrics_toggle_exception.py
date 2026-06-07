#!/usr/bin/env python3
# -*- coding: utf-8 -*-
# @file         : test_metrics_toggle_exception.py
# @author       : test
# @date         : 2026/06/03
# @Description  : 启用/停用指标异常流程测试

import pytest
from libs.metrics.metric_api import MetricApi
from config import log_config

log = log_config.get_logger(__file__)


class TestMetricsToggleException:
    """启用/停用指标异常流程测试"""

    @pytest.mark.parametrize(
        "metric_id,enabled,expected_code,test_name",
        [
            (999999, True, 3001, "不存在的指标ID-启用"),
            (999999, False, 3001, "不存在的指标ID-停用"),
            (0, True, 3001, "指标ID为0-启用"),
        ],
        ids=["不存在的指标ID-启用", "不存在的指标ID-停用", "指标ID为0-启用"]
    )
    def test_toggle_metric_not_found(self, db_reset, metric_id, enabled, expected_code, test_name):
        """启用/停用不存在的指标 (3001)"""
        log.info(f"测试场景: {test_name}, 请求参数: id={metric_id}, enabled={enabled}")
        result = MetricApi.toggle_metric(metric_id, enabled)
        log.info(f"响应结果: code={result.code}, message={result.message}, data={result.data}")
        assert result.code == expected_code
