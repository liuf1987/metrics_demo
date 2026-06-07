package com.example.metricplatform.task.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.metricplatform.task.entity.TaskExecutionResult;
import org.apache.ibatis.annotations.Mapper;

/**
 * 任务执行结果 Mapper 接口
 */
@Mapper
public interface TaskExecutionResultMapper extends BaseMapper<TaskExecutionResult> {
}
