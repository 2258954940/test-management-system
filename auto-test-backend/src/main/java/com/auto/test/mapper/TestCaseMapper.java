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

    // 改动1：INSERT 加 element_ids 字段（核心！存入关联元素ID）
    @Insert("INSERT INTO test_case(name, description, url, locator_type, locator_value, action_type, input_data, expected_result, creator, element_ids, create_time) " +
            "VALUES(#{name}, #{description}, #{url}, #{locatorType}, #{locatorValue}, #{actionType}, #{inputData}, #{expectedResult}, #{creator}, #{elementIds}, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insertTestCase(TestCase testCase);

    // 改动2：SELECT 加 element_ids 字段（查询时能读到关联元素ID）
    @Select("SELECT id, name, description, url, locator_type, locator_value, action_type, input_data, expected_result, creator, element_ids, create_time, update_time " +
            "FROM test_case WHERE id = #{id}")
    TestCase findById(Long id);

    // 改动3：SELECT ALL 加 element_ids 字段（列表查询也能读到）
    @Select("SELECT id, name, description, url, locator_type, locator_value, action_type, input_data, expected_result, creator, element_ids, create_time, update_time " +
            "FROM test_case ORDER BY create_time DESC")
    List<TestCase> findAll();

    // 改动4：UPDATE 加 element_ids 字段（编辑时更新关联元素ID）
    @Update("UPDATE test_case SET name=#{name}, description=#{description}, url=#{url}, locator_type=#{locatorType}, locator_value=#{locatorValue}, " +
            "action_type=#{actionType}, input_data=#{inputData}, expected_result=#{expectedResult}, creator=#{creator}, element_ids=#{elementIds} WHERE id=#{id}")
    int updateTestCase(TestCase testCase);

    @Delete("DELETE FROM test_case WHERE id = #{id}")
    int deleteTestCase(Long id);
    
}