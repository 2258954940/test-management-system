package com.auto.test.entity;

import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;
/**
 * 测试执行结果实体，对应表 test_result。
 */
@Data // 关键：lombok自动生成get/set/toString等方法
@TableName("test_result") 
public class TestResult {

    private Long id;
    private Long caseId;
    private String status;
    private String message;
    private String screenshotPath;
    private LocalDateTime runTime;
    private Long durationMs;
    private String verifyDetail;
  
}
