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
        >
          <el-table-column type="index" label="#" width="60" />
          <el-table-column prop="name" label="元素名称" />
          <el-table-column prop="locatorType" label="定位器类型" width="140" />
          <el-table-column prop="locatorValue" label="定位器值" />
          <el-table-column prop="page" label="所属页面" width="160" />
          <el-table-column prop="controlType" label="控件类型" width="120" />
          <el-table-column label="操作" width="160">
            <template #default="{ row }">
              <el-button type="text" size="small" @click="openEdit(row)"
                >编辑</el-button
              >
              <el-button type="text" size="small" @click="deleteElement(row.id)"
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

        <el-dialog
          v-model:visible="dialogVisible"
          width="520px"
          title="元素信息"
        >
          <el-form :model="editForm" label-width="100px">
            <el-form-item label="元素名称">
              <el-input v-model="editForm.name" />
            </el-form-item>
            <el-form-item label="定位器类型">
              <el-select v-model="editForm.locatorType">
                <el-option label="XPath" value="XPath" />
                <el-option label="id" value="id" />
                <el-option label="name" value="name" />
              </el-select>
            </el-form-item>
            <el-form-item label="定位器值">
              <el-input v-model="editForm.locatorValue" />
            </el-form-item>
            <el-form-item label="所属页面">
              <el-input v-model="editForm.page" />
            </el-form-item>
            <el-form-item label="控件类型">
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
            <el-button type="primary" @click="saveElement">保存</el-button>
          </template>
        </el-dialog>
      </el-tab-pane>

      <el-tab-pane label="DOM解析提取" name="parse">
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

// Tab 状态
const activeTab = ref("list");

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
    key: "page",
    type: "select",
    options: [
      { label: "首页", value: "home" },
      { label: "登录页", value: "login" },
      { label: "订单页", value: "order" },
    ],
  },
  {
    label: "控件类型",
    key: "controlType",
    type: "select",
    options: [
      { label: "input", value: "input" },
      { label: "button", value: "button" },
      { label: "select", value: "select" },
    ],
  },
];

// 模拟元素数据 15 条
function genElements(n = 15) {
  const pages = ["home", "login", "order"];
  const controls = ["input", "button", "select", "checkbox"];
  const locTypes = ["XPath", "id", "name"];
  const arr = [];
  for (let i = 1; i <= n; i++) {
    arr.push({
      id: i,
      name: `元素-${i}`,
      locatorType: locTypes[i % locTypes.length],
      locatorValue: `//div[@id="el-${i}"]`,
      page: pages[i % pages.length],
      controlType: controls[i % controls.length],
    });
  }
  return arr;
}

const fullList = ref(genElements(15));

// 分页与筛选
const pageParams = reactive({
  pageNum: 1,
  pageSize: 10,
  total: fullList.value.length,
});
const filter = reactive({ elementName: "", page: "", controlType: "" });

const filtered = computed(() => {
  return fullList.value.filter((it) => {
    const matchName = filter.elementName
      ? it.name.toLowerCase().includes(filter.elementName.toLowerCase())
      : true;
    const matchPage = filter.page ? it.page === filter.page : true;
    const matchControl = filter.controlType
      ? it.controlType === filter.controlType
      : true;
    return matchName && matchPage && matchControl;
  });
});
// 监听筛选后的列表，同步更新总条数（解决副作用报错）
watch(
  () => filtered.value,
  (newFilteredList) => {
    pageParams.total = newFilteredList.length;
    pageParams.pageNum = 1; // 筛选后重置到第1页，避免页码超出范围
  },
  { immediate: true } // 初始加载时触发一次，设置初始总条数
);

const displayList = computed(() => {
  const start = (pageParams.pageNum - 1) * pageParams.pageSize;
  return filtered.value.slice(start, start + pageParams.pageSize);
});

function handleSearch(params) {
  filter.elementName = params.elementName || "";
  filter.page = params.page || "";
  filter.controlType = params.controlType || "";
  pageParams.pageNum = 1;
}

function handleReset() {
  filter.elementName = "";
  filter.page = "";
  filter.controlType = "";
  pageParams.pageNum = 1;
}

function onPageChange({ pageNum, pageSize }) {
  pageParams.pageNum = pageNum;
  pageParams.pageSize = pageSize;
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
  editForm.id = null;
  editForm.name = "";
  editForm.locatorType = "XPath";
  editForm.locatorValue = "";
  editForm.page = "";
  editForm.controlType = "input";
  dialogVisible.value = true;
}

function openEdit(row) {
  editForm.id = row.id;
  editForm.name = row.name;
  editForm.locatorType = row.locatorType;
  editForm.locatorValue = row.locatorValue;
  editForm.page = row.page;
  editForm.controlType = row.controlType;
  dialogVisible.value = true;
}

function saveElement() {
  if (!editForm.name) {
    ElMessage.error("元素名称不能为空");
    return;
  }
  if (editForm.id) {
    const idx = fullList.value.findIndex((i) => i.id === editForm.id);
    if (idx > -1) {
      fullList.value[idx] = {
        ...fullList.value[idx],
        ...{
          name: editForm.name,
          locatorType: editForm.locatorType,
          locatorValue: editForm.locatorValue,
          page: editForm.page,
          controlType: editForm.controlType,
        },
      };
    }
    ElMessage.success("更新成功");
  } else {
    const newId = fullList.value.length
      ? Math.max(...fullList.value.map((i) => i.id)) + 1
      : 1;
    fullList.value.unshift({
      id: newId,
      name: editForm.name,
      locatorType: editForm.locatorType,
      locatorValue: editForm.locatorValue,
      page: editForm.page,
      controlType: editForm.controlType,
    });
    ElMessage.success("新增成功");
  }
  dialogVisible.value = false;
}

function deleteElement(id) {
  ElMessageBox.confirm("确定删除该元素吗？", "提示", { type: "warning" })
    .then(() => {
      const idx = fullList.value.findIndex((i) => i.id === id);
      if (idx > -1) fullList.value.splice(idx, 1);
      ElMessage.success("删除成功");
      const maxPage = Math.max(
        1,
        Math.ceil(filtered.value.length / pageParams.pageSize)
      );
      if (pageParams.pageNum > maxPage) pageParams.pageNum = maxPage;
    })
    .catch(() => {});
}

// DOM 解析模拟
const parseForm = reactive({ url: "" });
const parseResult = ref([]);
const selectedElements = ref([]);
const loading = ref(false);

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

function handleBatchImport() {
  if (!selectedElements.value || !selectedElements.value.length) {
    ElMessage.error("请先选择要导入的元素");
    return;
  }
  // 模拟导入：为每个选中元素分配新的 id 并加入 fullList
  const maxId = fullList.value.length
    ? Math.max(...fullList.value.map((i) => i.id))
    : 0;
  const itemsToAdd = selectedElements.value.map((it, idx) => ({
    id: maxId + idx + 1,
    name: it.name,
    locatorType: it.locatorType,
    locatorValue: it.locatorValue,
    page: "",
    controlType: it.controlType,
  }));
  fullList.value = itemsToAdd.concat(fullList.value);
  ElMessage.success("导入成功（模拟）");
  // 切回列表并刷新
  activeTab.value = "list";
  pageParams.pageNum = 1;
  parseResult.value = [];
  selectedElements.value = [];
}

onMounted(() => {
  // 初始加载完成，本地数据已准备
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
