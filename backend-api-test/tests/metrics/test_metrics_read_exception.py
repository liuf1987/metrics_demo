#!/usr/bin/env python3
# -*- coding: utf-8 -*-
# @file         : test_metrics_read_exception.py
# @author       : test
# @date         : 2026/06/03
# @Description  : 查询指标异常流程测试

import pytest
from libs.metrics.metric_api import MetricApi
from config import log_config

log = log_config.get_logger(__file__)


class TestMetricsReadException:
    """查询指标异常流程测试"""

    @pytest.mark.parametrize(
        "metric_code,expected_code,test_name",
        [
            ("non_existent_metric_code_xxx", 3102, "不存在的指标编码"),
            ("", 500, "空指标编码"),
        ],
        ids=["不存在的指标编码", "空指标编码"]
    )
    def test_get_metric_not_found(self, db_reset, metric_code, expected_code, test_name):
        """查询不存在的指标 (3102)"""
        log.info(f"测试场景: {test_name}, 请求参数: metricCode={metric_code}")
        result = MetricApi.get_metric_by_code(metric_code)
        log.info(f"响应结果: code={result.code}, message={result.message}, data={result.data}")
        assert result.code == expected_code
