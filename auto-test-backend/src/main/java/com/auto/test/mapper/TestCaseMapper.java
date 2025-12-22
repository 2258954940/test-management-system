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
public interface TestCaseMapper {

    // 核心修改：INSERT 语句补充断言+登录相关字段
    @Insert("INSERT INTO test_case(" +
            "name, description, url, locator_type, locator_value, action_type, " +
            "input_data, expected_result, creator, element_ids, " +
            "assert_type, assert_locator_type, assert_locator_value, assert_expected_value, " +
            "need_login, site_code, create_time, update_time" +
            ") VALUES(" +
            "#{name}, #{description}, #{url}, #{locatorType}, #{locatorValue}, #{actionType}, " +
            "#{inputData}, #{expectedResult}, #{creator}, #{elementIds}, " +
            "#{assertType}, #{assertLocatorType}, #{assertLocatorValue}, #{assertExpectedValue}, " +
            "#{needLogin}, #{siteCode}, NOW(), NOW()" +
            ")")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insertTestCase(TestCase testCase);

    // 核心修改：SELECT 语句补充断言+登录相关字段
    @Select("SELECT " +
            "id, name, description, url, locator_type, locator_value, action_type, " +
            "input_data, expected_result, creator, element_ids, " +
            "assert_type, assert_locator_type, assert_locator_value, assert_expected_value, " +
            "need_login, site_code, create_time, update_time " +
            "FROM test_case WHERE id = #{id}")
    TestCase findById(Long id);

    // 核心修改：SELECT ALL 补充断言+登录相关字段
    @Select("SELECT " +
            "id, name, description, url, locator_type, locator_value, action_type, " +
            "input_data, expected_result, creator, element_ids, " +
            "assert_type, assert_locator_type, assert_locator_value, assert_expected_value, " +
            "need_login, site_code, create_time, update_time " +
            "FROM test_case ORDER BY create_time DESC")
    List<TestCase> findAll();

    // 核心修改：UPDATE 语句补充断言+登录相关字段
    @Update("UPDATE test_case SET " +
            "name=#{name}, description=#{description}, url=#{url}, " +
            "locator_type=#{locatorType}, locator_value=#{locatorValue}, action_type=#{actionType}, " +
            "input_data=#{inputData}, expected_result=#{expectedResult}, creator=#{creator}, " +
            "element_ids=#{elementIds}, " +
            "assert_type=#{assertType}, assert_locator_type=#{assertLocatorType}, " +
            "assert_locator_value=#{assertLocatorValue}, assert_expected_value=#{assertExpectedValue}, " +
            "need_login=#{needLogin}, site_code=#{siteCode}, " +
            "update_time=NOW() " +
            "WHERE id=#{id}")
    int updateTestCase(TestCase testCase);

    @Delete("DELETE FROM test_case WHERE id = #{id}")
    int deleteTestCase(Long id);
    
}