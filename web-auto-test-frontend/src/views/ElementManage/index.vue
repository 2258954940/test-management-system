<template>
  <div class="element-manage-root">
    <el-tabs v-model:active-name="activeTab" class="tabs-root">
      <el-tab-pane label="元素列表" name="list">
        <div class="top-area">
          <common-query-form
            :fields="queryFields"
            @onSearch="handleSearch"
            @onReset="handleReset"
          />
          <div class="action-area">
            <el-button type="primary" @click="openAdd">新增元素</el-button>
          </div>
        </div>

        <el-table
          :data="displayList"
          stripe
          style="width: 100%"
          class="element-table"
          :loading="loading"
        >
          <el-table-column type="index" label="#" width="60" />
          <el-table-column prop="elementName" label="元素名称" />
          <el-table-column prop="locatorType" label="定位器类型" width="140" />
          <el-table-column prop="locatorValue" label="定位器值" />
          <el-table-column prop="pageUrl" label="所属页面" width="160" />
          <el-table-column prop="widgetType" label="控件类型" width="120" />
          <el-table-column label="操作" width="160">
            <template #default="{ row }">
              <el-button type="text" size="small" @click="openEdit(row)"
                >编辑</el-button
              >
              <el-button
                type="text"
                size="small"
                @click="handleDeleteElement(row.id)"
                >删除</el-button
              >
            </template>
          </el-table-column>
        </el-table>

        <common-pagination
          :total="pageParams.total"
          :pageNum="pageParams.pageNum"
          :pageSize="pageParams.pageSize"
          @onPageChange="onPageChange"
        />

        <el-dialog v-model="dialogVisible" width="520px" title="元素信息">
          <el-form :model="editForm" label-width="100px" ref="formRef">
            <el-form-item
              label="元素名称"
              prop="name"
              :rules="[{ required: true, message: '元素名称不能为空' }]"
            >
              <el-input v-model="editForm.name" />
            </el-form-item>
            <el-form-item
              label="定位器类型"
              prop="locatorType"
              :rules="[{ required: true, message: '定位器类型不能为空' }]"
            >
              <el-select v-model="editForm.locatorType">
                <el-option label="XPath" value="XPath" />
                <el-option label="id" value="id" />
                <el-option label="name" value="name" />
              </el-select>
            </el-form-item>
            <el-form-item
              label="定位器值"
              prop="locatorValue"
              :rules="[{ required: true, message: '定位器值不能为空' }]"
            >
              <el-input v-model="editForm.locatorValue" />
            </el-form-item>
            <el-form-item label="所属页面" prop="page">
              <el-input v-model="editForm.page" />
            </el-form-item>
            <el-form-item label="控件类型" prop="controlType">
              <el-select v-model="editForm.controlType">
                <el-option label="input" value="input" />
                <el-option label="button" value="button" />
                <el-option label="select" value="select" />
                <el-option label="checkbox" value="checkbox" />
              </el-select>
            </el-form-item>
          </el-form>
          <template #footer>
            <el-button @click="dialogVisible = false">取消</el-button>
            <el-button
              type="primary"
              @click="saveElement"
              :loading="submitLoading"
              >保存</el-button
            >
          </template>
        </el-dialog>
      </el-tab-pane>

      <el-tab-pane label="DOM解析提取" name="parse">
        <el-form :model="parseForm" class="parse-form" label-width="80px">
          <el-form-item label="页面URL">
            <el-input v-model="parseForm.url" placeholder="请输入页面URL" />
          </el-form-item>
          <el-form-item>
            <el-button
              type="primary"
              :loading="parseLoading"
              @click="handleParse"
              >解析</el-button
            >
            <el-button
              style="margin-left: 8px"
              @click="
                () => {
                  parseForm.url = '';
                  parseResult.value = [];
                  selectedElements.value = [];
                }
              "
              >重置</el-button
            >
          </el-form-item>
        </el-form>

        <el-table
          :data="parseResult"
          style="width: 100%"
          class="parse-table"
          row-key="id"
          @selection-change="onSelectionChange"
          show-header
        >
          <el-table-column type="selection" width="60" />
          <el-table-column prop="name" label="元素名称" />
          <el-table-column prop="locatorType" label="定位器类型" width="120" />
          <el-table-column prop="locatorValue" label="定位器值" />
          <el-table-column prop="controlType" label="控件类型" width="120" />
        </el-table>

        <div style="margin-top: 16px">
          <el-button type="primary" @click="handleBatchImport"
            >批量导入</el-button
          >
        </div>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import CommonQueryForm from "@/components/CommonQueryForm.vue";
import CommonPagination from "@/components/CommonPagination.vue";
import {
  getElementList,
  addElement,
  updateElement,
  deleteElement,
  batchImportElement,
  parseUrlElements,
} from "@/api/element";

// Tab 状态
const activeTab = ref("list");

