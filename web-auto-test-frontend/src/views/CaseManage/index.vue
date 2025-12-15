<template>
  <div class="case-manage-root">
    <div class="top-area">
      <common-query-form
        :fields="queryFields"
        @onSearch="handleSearch"
        @onReset="handleReset"
      />
      <div class="action-area">
        <el-button type="primary" @click="openAdd">新增用例</el-button>
      </div>
    </div>

    <!-- 新增：表格包裹容器（用于修复ResizeObserver报错） -->
    <div ref="tableWrap" class="table-wrap">
      <el-table
        :data="displayList"
        stripe
        style="width: 100%"
        class="case-table"
        row-key="id"
        :loading="loading"
      >
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="name" label="用例名称" min-width="160" />
        <el-table-column
          prop="url"
          label="测试URL"
          min-width="200"
          show-overflow-tooltip
        />

        <el-table-column prop="creator" label="创建人" width="120" />

        <el-table-column prop="createTime" label="创建时间" width="180">
          <template #default="{ row }">{{
            formatDate(row.createTime)
          }}</template>
        </el-table-column>

        <el-table-column prop="updateTime" label="更新时间" width="180">
          <template #default="{ row }">{{
            formatDate(row.updateTime)
          }}</template>
        </el-table-column>

        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <el-button type="text" size="small" @click="openEdit(row)"
              >编辑</el-button
            >
            <el-button type="text" size="small" @click="handleDelete(row.id)"
              >删除</el-button
            >
            <el-button type="text" size="small" @click="handleRunCase(row)"
              >执行</el-button
            >
          </template>
        </el-table-column>
      </el-table>
    </div>

    <common-pagination
      :total="pageParams.total"
      :pageNum="pageParams.pageNum"
      :pageSize="pageParams.pageSize"
      :disabled="loading"
      @onPageChange="handlePageChange"
    />

    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="560px"
      style="z-index: 9999"
    >
      <el-form
        ref="formRef"
        :model="formModel"
        :rules="formRules"
        label-width="100px"
        class="dialog-form"
      >
        <el-form-item label="用例名称" prop="name">
          <el-input v-model="formModel.name" placeholder="请输入用例名称" />
        </el-form-item>
        <el-form-item label="测试URL" prop="url">
          <el-input v-model="formModel.url" placeholder="请输入测试URL" />
        </el-form-item>
        <el-form-item label="步骤" prop="description">
          <el-input
            v-model="formModel.description"
            type="textarea"
            :rows="4"
            placeholder="请输入执行步骤（支持多行）"
          />
        </el-form-item>
        <!-- 新增：输入数据输入框 -->
        <el-form-item label="输入数据" prop="inputData">
          <el-input
            v-model="formModel.inputData"
            placeholder="请输入要在输入框中填写的内容（如：自动化测试）"
          />
        </el-form-item>
        <!-- 新增：预期结果输入框 -->
        <el-form-item label="预期结果" prop="expectedResult">
          <el-input
            v-model="formModel.expectedResult"
            placeholder="请输入预期结果（如：显示“自动化测试”搜索结果）"
          />
        </el-form-item>
        <el-form-item label="创建人" prop="creator">
          <el-input
            v-model="formModel.creator"
            placeholder="请输入创建人"
            :disabled="isEdit"
          />
        </el-form-item>
        <!-- 新增：关联元素选择框 -->
        <el-form-item label="关联元素">
          <el-select
            v-model="formModel.elementIds"
            multiple
            placeholder="请选择要关联的元素（多选）"
            style="width: 100%"
          >
            <el-option
              v-for="item in elementList"
              :key="item.id"
              :label="item.elementName"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button
          type="primary"
          :loading="submitLoading"
          @click="handleSubmit"
        >
          确定
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch, onUnmounted } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import CommonQueryForm from "@/components/CommonQueryForm.vue";
import CommonPagination from "@/components/CommonPagination.vue";
import {
  addCase,
  deleteCase,
  getCaseList,
  updateCase,
  runCase,
} from "@/api/case";
import { useUserStore } from "@/store";

// 新增：导入元素列表接口
import { getElementList } from "@/api/element";

// 新增：修复ResizeObserver报错相关 - 表格包裹容器ref + 监听实例
const tableWrap = ref(null);
let resizeObserver = null;

const queryFields = [
  {
    label: "用例名称",
    key: "name",
    type: "input",
    placeholder: "按名称搜索",
  },
  {
    label: "创建人",
    key: "creator",
    type: "input",
    placeholder: "按创建人搜索",
  },
];

const caseList = ref([]);
const loading = ref(false);
const submitLoading = ref(false);
// 新增：元素列表
const elementList = ref([]);

const filterParams = reactive({
  name: "",
  creator: "",
});

const pageParams = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0,
});

const filteredList = computed(() => {
  const nameKw = (filterParams.name || "").trim().toLowerCase();
  const creatorKw = (filterParams.creator || "").trim().toLowerCase();
  return caseList.value.filter((item) => {
    const nameMatch = nameKw
      ? String(item.name || "")
          .toLowerCase()
          .includes(nameKw)
      : true;
    const creatorMatch = creatorKw
      ? String(item.creator || "")
          .toLowerCase()
          .includes(creatorKw)
      : true;
    return nameMatch && creatorMatch;
  });
});

