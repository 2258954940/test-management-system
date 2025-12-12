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

    // 改动1：新增SQL添加 creator 字段的插入（解决创建人存不上）
    @Insert("INSERT INTO test_case(name, description, url, locator_type, locator_value, action_type, input_data, expected_result, creator, create_time) " +
            "VALUES(#{name}, #{description}, #{url}, #{locatorType}, #{locatorValue}, #{actionType}, #{inputData}, #{expectedResult}, #{creator}, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insertTestCase(TestCase testCase);

    // 改动2：查询SQL添加 creator 字段（解决创建人列不显示）
        @Select("SELECT id, name, description, url, locator_type, locator_value, action_type, input_data, expected_result, creator, create_time, update_time " +
                "FROM test_case WHERE id = #{id}")
        TestCase findById(Long id);

    // 改动3：查询所有SQL添加 creator 字段（解决创建人列不显示）
        @Select("SELECT id, name, description, url, locator_type, locator_value, action_type, input_data, expected_result, creator, create_time, update_time " +
                "FROM test_case ORDER BY create_time DESC")
        List<TestCase> findAll();

    // 改动4：更新SQL添加 creator 字段（编辑时 creator 禁用，可加可不加，建议加上）
    @Update("UPDATE test_case SET name=#{name}, description=#{description}, url=#{url}, locator_type=#{locatorType}, locator_value=#{locatorValue}, " +
            "action_type=#{actionType}, input_data=#{inputData}, expected_result=#{expectedResult}, creator=#{creator} WHERE id=#{id}")
    int updateTestCase(TestCase testCase);

    @Delete("DELETE FROM test_case WHERE id = #{id}")
    int deleteTestCase(Long id);

    
}