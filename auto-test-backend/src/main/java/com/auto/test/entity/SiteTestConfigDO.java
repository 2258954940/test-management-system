package com.auto.test.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 多网站测试配置实体（对应site_test_config表）
 */
@Data
@TableName("site_test_config") // 关联数据库表名
public class SiteTestConfigDO {
    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 网站唯一标识（如BAIDU/TAOBAO）
     */
    private String siteCode;

    /**
     * 网站名称
     */
    private String siteName;

    /**
     * 首页URL
     */
    private String homeUrl;

    /**
     * 登录页URL
     */
    private String loginPageUrl;

    /**
     * 用户信息API地址
     */
    private String userInfoApi;

    /**
     * 登录态Cookie名称
     */
    private String loginCookieName;

    /**
     * 昵称元素ID
     */
    private String nicknameElementId;

    /**
     * 昵称JsonPath路径
     */
    private String nicknameJsonPath;

    /**
     * 账号输入框ID
     */
    private String usernameElementId;

    /**
     * 密码输入框ID
     */
    private String passwordElementId;

    /**
     * 登录按钮ID
     */
    private String loginBtnElementId;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}