#!/usr/bin/env python3
# -*- coding: utf-8 -*-
# @file         : test_metrics_delete_exception.py
# @author       : test
# @date         : 2026/06/03
# @Description  : 删除指标异常流程测试

import pytest
from libs.metrics.metric_api import MetricApi
from config import log_config

log = log_config.get_logger(__file__)


class TestMetricsDeleteException:
    """删除指标异常流程测试"""

    @pytest.mark.parametrize(
        "metric_id,expected_code,test_name",
        [
            (999999, 3001, "不存在的指标ID"),
            (0, 3001, "指标ID为0"),
        ],
        ids=["不存在的指标ID", "指标ID为0"]
    )
    def test_delete_metric_not_found(self, db_reset, metric_id, expected_code, test_name):
        """删除不存在的指标 (3001)"""
        log.info(f"测试场景: {test_name}, 请求参数: id={metric_id}")
        result = MetricApi.delete_metric(metric_id)
        log.info(f"响应结果: code={result.code}, message={result.message}, data={result.data}")
        assert result.code == expected_code
