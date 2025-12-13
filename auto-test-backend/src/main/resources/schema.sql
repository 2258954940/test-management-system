-- MySQL 建表脚本，包含测试用例与执行结果两张表

CREATE TABLE IF NOT EXISTS test_case (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键，自增ID',
    name VARCHAR(100) NOT NULL COMMENT '用例名称',
    description VARCHAR(255) NULL COMMENT '用例描述',
    url VARCHAR(255) NOT NULL COMMENT '目标地址',
    locator_type VARCHAR(20) NOT NULL COMMENT '定位方式：id/name/xpath',
    locator_value VARCHAR(255) NOT NULL COMMENT '定位值',
    action_type VARCHAR(20) NOT NULL COMMENT '动作类型：input/click',
    input_data VARCHAR(255) NULL COMMENT '输入内容，click 时可为空',
    expected_result VARCHAR(255) NOT NULL COMMENT '期望结果描述',
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='测试用例表';

CREATE TABLE IF NOT EXISTS test_result (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键，自增ID',
    case_id BIGINT NOT NULL COMMENT '关联的用例ID',
    status VARCHAR(20) NOT NULL COMMENT '执行状态：PASS/FAILED',
    message VARCHAR(500) NULL COMMENT '执行信息/错误原因',
    screenshot_path VARCHAR(255) NULL COMMENT '截图路径',
    run_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '执行时间',
    duration_ms BIGINT NULL COMMENT '执行耗时，毫秒',
    CONSTRAINT fk_case FOREIGN KEY (case_id) REFERENCES test_case(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='测试执行结果表';

DROP TABLE IF EXISTS `test_element`;
CREATE TABLE `test_element` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '元素ID（主键）',
  `element_name` varchar(100) NOT NULL COMMENT '元素名称（如“百度搜索框”）',
  `page_url` varchar(255) NOT NULL COMMENT '所属页面URL（如https://www.baidu.com）',
  `locator_type` varchar(20) NOT NULL COMMENT '定位器类型（id/name/xpath等）',
  `locator_value` varchar(255) NOT NULL COMMENT '定位器值（如kw、//input[@id="kw"]）',
  `widget_type` varchar(20) DEFAULT '' COMMENT '控件类型（button/input/select等）',
  `creator` varchar(50) NOT NULL DEFAULT 'admin' COMMENT '创建人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='元素管理表';