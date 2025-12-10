package com.auto.test.mapper;

import com.auto.test.entity.TestResult;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;

/**
 * 测试结果的 MyBatis 注解版 Mapper，负责落库执行结果。
 */
@Mapper
public interface TestResultMapper {

    @Insert("INSERT INTO test_result(case_id, status, message, screenshot_path, run_time, duration_ms) " +
            "VALUES(#{caseId}, #{status}, #{message}, #{screenshotPath}, #{runTime}, #{durationMs})")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insertTestResult(TestResult result);

    @Select("SELECT id, case_id, status, message, run_time, duration_ms, screenshot_path FROM test_result ORDER BY run_time DESC")
    java.util.List<TestResult> findAll();

    @Select("SELECT id, case_id, status, message, run_time, duration_ms, screenshot_path FROM test_result WHERE case_id = #{caseId} ORDER BY run_time DESC")
    java.util.List<TestResult> findByCaseId(Long caseId);

    @Select("SELECT id, case_id, status, message, run_time, duration_ms, screenshot_path FROM test_result WHERE id = #{id}")
    TestResult findById(Long id);
}
