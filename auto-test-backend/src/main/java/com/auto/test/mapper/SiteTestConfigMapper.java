package com.auto.test.mapper;

import com.auto.test.entity.SiteTestConfigDO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper; // MyBatis-Plus核心
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository // 必须加，否则Spring无法扫描到Mapper
public interface SiteTestConfigMapper extends BaseMapper<SiteTestConfigDO> {

    /**
     * 根据网站标识查询配置
     */
    @Select("SELECT * FROM site_test_config WHERE site_code = #{siteCode}")
    SiteTestConfigDO selectBySiteCode(String siteCode);

    // 可选：如果不用MyBatis-Plus的selectList，手动写原生查询（二选一）
    @Select("SELECT * FROM site_test_config ORDER BY create_time DESC")
    List<SiteTestConfigDO> selectAll();
}