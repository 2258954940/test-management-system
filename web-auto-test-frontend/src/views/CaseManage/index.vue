<template>
  <div class="case-manage-root">
    <div class="top-area">
      <common-query-form
        :fields="queryFields"
        @onSearch="handleSearch"
        @onReset="handleReset"
      />
      <div class="action-area">
        <el-button type="primary" @click="handleAdd">新增用例</el-button>
      </div>
    </div>

    <el-table
      :data="displayList"
      stripe
      style="width: 100%"
      class="case-table"
      row-key="id"
      :loading="loading"
    >
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="name" label="用例名称" />
      <el-table-column prop="url" label="URL" min-width="180" />
      <el-table-column prop="locatorType" label="定位方式" width="120" />
      <el-table-column prop="locatorValue" label="定位值" min-width="180" />
      <el-table-column prop="actionType" label="操作类型" width="120" />
      <el-table-column prop="inputData" label="输入数据" min-width="160" />
      <el-table-column prop="expectedResult" label="预期结果" min-width="160" />
      <el-table-column prop="createTime" label="创建时间" width="200">
        <template #default="{ row }">{{ formatDate(row.createTime) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="240">
        <template #default="{ row }">
          <el-button type="text" size="small" @click="handleEdit(row)"
            >编辑</el-button
          >
          <el-button type="text" size="small" @click="handleDelete(row.id)"
            >删除</el-button
          >
          <el-button type="text" size="small" @click="handleRun(row.id)"
            >执行</el-button
          >
        </template>
      </el-table-column>
    </el-table>

    <common-pagination
      :total="pageParams.total"
      :pageNum="pageParams.pageNum"
      :pageSize="pageParams.pageSize"
      @onPageChange="handlePageChange"
    />
  </div>
</template>

<script setup>
import { reactive, ref, computed, onMounted } from "vue";
import { ElMessage } from "element-plus";
import CommonQueryForm from "@/components/CommonQueryForm.vue";
import CommonPagination from "@/components/CommonPagination.vue";
import { getCaseList } from "@/api/case";

// 查询字段配置
const queryFields = [
  {
    label: "用例名称",
    key: "caseName",
    type: "input",
    placeholder: "请输入用例名称",
  },
  {
    label: "所属模块",
    key: "module",
    type: "select",
    options: [
      { label: "登录模块", value: "login" },
      { label: "订单模块", value: "order" },
      { label: "支付模块", value: "pay" },
    ],
  },
  {
    label: "执行状态",
    key: "status",
    type: "select",
    options: [
      { label: "待执行", value: "pending" },
      { label: "执行中", value: "running" },
      { label: "已完成", value: "finished" },
    ],
  },
];

const fullList = ref([]);
const pageParams = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0,
});

const filterParams = reactive({
  caseName: "",
  module: "",
  status: "",
});

const loading = ref(false);

const displayList = computed(() => {
  const start = (pageParams.pageNum - 1) * pageParams.pageSize;
  return fullList.value.slice(start, start + pageParams.pageSize);
});

async function fetchCaseList() {
  loading.value = true;
  try {
    const params = {
      pageNum: pageParams.pageNum,
      pageSize: pageParams.pageSize,
      caseName: filterParams.caseName || undefined,
      module: filterParams.module || undefined,
      status: filterParams.status || undefined,
    };
    const res = (await getCaseList(params)) || {};
    const list = Array.isArray(res?.data) ? res.data : res?.list || res || [];
    fullList.value = Array.isArray(list) ? list : [];
    pageParams.total = res?.total ?? fullList.value.length ?? 0;
  } catch (err) {
    fullList.value = [];
    ElMessage.error("获取用例列表失败");
  } finally {
    loading.value = false;
  }
}

function handleSearch(params) {
  filterParams.caseName = params.caseName || "";
  filterParams.module = params.module || "";
  filterParams.status = params.status || "";
  pageParams.pageNum = 1;
  fetchCaseList();
}

function handleReset() {
  filterParams.caseName = "";
  filterParams.module = "";
  filterParams.status = "";
  pageParams.pageNum = 1;
  fetchCaseList();
}

function handlePageChange({ pageNum, pageSize }) {
  pageParams.pageNum = pageNum;
  pageParams.pageSize = pageSize;
  fetchCaseList();
}

function handleAdd() {
  ElMessage.info("新增用例功能待对接后端");
}

function handleEdit(row) {
  ElMessage.info(`编辑用例(${row?.id || "--"}) 功能待对接后端`);
}

function handleDelete(id) {
  ElMessage.info(`删除用例(${id || "--"}) 功能待对接后端`);
}

function handleRun(id) {
  ElMessage.info(`执行用例(${id || "--"}) 功能待对接后端`);
}

function formatDate(ts) {
  if (!ts) return "-";
  const d = new Date(ts);
  if (Number.isNaN(d.getTime())) return ts;
  const y = d.getFullYear();
  const m = String(d.getMonth() + 1).padStart(2, "0");
  const day = String(d.getDate()).padStart(2, "0");
  const hh = String(d.getHours()).padStart(2, "0");
  const mm = String(d.getMinutes()).padStart(2, "0");
  const ss = String(d.getSeconds()).padStart(2, "0");
  return `${y}-${m}-${day} ${hh}:${mm}:${ss}`;
}

onMounted(() => {
  fetchCaseList();
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
}

.action-area {
  margin-left: 16px;
}

.case-table {
  background: #fff;
  border-radius: 8px;
  position: relative;
}

.el-table__header th {
  background: #f9fafb !important;
}

:deep(.el-loading-mask) {
  background-color: rgba(255, 255, 255, 0.65);
}

.dialog-body {
  padding: 12px 0;
}

@media (max-width: 768px) {
  .top-area {
    flex-direction: column;
    align-items: stretch;
    gap: 12px;
  }
}
</style>