// 加载状态
const loading = ref(false); // 列表加载
const parseLoading = ref(false); // 解析按钮加载
const submitLoading = ref(false); // 提交按钮加载
const formRef = ref(null); // 表单ref

// 查询字段配置
const queryFields = [
  {
    label: "元素名称",
    key: "elementName",
    type: "input",
    placeholder: "请输入元素名称",
  },
  {
    label: "所属页面",
    key: "pageUrl",
    type: "select",
    options: [
      { label: "首页", value: "home" },
      { label: "登录页", value: "login" },
      { label: "订单页", value: "order" },
    ],
  },
  {
    label: "控件类型",
    key: "widgetType",
    type: "select",
    options: [
      { label: "input", value: "input" },
      { label: "button", value: "button" },
      { label: "select", value: "select" },
    ],
  },
];

// 数据列表
const fullList = ref([]);
// 分页参数
const pageParams = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0,
});
// 筛选条件
const filter = reactive({ elementName: "", pageUrl: "", widgetType: "" });

// 加载元素列表
async function fetchElementList() {
  loading.value = true;
  try {
    const res = await getElementList();
    if (res.code === 200 || res.code === undefined) {
      fullList.value = res.data || res || [];
      pageParams.total = fullList.value.length;
    } else {
      ElMessage.error(res.msg || "查询元素列表失败");
      fullList.value = [];
      pageParams.total = 0;
    }
  } catch (err) {
    const errMsg =
      err?.response?.data?.msg ||
      err?.response?.statusText ||
      "查询元素列表失败";
    ElMessage.error(`查询元素列表失败：${errMsg}`);
    fullList.value = [];
    pageParams.total = 0;
  } finally {
    loading.value = false;
  }
}

// 筛选列表
const filtered = computed(() => {
  return fullList.value.filter((it) => {
    const matchName = filter.elementName
      ? it.elementName.toLowerCase().includes(filter.elementName.toLowerCase())
      : true;
    const matchPage = filter.pageUrl ? it.pageUrl === filter.pageUrl : true;
    const matchControl = filter.widgetType
      ? it.widgetType === filter.widgetType
      : true;
    return matchName && matchPage && matchControl;
  });
});

// 监听筛选结果更新分页
watch(
  () => filtered.value,
  (newFilteredList) => {
    pageParams.total = newFilteredList.length;
    pageParams.pageNum = 1;
  },
  { immediate: true }
);

// 分页展示列表
const displayList = computed(() => {
  const start = (pageParams.pageNum - 1) * pageParams.pageSize;
  return filtered.value.slice(start, start + pageParams.pageSize);
});

// 搜索
function handleSearch(params) {
  filter.elementName = params.elementName || "";
  filter.pageUrl = params.pageUrl || "";
  filter.widgetType = params.widgetType || "";
  pageParams.pageNum = 1;
  fetchElementList();
}

// 重置
function handleReset() {
  filter.elementName = "";
  filter.pageUrl = "";
  filter.widgetType = "";
  pageParams.pageNum = 1;
  fetchElementList();
}

// 分页切换
function onPageChange({ pageNum, pageSize }) {
  pageParams.pageNum = pageNum;
  pageParams.pageSize = pageSize;
  fetchElementList();
}

// 新增/编辑弹窗
const dialogVisible = ref(false);
const editForm = reactive({
  id: null,
  name: "",
  locatorType: "XPath",
  locatorValue: "",
  page: "",
  controlType: "input",
});

// 打开新增弹窗
function openAdd() {
  console.log("==== openAdd函数被点击触发了 ====");
  editForm.id = null;
  editForm.name = "";
  editForm.locatorType = "XPath";
  editForm.locatorValue = "";
  editForm.page = "";
  editForm.controlType = "input";
  dialogVisible.value = true;
  setTimeout(() => {
    formRef.value?.resetFields();
  }, 100);
}

// 打开编辑弹窗
function openEdit(row) {
  console.log("==== openEdit函数被点击触发了，行数据：", row);
  editForm.id = row.id;
  editForm.name = row.elementName;
  editForm.locatorType = row.locatorType;
  editForm.locatorValue = row.locatorValue;
  editForm.page = row.pageUrl;
  editForm.controlType = row.widgetType;
  dialogVisible.value = true;
  setTimeout(() => {
    formRef.value?.resetFields();
    editForm.id = row.id;
    editForm.name = row.elementName;
    editForm.locatorType = row.locatorType;
    editForm.locatorValue = row.locatorValue;
    editForm.page = row.pageUrl;
    editForm.controlType = row.widgetType;
  }, 100);
}

