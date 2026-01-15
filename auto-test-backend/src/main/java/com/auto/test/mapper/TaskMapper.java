package com.auto.test.mapper;

import com.auto.test.entity.Task;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;

@Mapper
public interface TaskMapper extends BaseMapper<Task> {
    // 查询所有已完成的任务（前端下拉框用）
    List<Task> selectFinishedTasks();
}