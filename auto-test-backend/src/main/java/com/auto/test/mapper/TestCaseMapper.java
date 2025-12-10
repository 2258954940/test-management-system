package com.auto.test.mapper;

import com.auto.test.entity.TestCase;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.annotations.Delete;

import java.util.List;

/**
 * 测试用例的 MyBatis 注解版 Mapper，负责新增和查询。
 */
// 核心：删除 @Mapper 注解（启动类 @MapperScan 已扫描，避免双重注册）
public interface TestCaseMapper {

    @Insert("INSERT INTO test_case(name, description, url, locator_type, locator_value, action_type, input_data, expected_result, create_time) " +
            "VALUES(#{name}, #{description}, #{url}, #{locatorType}, #{locatorValue}, #{actionType}, #{inputData}, #{expectedResult}, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insertTestCase(TestCase testCase);

    @Select("SELECT id, name, description, url, locator_type, locator_value, action_type, input_data, expected_result, create_time " +
            "FROM test_case WHERE id = #{id}")
    TestCase findById(Long id);

    @Select("SELECT id, name, description, url, locator_type, locator_value, action_type, input_data, expected_result, create_time " +
            "FROM test_case ORDER BY create_time DESC")
    List<TestCase> findAll();

    @Update("UPDATE test_case SET name=#{name}, description=#{description}, url=#{url}, locator_type=#{locatorType}, locator_value=#{locatorValue}, " +
            "action_type=#{actionType}, input_data=#{inputData}, expected_result=#{expectedResult} WHERE id=#{id}")
    int updateTestCase(TestCase testCase);

    @Delete("DELETE FROM test_case WHERE id = #{id}")
    int deleteTestCase(Long id);
}