watch(
  () => filteredList.value.length,
  (len) => {
    pageParams.total = len;
    const maxPage = Math.max(1, Math.ceil((len || 1) / pageParams.pageSize));
    if (pageParams.pageNum > maxPage) {
      pageParams.pageNum = maxPage;
    }
  },
  { immediate: true }
);

const displayList = computed(() => {
  const start = (pageParams.pageNum - 1) * pageParams.pageSize;
  return filteredList.value.slice(start, start + pageParams.pageSize);
});

function getDefaultCreator() {
  const store = useUserStore();
  if (store?.username) return store.username;
  return localStorage.getItem("username") || "";
}

// index.vue 的 fetchCaseList 函数
async function fetchCaseList() {
  loading.value = true;
  try {
    const res = await getCaseList();
    const list = Array.isArray(res?.list)
      ? res.list
      : Array.isArray(res)
      ? res
      : Array.isArray(res?.data)
      ? res.data
      : [];
    // 注释掉这行兜底逻辑（改后加的，没改之前没有）
    // caseList.value = list.map((item) => ({...}));
    // 改回原来的逻辑：
    caseList.value = list;
    pageParams.total = filteredList.value.length;
  } catch (err) {
    caseList.value = [];
    pageParams.total = 0;
    ElMessage.error("获取用例列表失败");
  } finally {
    loading.value = false;
  }
}

// 新增：加载元素列表
async function fetchElementList() {
  try {
    const res = await getElementList(); // 不传参，正确
    // 兼容后端返回格式，赋值给ref的value
    elementList.value = res.data || res || [];
  } catch (err) {
    const errMsg =
      err?.response?.data?.msg ||
      err?.response?.statusText ||
      "加载元素列表失败";
    ElMessage.error(`加载元素列表失败：${errMsg}`);
    elementList.value = []; // 空列表兜底
  }
}

function handleSearch(params) {
  filterParams.name = params.name || "";
  filterParams.creator = params.creator || "";
  pageParams.pageNum = 1;
  fetchCaseList();
}

function handleReset() {
  filterParams.name = "";
  filterParams.creator = "";
  pageParams.pageNum = 1;
  fetchCaseList();
}

function handlePageChange({ pageNum, pageSize }) {
  pageParams.pageNum = pageNum;
  pageParams.pageSize = pageSize;
}

const dialogVisible = ref(false);
const isEdit = ref(false);
const formRef = ref();
const formModel = reactive({
  id: null,
  name: "",
  url: "",
  description: "",
  creator: "",
  // 新增：关联元素ID数组（多选）
  elementIds: [],
  inputData: "", // 新增：输入数据
  expectedResult: "", // 新增：预期结果
});

const formRules = {
  name: [{ required: true, message: "请输入用例名称", trigger: "blur" }],
  url: [{ required: true, message: "请输入测试URL", trigger: "blur" }],
  description: [{ required: true, message: "请输入用例步骤", trigger: "blur" }],
  creator: [{ required: true, message: "请输入创建人", trigger: "blur" }],
  inputData: [
    { required: true, message: "请输入要填写的内容", trigger: "blur" },
  ], // 新增
  expectedResult: [
    { required: true, message: "请输入预期结果", trigger: "blur" },
  ], // 新增
};

const dialogTitle = computed(() => (isEdit.value ? "编辑用例" : "新增用例"));

function openAdd() {
  // ① 重置表单（和系统管理一致）
  isEdit.value = false;
  formModel.id = null;
  formModel.name = "";
  formModel.url = "";
  formModel.description = "";
  formModel.creator = "admin"; // 先固定值，避开getDefaultCreator的依赖
  formModel.inputData = ""; // 新增：重置输入数据
  formModel.expectedResult = ""; // 新增：重置预期结果
  // 新增：重置关联元素
  formModel.elementIds = [];

  // ② 直接显示弹窗（和系统管理一致）
  dialogVisible.value = true;
  // ③ 延迟清验证（等弹窗渲染完，和系统管理一致）
  setTimeout(() => {
    if (formRef.value) formRef.value.clearValidate();
  }, 100);
  console.log("dialogVisible设置为：", dialogVisible.value);
}

function openEdit(row) {
  console.log("点击了编辑按钮，执行openEdit，行数据：", row);
  // ① 重置编辑状态+赋值
  isEdit.value = true;
  formModel.id = row.id;
  formModel.name = row.name || "";
  formModel.url = row.url || "";
  formModel.description = row.description || "";
  formModel.creator = row.creator || "admin"; // 固定值，避开依赖
  formModel.inputData = row.inputData || ""; // 新增：回显输入数据
  formModel.expectedResult = row.expectedResult || ""; // 新增：回显预期结果
  // 新增：回显关联元素（字符串转数组）
  formModel.elementIds = row.elementIds
    ? row.elementIds.split(",").map((id) => Number(id))
    : [];

  // ② 直接显示弹窗（和系统管理/新增按钮一致）
  dialogVisible.value = true;

  // ③ 延迟清验证（等弹窗渲染完）
  setTimeout(() => {
    if (formRef.value) formRef.value.clearValidate();
  }, 100);

  console.log("dialogVisible设置为：", dialogVisible.value);
}

