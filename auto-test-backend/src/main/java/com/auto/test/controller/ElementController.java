// auto-test-backend/src/main/java/com/auto/test/controller/ElementController.java
package com.auto.test.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.auto.test.entity.Element;
import com.auto.test.service.ElementService;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired; 
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/element")
public class ElementController {
    @Autowired
    private ElementService elementService;
    private static final Logger log = LoggerFactory.getLogger(ElementController.class);

    // 新增元素：加日志
    @PostMapping
    public Map<String, Object> addElement(@RequestBody Element element) {
        log.info("=== 接收新增元素请求，参数：{} ===", element); // 新增日志
        try {
            Element newElement = elementService.addElement(element);
            log.info("新增元素成功，ID：{}", newElement.getId()); // 新增日志
            return Map.of("code", 200, "msg", "新增元素成功", "data", newElement);
        } catch (Exception e) {
            log.error("新增元素失败！", e); // 新增日志（打印异常堆栈）
            return Map.of("code", 500, "msg", "新增元素失败：" + e.getMessage());
        }
    }

    // 编辑元素：加日志
    @PutMapping("/{id}")
    public Map<String, Object> updateElement(@PathVariable Long id, @RequestBody Element element) {
        log.info("=== 接收编辑元素请求，ID：{}，参数：{} ===", id, element); // 新增日志
        try {
            elementService.updateElement(id, element);
            log.info("编辑元素成功，ID：{}", id); // 新增日志
            return Map.of("code", 200, "msg", "编辑元素成功");
        } catch (Exception e) {
            log.error("编辑元素失败！ID：{}", id, e); // 新增日志（打印异常堆栈）
            return Map.of("code", 500, "msg", "编辑元素失败：" + e.getMessage());
        }
    }


    // 查询所有元素（表格展示）
    @GetMapping
    public Map<String, Object> listElements() {
        try {
            List<Element> elements = elementService.listAllElements();
            return Map.of("code", 200, "msg", "查询成功", "data", elements);
        } catch (Exception e) {
            return Map.of("code", 500, "msg", "查询失败：" + e.getMessage());
        }
    }

    // 查询单个元素（编辑回显）
    @GetMapping("/{id}")
    public Map<String, Object> getElement(@PathVariable Long id) {
        try {
            Element element = elementService.getElementById(id);
            return Map.of("code", 200, "msg", "查询成功", "data", element);
        } catch (Exception e) {
            return Map.of("code", 500, "msg", "查询失败：" + e.getMessage());
        }
    }

    // 删除元素（已有，不用改）
    @DeleteMapping("/{id}")
    public Map<String, Object> deleteElement(@PathVariable Long id) {
        try {
            elementService.deleteElement(id);
            return Map.of("code", 200, "msg", "删除成功");
        } catch (Exception e) {
            return Map.of("code", 500, "msg", "删除失败：" + e.getMessage());
        }
    }

    // 批量导入元素接口
    @PostMapping("/batchImport")
    public Map<String, Object> batchImportElements(@RequestBody Map<String, List<Element>> request) {
    try {
        List<Element> elements = request.get("list");
        if (elements == null || elements.isEmpty()) {
        return Map.of("code", 400, "msg", "导入列表不能为空");
        }
        elementService.batchImportElements(elements);
        return Map.of("code", 200, "msg", "批量导入成功");
    } catch (Exception e) {
        log.error("批量导入元素失败！", e);
        return Map.of("code", 500, "msg", "批量导入失败：" + e.getMessage());
    }
    }
}