// 保存元素
async function saveElement() {
  console.log("=== 点击了保存按钮，开始执行saveElement ===");
  if (!formRef.value) {
    console.error("formRef为空！");
    return;
  }
  try {
    console.log("表单提交数据：", JSON.parse(JSON.stringify(editForm)));
    await formRef.value.validate();
    console.log("表单校验通过！");
    submitLoading.value = true;

    if (editForm.id) {
      console.log("执行编辑逻辑，ID：", editForm.id);
      const res = await updateElement(editForm.id, editForm);
      console.log("编辑接口返回：", res);
      if (res.code === 200) {
        ElMessage.success("更新成功");
        fetchElementList();
      } else {
        ElMessage.error(res.msg || "更新失败");
      }
    } else {
      console.log("执行新增逻辑");
      const res = await addElement(editForm);
      console.log("新增接口返回：", res);
      if (res.code === 200) {
        ElMessage.success("新增成功");
        fetchElementList();
      } else {
        ElMessage.error(res.msg || "新增失败");
      }
    }
    dialogVisible.value = false;
  } catch (err) {
    console.error("保存失败详细原因：", err);
    ElMessage.error("提交失败：" + err.message);
  } finally {
    submitLoading.value = false;
  }
}

// 删除元素
async function handleDeleteElement(id) {
  try {
    await ElMessageBox.confirm("确定删除该元素吗？", "提示", {
      type: "warning",
    });
    const res = await deleteElement(id);
    if (res.code === 200) {
      ElMessage.success("删除成功");
      fetchElementList();
      const maxPage = Math.max(
        1,
        Math.ceil(filtered.value.length / pageParams.pageSize)
      );
      if (pageParams.pageNum > maxPage) pageParams.pageNum = maxPage;
    } else {
      ElMessage.error(res.msg || "删除失败");
    }
  } catch (err) {
    ElMessage.info("取消删除");
  }
}

// DOM解析相关
const parseForm = reactive({ url: "" });
const parseResult = ref([]);
const selectedElements = ref([]);

// 表格选中事件
function onSelectionChange(val) {
  selectedElements.value = val;
  console.log("选中的元素列表：", selectedElements.value);
}

// 解析URL
async function handleParse() {
  if (!parseForm.url || !/^https?:\/\//.test(parseForm.url)) {
    ElMessage.error("请输入合法的 URL（以 http:// 或 https:// 开头）");
    return;
  }
  parseLoading.value = true;
  try {
    const res = await parseUrlElements({ url: parseForm.url });
    if (res.code === 200) {
      parseResult.value = res.data.map((item, index) => ({
        id: index + 1,
        name: item.elementName,
        locatorType: item.locatorType,
        locatorValue: item.locatorValue,
        controlType: item.widgetType,
      }));
      ElMessage.success("解析完成");
    } else {
      ElMessage.error(res.msg || "解析失败");
    }
  } catch (err) {
    ElMessage.error(`解析异常：${err.response?.data?.msg || err.message}`);
  } finally {
    parseLoading.value = false;
  }
}

// 批量导入
async function handleBatchImport() {
  if (!selectedElements.value || selectedElements.value.length === 0) {
    ElMessage.error("请先选择要导入的元素");
    return;
  }

  try {
    const importData = {
      url: parseForm.url,
      // 重点：强制确保elementName字段被添加，且值正确
      list: selectedElements.value.map((item) => {
        const obj = {
          elementName: item.name, // 这行是核心！必须保留，且字段名是elementName
          locatorType: item.locatorType,
          locatorValue: item.locatorValue,
          pageUrl: parseForm.url,
          widgetType: item.controlType,
          createBy: "admin",
        };
        // 打印每个构造的对象，确认有elementName
        console.log("构造的单个参数：", obj);
        return obj;
      }),
    };
    // 打印最终请求参数，确认list里有elementName
    console.log("最终请求参数：", importData);

    const res = await batchImportElement(importData);
    if (res && res.code === 200) {
      ElMessage.success("导入成功");
      fetchElementList();
    } else {
      ElMessage.error(res?.msg || "导入失败：接口返回异常");
    }

    // 清空状态
    activeTab.value = "list";
    pageParams.pageNum = 1;
    parseResult.value = [];
    selectedElements.value = [];
    parseForm.url = "";
  } catch (err) {
    console.error("批量导入异常：", err);
    const errorMsg =
      err?.response?.data?.msg || err?.message || "导入失败：网络异常";
    ElMessage.error(errorMsg);
  }
}

// 页面加载时请求列表
onMounted(() => {
  fetchElementList();
});
</script>

<style scoped lang="less">
.element-manage-root {
  padding: 20px;
}

.tabs-root {
  margin-bottom: 16px;
}

.top-area {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.action-area {
  margin-left: 12px;
}

.element-table,
.parse-table {
  background: #fff;
  border-radius: 8px;
}

.parse-form {
  margin-bottom: 16px;
}

@media (max-width: 768px) {
  .top-area {
    flex-direction: column;
    align-items: stretch;
    gap: 12px;
  }
}
</style>
