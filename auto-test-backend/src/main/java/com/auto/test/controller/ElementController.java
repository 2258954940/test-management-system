package com.auto.test.controller;

import com.auto.test.dto.ElementParseRequest;
import com.auto.test.entity.Element;
import com.auto.test.service.ElementService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/element")
public class ElementController {
    @Autowired
    private ElementService elementService;
    private static final Logger log = LoggerFactory.getLogger(ElementController.class);

    // DOM解析接口
    @PostMapping("/parse")
    public Map<String, Object> parseUrlElements(@Valid @RequestBody ElementParseRequest request) {
        log.info("=== 接收DOM解析请求，URL：{} ===", request.getUrl());
        try {
            List<Element> elements = elementService.parseUrlElements(request);
            log.info("解析成功，提取到{}个元素", elements.size());
            return Map.of("code", 200, "msg", "解析成功", "data", elements);
        } catch (Exception e) {
            log.error("解析URL失败！URL：{}", request.getUrl(), e);
            return Map.of("code", 500, "msg", "解析失败：" + e.getMessage());
        }
    }

    // 新增元素
    @PostMapping
    public Map<String, Object> addElement(@RequestBody Element element) {
        log.info("=== 接收新增元素请求，参数：{} ===", element);
        try {
            Element newElement = elementService.addElement(element);
            log.info("新增元素成功，ID：{}", newElement.getId());
            return Map.of("code", 200, "msg", "新增元素成功", "data", newElement);
        } catch (Exception e) {
            log.error("新增元素失败！", e);
            return Map.of("code", 500, "msg", "新增元素失败：" + e.getMessage());
        }
    }

    // 编辑元素
    @PutMapping("/{id}")
    public Map<String, Object> updateElement(@PathVariable Long id, @RequestBody Element element) {
        log.info("=== 接收编辑元素请求，ID：{}，参数：{} ===", id, element);
        try {
            elementService.updateElement(id, element);
            log.info("编辑元素成功，ID：{}", id);
            return Map.of("code", 200, "msg", "编辑元素成功");
        } catch (Exception e) {
            log.error("编辑元素失败！ID：{}", id, e);
            return Map.of("code", 500, "msg", "编辑元素失败：" + e.getMessage());
        }
    }

    // 查询所有元素
    @GetMapping
    public Map<String, Object> listElements() {
        try {
            List<Element> elements = elementService.listAllElements();
            return Map.of("code", 200, "msg", "查询成功", "data", elements);
        } catch (Exception e) {
            return Map.of("code", 500, "msg", "查询失败：" + e.getMessage());
        }
    }

    // 查询单个元素
    @GetMapping("/{id}")
    public Map<String, Object> getElement(@PathVariable Long id) {
        try {
            Element element = elementService.getElementById(id);
            return Map.of("code", 200, "msg", "查询成功", "data", element);
        } catch (Exception e) {
            return Map.of("code", 500, "msg", "查询失败：" + e.getMessage());
        }
    }

    // 删除元素
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
public Map<String, Object> batchImportElements(@RequestBody Map<String, Object> request) {
    log.info("接收批量导入请求，原始参数：{}", request);
    try {
        List<Map<String, Object>> rawList = (List<Map<String, Object>>) request.get("list");
        String url = (String) request.get("url");

        if (rawList == null || rawList.isEmpty()) {
            return Map.of("code", 400, "msg", "导入列表不能为空");
        }

        // 关键：打印每个item的所有字段，看是否有elementName
        for (int i = 0; i < rawList.size(); i++) {
            Map<String, Object> item = rawList.get(i);
            log.info("第{}个元素的所有字段：{}", i+1, item); // 重点看是否有elementName字段
        }

        List<Element> elements = new ArrayList<>();
        for (int i = 0; i < rawList.size(); i++) {
            Map<String, Object> item = rawList.get(i);
            Element element = new Element();
            
            // 强制取值：不管字段名是elementName/name，都兜底
            String elementName = (String) item.get("elementName");
            // 兜底：如果elementName为空，取name字段（兼容前端可能的字段名错误）
            if (elementName == null || elementName.isEmpty()) {
                elementName = (String) item.get("name");
            }
            // 最终兜底：确保非空
            element.setElementName(elementName != null && !elementName.trim().isEmpty() 
                                   ? elementName.trim() 
                                   : "element_" + (i + 1) + "_" + System.currentTimeMillis());
            
            element.setLocatorType((String) item.get("locatorType") != null ? (String) item.get("locatorType") : "XPath");
            element.setLocatorValue((String) item.get("locatorValue") != null ? (String) item.get("locatorValue") : "//default");
            element.setPageUrl(url);
            element.setWidgetType((String) item.get("widgetType") != null ? (String) item.get("widgetType") : "unknown");
            element.setCreateBy("admin");

            elements.add(element);
            log.info("转换后的元素：{}", element);
        }

        elementService.batchImportElements(elements);
        return Map.of("code", 200, "msg", "批量导入成功");
    } catch (Exception e) {
        log.error("批量导入失败", e);
        return Map.of("code", 500, "msg", "批量导入失败：" + e.getMessage());
    }
}
}