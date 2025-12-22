package com.auto.test.service;

import com.auto.test.entity.SiteTestConfigDO;
import java.util.List; // 新增：导入List

public interface SiteTestConfigService {
    /**
     * 根据网站标识查询配置
     */
    SiteTestConfigDO getBySiteCode(String siteCode);

    /**
     * 查询所有网站配置（给前端下拉框用）
     */
    List<SiteTestConfigDO> listAll(); // 新增：核心缺失方法
}