// 新增：执行用例的方法（适配后端参数格式）
// 替换原有handleRunCase函数
async function handleRunCase(row) {
  // 基础校验（毕设必要）
  if (!row.id) {
    ElMessage.error("用例ID为空，无法执行");
    return;
  }
  try {
    ElMessage.info(`正在执行用例：${row.name}`);
    const res = await runCase({ caseId: row.id });

    // 打印验证（毕设调试用，可保留/删除）
    console.log("后端返回的执行结果：", res);

    // 核心：直接判断后端的status字段（PASS=成功，其他=失败）
    const execStatus = res.status || "UNKNOWN";
    if (execStatus === "PASS") {
      ElMessage.success(`用例执行成功：${row.name}，状态：${execStatus}`);
    } else {
      ElMessage.error(`用例执行失败：${row.name}，状态：${execStatus}`);
    }
  } catch (err) {
    // 异常处理（毕设严谨性）
    const errMsg = err?.message || "执行异常（网络/接口错误）";
    ElMessage.error(`用例执行出错：${errMsg}`);
  }
}

async function handleSubmit() {
  if (!formRef.value) return;
  try {
    await formRef.value.validate();
  } catch (err) {
    // 新增：验证失败时明确提示用户
    ElMessage.error("请填写所有必填字段（用例名称/URL/步骤/创建人）");
    return;
  }

  submitLoading.value = true;
  try {
    // 构造提交数据（把elementIds数组转成逗号分隔的字符串）
    const submitData = {
      ...formModel,
      elementIds: formModel.elementIds.join(",") || "",
    };

    if (isEdit.value) {
      await updateCase(submitData);
      ElMessage.success("编辑用例成功");
    } else {
      await addCase({
        ...submitData,
        creator: submitData.creator || getDefaultCreator(),
      });
      ElMessage.success("新增用例成功");
    }
    dialogVisible.value = false;
    fetchCaseList();
  } catch (err) {
    // 错误提示在响应拦截器里处理
  } finally {
    submitLoading.value = false;
  }
}

function handleDelete(id) {
  if (!id) {
    ElMessage.error("缺少用例ID");
    return;
  }
  ElMessageBox.confirm("确认删除该用例吗？", "提示", { type: "warning" })
    .then(async () => {
      await deleteCase(id);
      ElMessage.success("删除用例成功");
      fetchCaseList();
    })
    .catch(() => {});
}

function formatDate(val) {
  if (!val) return "";
  val = typeof val === "string" ? val.replace("T", " ") : val;
  const d = new Date(val);
  if (Number.isNaN(d.getTime())) return val;
  const y = d.getFullYear();
  const m = String(d.getMonth() + 1).padStart(2, "0");
  const day = String(d.getDate()).padStart(2, "0");
  const hh = String(d.getHours()).padStart(2, "0");
  const mm = String(d.getMinutes()).padStart(2, "0");
  const ss = String(d.getSeconds()).padStart(2, "0");
  return `${y}-${m}-${day} ${hh}:${mm}:${ss}`;
}

// 新增：初始化表格尺寸监听（根治ResizeObserver报错）
function initTableResizeObserver() {
  if (!tableWrap.value) return;
  resizeObserver = new ResizeObserver((entries) => {
    setTimeout(() => {
      const wrap = entries[0].target;
      wrap.style.height = `${wrap.clientHeight}px`;
    }, 100);
  });
  resizeObserver.observe(tableWrap.value);
}

// 新增：销毁表格尺寸监听（避免内存泄漏）
function destroyTableResizeObserver() {
  if (resizeObserver) {
    resizeObserver.disconnect();
    resizeObserver = null;
  }
}

onMounted(() => {
  fetchCaseList();
  fetchElementList();
  initTableResizeObserver(); // 初始化监听
});

// 新增：组件卸载时销毁监听
onUnmounted(() => {
  destroyTableResizeObserver();
});
</script>

<style scoped lang="less">
.case-manage-root {
  padding: 20px;
  background: transparent;
}

.top-area {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  gap: 12px;
}

.action-area {
  flex-shrink: 0;
}

// 新增：表格包裹容器样式（修复报错用）
.table-wrap {
  width: 100%;
  min-height: 400px;
  overflow: auto;
  box-sizing: border-box;
  margin-bottom: 16px;
}

.case-table {
  background: #fff;
  border-radius: 8px;
  position: relative;
}

.steps-cell {
  white-space: normal;
  line-height: 1.4;
}

.el-table__header th {
  background: #f9fafb !important;
}

:deep(.el-loading-mask) {
  background-color: rgba(255, 255, 255, 0.65);
}

.dialog-form {
  padding-top: 4px;
}

@media (max-width: 768px) {
  .top-area {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
