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
          <!-- 改：prop从name→elementName（对齐后端返回字段） -->
          <el-table-column prop="elementName" label="元素名称" />
          <el-table-column prop="locatorType" label="定位器类型" width="140" />
          <el-table-column prop="locatorValue" label="定位器值" />
          <!-- 改：prop从page→pageUrl（对齐后端返回字段） -->
          <el-table-column prop="pageUrl" label="所属页面" width="160" />
          <!-- 改：prop从controlType→widgetType（对齐后端返回字段） -->
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
              <!-- 改：deleteElement → handleDeleteElement -->
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
            <!-- 新增：formRef用于校验 -->
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
            <!-- 新增：submitLoading -->
          </template>
        </el-dialog>
      </el-tab-pane>

      <el-tab-pane label="DOM解析提取" name="parse">
        <!-- 保留原有DOM解析UI，暂时还是模拟数据 -->
        <el-form :model="parseForm" class="parse-form" label-width="80px">
          <el-form-item label="页面URL">
            <el-input v-model="parseForm.url" placeholder="请输入页面URL" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="loading" @click="handleParse"
              >解析</el-button
            >
            <el-button
              style="margin-left: 8px"
              @click="
                () => {
                  parseForm.url = '';
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
// 新增：导入element.js的接口方法
import {
  getElementList,
  addElement,
  updateElement,
  deleteElement,
  batchImportElement,
} from "@/api/element";

// Tab 状态
const activeTab = ref("list");

// 新增：加载状态
const loading = ref(false);
const submitLoading = ref(false);
// 新增：表单ref
const formRef = ref(null);

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
    key: "pageUrl", // 改：key从page→pageUrl（对齐后端）
    type: "select",
    options: [
      { label: "首页", value: "home" },
      { label: "登录页", value: "login" },
      { label: "订单页", value: "order" },
    ],
  },
  {
    label: "控件类型",
    key: "widgetType", // 改：key从controlType→widgetType（对齐后端）
    type: "select",
    options: [
      { label: "input", value: "input" },
      { label: "button", value: "button" },
      { label: "select", value: "select" },
    ],
  },
];

// 改：替换模拟数据为接口返回的真实数据
const fullList = ref([]);

// 分页与筛选
const pageParams = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0, // 初始化为0，接口返回后更新
});
const filter = reactive({ elementName: "", pageUrl: "", widgetType: "" }); // 改：filter字段对齐后端

// 新增：加载元素列表的方法
async function fetchElementList() {
  loading.value = true;
  try {
    const res = await getElementList({
      pageNum: pageParams.pageNum,
      pageSize: pageParams.pageSize,
      ...filter,
    });
    if (res.code === 200) {
      fullList.value = res.data || [];
      pageParams.total = fullList.value.length; // 后端暂时没分页，用总条数
    } else {
      ElMessage.error(res.msg || "查询元素列表失败");
    }
  } catch (err) {
    ElMessage.error("查询元素列表失败：" + err.message);
  } finally {
    loading.value = false;
  }
}

const filtered = computed(() => {
  return fullList.value.filter((it) => {
    const matchName = filter.elementName
      ? it.elementName.toLowerCase().includes(filter.elementName.toLowerCase()) // 改：it.name→it.elementName
      : true;
    const matchPage = filter.pageUrl ? it.pageUrl === filter.pageUrl : true; // 改：it.page→it.pageUrl
    const matchControl = filter.widgetType
      ? it.widgetType === filter.widgetType // 改：it.controlType→it.widgetType
      : true;
    return matchName && matchPage && matchControl;
  });
});

// 监听筛选后的列表，同步更新总条数
watch(
  () => filtered.value,
  (newFilteredList) => {
    pageParams.total = newFilteredList.length;
    pageParams.pageNum = 1;
  },
  { immediate: true }
);

const displayList = computed(() => {
  const start = (pageParams.pageNum - 1) * pageParams.pageSize;
  return filtered.value.slice(start, start + pageParams.pageSize);
});

function handleSearch(params) {
  filter.elementName = params.elementName || "";
  filter.pageUrl = params.pageUrl || ""; // 改：page→pageUrl
  filter.widgetType = params.widgetType || ""; // 改：controlType→widgetType
  pageParams.pageNum = 1;
  fetchElementList(); // 新增：筛选后重新请求接口
}

function handleReset() {
  filter.elementName = "";
  filter.pageUrl = ""; // 改：page→pageUrl
  filter.widgetType = ""; // 改：controlType→widgetType
  pageParams.pageNum = 1;
  fetchElementList(); // 新增：重置后重新请求接口
}

