package com.auto.test.service;

import com.auto.test.dto.ElementParseRequest;
import com.auto.test.entity.Element;
import com.auto.test.mapper.ElementMapper;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class ElementService {
    private static final Logger log = LoggerFactory.getLogger(ElementService.class);

    @Autowired
    private ElementMapper elementMapper;

    /**
     * 新增元素
     */
    @Transactional
    public Element addElement(Element element) {
        if (element.getCreateBy() == null || element.getCreateBy().isEmpty()) {
            element.setCreateBy("admin");
        }
        elementMapper.insertElement(element);
        return element;
    }

    /**
     * 编辑元素
     */
    @Transactional
    public void updateElement(Long id, Element element) {
        Element existElement = elementMapper.findById(id); // 改：findById
        if (existElement == null) {
            throw new IllegalArgumentException("元素不存在，ID=" + id);
        }
        element.setId(id);
        elementMapper.updateElement(element);
    }

    /**
     * 查询所有元素
     */
    public List<Element> listAllElements() {
        return elementMapper.findAll(); // 改：findAll
    }

    /**
     * 根据ID查询单个元素
     */
    public Element getElementById(Long id) {
        Element element = elementMapper.findById(id); // 改：findById
        if (element == null) {
            throw new IllegalArgumentException("元素不存在，ID=" + id);
        }
        return element;
    }

    /**
     * 删除元素
     */
    @Transactional
    public void deleteElement(Long id) {
        Element existElement = elementMapper.findById(id); // 改：findById
        if (existElement == null) {
            throw new IllegalArgumentException("元素不存在，ID=" + id);
        }
        elementMapper.deleteElement(id);
    }

    /**
     * 批量导入元素（改用Mapper批量插入）
     */
    @Transactional
    public void batchImportElements(List<Element> elements) {
        if (elements == null || elements.isEmpty()) {
            throw new IllegalArgumentException("导入的元素列表不能为空");
        }
        // 改：调用Mapper的批量插入方法
        elementMapper.batchInsertElements(elements); 
        log.info("批量导入元素完成，共导入{}个元素", elements.size());
    }

    /**
     * 解析URL提取DOM元素
     */
    public List<Element> parseUrlElements(ElementParseRequest request) {
    String url = request.getUrl();
    List<Element> elementList = new ArrayList<>();

    // 校验URL
    if (url == null || url.isEmpty() || (!url.startsWith("http://") && !url.startsWith("https://"))) {
        throw new IllegalArgumentException("请输入合法的URL（以http/https开头）");
    }

    try {
        // 1. Jsoup模拟浏览器请求爬取页面
        Document doc = Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36")
                .timeout(10000)
                .ignoreContentType(true)
                .followRedirects(true)
                .get();

        // 2. 提取核心可操作元素
        Elements coreElements = doc.select("input, button, select, textarea, a");
        log.info("爬取页面成功，共提取到{}个可操作元素", coreElements.size());

        // 3. 转换为自定义Element实体（用全限定类名避免冲突）
        for (int i = 0; i < coreElements.size(); i++) {
            // 关键：用全限定类名声明Jsoup的Element
            org.jsoup.nodes.Element jsoupEl = coreElements.get(i);
            Element element = new Element(); // 自定义的Element（实体类）
            String tagName = jsoupEl.tagName().toLowerCase();

            // 填充自定义Element的字段
            element.setElementName(tagName + "_" + (i + 1));
            String id = jsoupEl.id();
            String name = jsoupEl.attr("name");
            if (!id.isEmpty()) {
                element.setLocatorType("id");
                element.setLocatorValue(id);
            } else if (!name.isEmpty()) {
                element.setLocatorType("name");
                element.setLocatorValue(name);
            } else {
                element.setLocatorType("XPath");
                element.setLocatorValue("//" + tagName + "[@class='" + jsoupEl.attr("class") + "']");
            }
            element.setPageUrl(url);
            element.setWidgetType(tagName);
            element.setCreateBy("admin");

            elementList.add(element);
        }

        return elementList;
    } catch (IOException e) {
        log.error("解析URL失败！URL={}", url, e);
        throw new RuntimeException("解析URL失败：" + e.getMessage());
    } catch (Exception e) {
        log.error("提取DOM元素异常！URL={}", url, e);
        throw new RuntimeException("提取元素异常：" + e.getMessage());
    }
 }
}