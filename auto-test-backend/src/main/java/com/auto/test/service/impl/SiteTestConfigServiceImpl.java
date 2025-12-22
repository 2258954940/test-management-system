package com.auto.test.service.impl;

import com.auto.test.entity.SiteTestConfigDO;
import com.auto.test.mapper.SiteTestConfigMapper;
import com.auto.test.service.SiteTestConfigService;
import org.springframework.beans.factory.annotation.Autowired; // 替换为Spring注解
import org.springframework.stereotype.Service;

import java.util.List;

@Service // 确保Spring能扫描到（包路径要在@SpringBootApplication扫描范围内）
public class SiteTestConfigServiceImpl implements SiteTestConfigService {

    // 替换@Resource为@Autowired，Spring更推荐，避免JDK注解兼容问题
    @Autowired
    private SiteTestConfigMapper siteTestConfigMapper;

    @Override
    public SiteTestConfigDO getBySiteCode(String siteCode) {
        return siteTestConfigMapper.selectBySiteCode(siteCode);
    }

    // 新增：实现listAll方法
    @Override
    public List<SiteTestConfigDO> listAll() {
        return siteTestConfigMapper.selectList(null); // MyBatis-Plus通用方法（推荐）
        // 如果是原生MyBatis，替换为：return siteTestConfigMapper.selectAll();
    }
}