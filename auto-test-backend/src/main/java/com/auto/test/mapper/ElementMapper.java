package com.auto.test.mapper;

import com.auto.test.entity.Element;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.annotations.Delete;

import java.util.List;

public interface ElementMapper {
    // 新增元素：把creator→create_by，去掉update_time
    @Insert("INSERT INTO test_element(element_name, page_url, locator_type, locator_value, widget_type, create_by, create_time) " +
            "VALUES(#{elementName}, #{pageUrl}, #{locatorType}, #{locatorValue}, #{widgetType}, #{createBy}, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insertElement(Element element);

    // 查询单个元素：去掉update_time，creator→create_by
    @Select("SELECT id, element_name, page_url, locator_type, locator_value, widget_type, create_by, create_time " +
            "FROM test_element WHERE id = #{id}")
    Element findById(Long id);

    // 查询所有元素：去掉update_time，creator→create_by
    @Select("SELECT id, element_name, page_url, locator_type, locator_value, widget_type, create_by, create_time " +
            "FROM test_element ORDER BY create_time DESC")
    List<Element> findAll();

    // 编辑元素：creator→create_by，去掉update_time
    @Update("UPDATE test_element SET element_name=#{elementName}, page_url=#{pageUrl}, locator_type=#{locatorType}, " +
            "locator_value=#{locatorValue}, widget_type=#{widgetType}, create_by=#{createBy} WHERE id=#{id}")
    int updateElement(Element element);

    // 删除元素（无需修改）
    @Delete("DELETE FROM test_element WHERE id = #{id}")
    int deleteElement(Long id);

    // 批量插入元素（用MyBatis foreach实现）
@Insert("<script>" +
        "INSERT INTO test_element(element_name, page_url, locator_type, locator_value, widget_type, create_by, create_time) " +
        "VALUES " +
        "<foreach collection='list' item='element' separator=','>" +
        "(#{element.elementName}, #{element.pageUrl}, #{element.locatorType}, #{element.locatorValue}, #{element.widgetType}, #{element.createBy}, NOW())" +
        "</foreach>" +
        "</script>")
int batchInsertElements(@Param("list") List<Element> elements);
}