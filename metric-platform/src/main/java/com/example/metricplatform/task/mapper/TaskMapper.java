package com.example.metricplatform.task.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.metricplatform.task.entity.Task;
import org.apache.ibatis.annotations.Mapper;

/**
 * 任务 Mapper 接口
 */
@Mapper
public interface TaskMapper extends BaseMapper<Task> {
}
