package com.example.metricplatform.task.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.metricplatform.task.entity.TaskMetricItem;
import org.apache.ibatis.annotations.Mapper;

/**
 * 任务子指标 Mapper 接口
 */
@Mapper
public interface TaskMetricItemMapper extends BaseMapper<TaskMetricItem> {
}
