package com.auto.test.mapper;

import com.auto.test.entity.TestResult;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;
import com.auto.test.dto.TestReportExcelDTO;

/**
 * 测试结果的 MyBatis 注解版 Mapper，负责落库执行结果。
 */
public interface TestResultMapper {

        @Insert("INSERT INTO test_result(case_id, task_id, status, message, screenshot_path, run_time, duration_ms) " +
            "VALUES(#{caseId}, #{taskId}, #{status}, #{message}, #{screenshotPath}, #{runTime}, #{durationMs})")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insertTestResult(TestResult result);

    @Select("SELECT id, case_id, task_id, status, message, run_time, duration_ms, screenshot_path FROM test_result ORDER BY run_time DESC")
    java.util.List<TestResult> findAll();

    @Select("SELECT id, case_id, task_id, status, message, run_time, duration_ms, screenshot_path FROM test_result WHERE case_id = #{caseId} ORDER BY run_time DESC")
    java.util.List<TestResult> findByCaseId(Long caseId);

    @Select("SELECT id, case_id, task_id, status, message, run_time, duration_ms, screenshot_path FROM test_result WHERE id = #{id}")
    TestResult findById(Long id);

    // 新增：按任务查询执行结果
    @Select("SELECT id, case_id, status, message, screenshot_path, run_time, duration_ms, task_id FROM test_result WHERE task_id = #{taskId} ORDER BY run_time DESC")
    List<TestResult> findByTaskId(Long taskId);

    // 新增：统计任务成功/失败数
    @Select("SELECT COUNT(*) FROM test_result WHERE task_id = #{taskId} AND status = 'PASS'")
    Integer countSuccessByTaskId(Long taskId);

    @Select("SELECT COUNT(*) FROM test_result WHERE task_id = #{taskId} AND status = 'FAILED'")
    Integer countFailByTaskId(Long taskId);

        /**
         * 导出报告所需的关联查询结果（XML Mapper定义）。
         */
        List<TestReportExcelDTO> selectReportRows();

    List<TestReportExcelDTO> selectReportRowsByTaskId(Long taskId);
}
