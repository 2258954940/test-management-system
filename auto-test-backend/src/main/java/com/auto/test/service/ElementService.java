// auto-test-backend/src/main/java/com/auto/test/service/ElementService.java
package com.auto.test.service;

import com.auto.test.entity.Element;
import com.auto.test.mapper.ElementMapper;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired; 
import java.util.List;

@Service
public class ElementService {
     @Autowired
    private ElementMapper elementMapper;

// 新增元素：补全pageUrl默认值
public Element addElement(Element element) {
    if (element.getCreateBy() == null) {
        element.setCreateBy("admin");
    }
    if (element.getWidgetType() == null) {
        element.setWidgetType("");
    }
    // 新增：pageUrl为空时设默认值（数据库page_url字段非空）
    if (element.getPageUrl() == null || element.getPageUrl().trim().isEmpty()) {
        element.setPageUrl("");
    }
    elementMapper.insertElement(element);
    return element;
}

  // 编辑元素：补全pageUrl的更新逻辑
public void updateElement(Long id, Element element) {
    Element originalElement = elementMapper.findById(id);
    if (originalElement == null) {
        throw new IllegalArgumentException("元素不存在");
    }
    // 只更新传了值的字段
    if (element.getElementName() != null) {
        originalElement.setElementName(element.getElementName());
    }
    if (element.getPageUrl() != null) {
        originalElement.setPageUrl(element.getPageUrl().trim().isEmpty() ? "" : element.getPageUrl());
    }
    if (element.getLocatorType() != null) {
        originalElement.setLocatorType(element.getLocatorType());
    }
    if (element.getLocatorValue() != null) {
        originalElement.setLocatorValue(element.getLocatorValue());
    }
    if (element.getWidgetType() != null) {
        originalElement.setWidgetType(element.getWidgetType());
    }
    if (element.getCreateBy() != null) {
        originalElement.setCreateBy(element.getCreateBy());
    }
    elementMapper.updateElement(originalElement);
}

    // 批量导入元素
public void batchImportElements(List<Element> elements) {
  // 给空字段加默认值（避免数据库报错）
  for (Element element : elements) {
    if (element.getCreateBy() == null) {
      element.setCreateBy("admin");
    }
    if (element.getWidgetType() == null) {
      element.setWidgetType("");
    }
    if (element.getPageUrl() == null) {
      element.setPageUrl("");
    }
  }
  elementMapper.batchInsertElements(elements);
}

    // 查询所有元素
    public List<Element> listAllElements() {
        return elementMapper.findAll();
    }

    // 查询单个元素（编辑回显）
    public Element getElementById(Long id) {
        return elementMapper.findById(id);
    }

    // 删除元素（已有，不用改）
    public void deleteElement(Long id) {
        elementMapper.deleteElement(id);
    }
}