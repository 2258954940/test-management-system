// src/main/java/com/auto/test/mapper/TaskMapper.java
package com.auto.test.mapper;

import com.auto.test.entity.Task;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import java.util.List;

public interface TaskMapper extends BaseMapper<Task> {
    // 查询已完成任务
    @Select("SELECT * FROM test_task WHERE status = 'finished' ORDER BY create_time DESC")
    List<Task> selectFinishedTasks();

    // 新增任务
    @Insert("INSERT INTO test_task(task_name, case_id, exec_type, cron_expression, status, create_by) " +
            "VALUES(#{taskName}, #{caseId}, #{execType}, #{cronExpression}, #{status}, #{createBy})")
    int insertTask(Task task);

    // 更新任务状态
    @Update("UPDATE test_task SET status = #{status} WHERE id = #{id}")
    int updateTaskStatus(Integer id, String status);
}