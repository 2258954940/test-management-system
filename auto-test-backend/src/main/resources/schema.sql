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