function onPageChange({ pageNum, pageSize }) {
  pageParams.pageNum = pageNum;
  pageParams.pageSize = pageSize;
  fetchElementList(); // 新增：分页切换后重新请求接口
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

function openAdd() {
  console.log("==== openAdd函数被点击触发了 ====");
  // 重置表单数据
  editForm.id = null;
  editForm.name = "";
  editForm.locatorType = "XPath";
  editForm.locatorValue = "";
  editForm.page = "";
  editForm.controlType = "input";
  // 直接显示弹窗（去掉nextTick，先确保弹窗弹出）
  dialogVisible.value = true;
  // 表单重置放到弹窗打开后（用setTimeout替代nextTick，更直观）
  setTimeout(() => {
    formRef.value?.resetFields();
  }, 100);
}

function openEdit(row) {
  console.log("==== openEdit函数被点击触发了，行数据：", row);
  // 回显数据
  editForm.id = row.id;
  editForm.name = row.elementName;
  editForm.locatorType = row.locatorType;
  editForm.locatorValue = row.locatorValue;
  editForm.page = row.pageUrl;
  editForm.controlType = row.widgetType;
  // 直接显示弹窗
  dialogVisible.value = true;
  // 表单重置+重新赋值
  setTimeout(() => {
    formRef.value?.resetFields();
    // 重新赋值（避免resetFields清空）
    editForm.id = row.id;
    editForm.name = row.elementName;
    editForm.locatorType = row.locatorType;
    editForm.locatorValue = row.locatorValue;
    editForm.page = row.pageUrl;
    editForm.controlType = row.widgetType;
  }, 100);
}
async function saveElement() {
  console.log("=== 点击了保存按钮，开始执行saveElement ==="); // 新增日志
  if (!formRef.value) {
    console.error("formRef为空！"); // 新增日志
    return;
  }
  try {
    // 新增：打印表单数据，确认字段是否有值
    console.log("表单提交数据：", JSON.parse(JSON.stringify(editForm)));
    // 表单校验
    await formRef.value.validate();
    console.log("表单校验通过！"); // 新增日志
    submitLoading.value = true;

    if (editForm.id) {
      // 编辑元素
      console.log("执行编辑逻辑，ID：", editForm.id); // 新增日志
      const res = await updateElement(editForm.id, editForm);
      console.log("编辑接口返回：", res); // 新增日志
      if (res.code === 200) {
        ElMessage.success("更新成功");
        fetchElementList(); // 刷新列表
      } else {
        ElMessage.error(res.msg || "更新失败");
      }
    } else {
      // 新增元素
      console.log("执行新增逻辑"); // 新增日志
      const res = await addElement(editForm);
      console.log("新增接口返回：", res); // 新增日志
      if (res.code === 200) {
        ElMessage.success("新增成功");
        fetchElementList(); // 刷新列表
      } else {
        ElMessage.error(res.msg || "新增失败");
      }
    }
    dialogVisible.value = false;
  } catch (err) {
    console.error("保存失败详细原因：", err); // 新增日志（看是校验失败还是接口失败）
    ElMessage.error("提交失败：" + err.message);
  } finally {
    submitLoading.value = false;
  }
}

// 改：deleteElement调用真实接口
// 2. 把自定义的deleteElement函数名改成handleDeleteElement（或其他不重复的名字）
async function handleDeleteElement(id) {
  // 改：deleteElement → handleDeleteElement
  try {
    await ElMessageBox.confirm("确定删除该元素吗？", "提示", {
      type: "warning",
    });
    const res = await deleteElement(id); // 调用导入的deleteElement接口（名字不变）
    if (res.code === 200) {
      ElMessage.success("删除成功");
      fetchElementList(); // 刷新列表
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
// DOM 解析（保留模拟，后续对接）
const parseForm = reactive({ url: "" });
const parseResult = ref([]);
const selectedElements = ref([]);

function genParseResults() {
  const controls = ["input", "button", "select", "checkbox"];
  const arr = [];
  for (let i = 1; i <= 8; i++) {
    arr.push({
      id: i,
      name: `解析元素-${i}`,
      locatorType: "XPath",
      locatorValue: `//div[@class="parsed-${i}"]`,
      controlType: controls[i % controls.length],
    });
  }
  return arr;
}

function handleParse() {
  if (!parseForm.url || !/^https?:\/\//.test(parseForm.url)) {
    ElMessage.error("请输入合法的 URL（以 http:// 或 https:// 开头）");
    return;
  }
  loading.value = true;
  setTimeout(() => {
    parseResult.value = genParseResults();
    loading.value = false;
    ElMessage.success("解析完成（模拟数据）");
  }, 800);
}

function onSelectionChange(selection) {
  selectedElements.value = selection;
}

// 改：批量导入调用真实接口（可选，暂时还是模拟）
async function handleBatchImport() {
  if (!selectedElements.value || !selectedElements.value.length) {
    ElMessage.error("请先选择要导入的元素");
    return;
  }
  try {
    // 调用批量导入接口（暂时注释，先用模拟）
    const res = await batchImportElement({ list: selectedElements.value });
    if (res.code === 200) {
      ElMessage.success("导入成功");
      fetchElementList();
    } else {
      ElMessage.error(res.msg || "导入失败");
    }

    activeTab.value = "list";
    pageParams.pageNum = 1;
    parseResult.value = [];
    selectedElements.value = [];
    fetchElementList(); // 刷新列表
  } catch (err) {
    ElMessage.error("导入失败：" + err.message);
  }
}

// 新增：页面加载时请求真实接口
onMounted(() => {
  fetchElementList();
});
</script>

<style scoped lang="less">
// 保留原有样式，无修改
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
