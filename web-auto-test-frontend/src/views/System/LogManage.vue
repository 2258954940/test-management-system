<template>
  <div class="log-root">
    <el-form :model="filters" inline class="query-form">
      <el-form-item label="用户名">
        <el-input v-model="filters.username" placeholder="用户名"></el-input>
      </el-form-item>

      <el-form-item label="操作类型">
        <el-select v-model="filters.operationType" placeholder="请选择">
          <el-option label="全部" value="" />
          <el-option label="新增用户" value="新增用户" />
          <el-option label="编辑用户" value="编辑用户" />
          <el-option label="删除用户" value="删除用户" />
          <el-option label="新增测试用例" value="新增测试用例" />
          <el-option label="执行测试用例" value="执行测试用例" />
          <el-option label="新增调度任务" value="新增调度任务" />
        </el-select>
      </el-form-item>

      <el-form-item label="时间范围">
        <el-date-picker
          v-model="filters.timeRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          value-format="YYYY-MM-DD HH:mm:ss"
        />
      </el-form-item>

      <el-form-item>
        <el-button type="text" @click="handleReset">重置</el-button>
        <el-button type="primary" @click="handleSearch">查询</el-button>
      </el-form-item>
    </el-form>

    <el-card class="list-card">
      <el-table
        :data="logList"
        stripe
        style="width: 100%"
        row-key="id"
        v-loading="loading"
      >
        <el-table-column prop="id" label="日志ID" width="80" />
        <el-table-column prop="username" label="用户名" width="120" />
        <el-table-column prop="operationType" label="操作类型" width="120" />
        <el-table-column prop="operationContent" label="操作内容" />
        <el-table-column prop="createTime" label="操作时间" width="200">
          <template #default="{ row }">{{
            formatDate(row.createTime)
          }}</template>
        </el-table-column>
      </el-table>
      <div class="pagination-wrap">
        <common-pagination
          :total="page.total"
          :pageNum="page.pageNum"
          :pageSize="page.pageSize"
          @onPageChange="onPageChange"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from "vue";
import CommonPagination from "@/components/CommonPagination.vue";
import { ElMessage } from "element-plus";
import { getLogList } from "@/api/system";

const loading = ref(false);
const logList = ref([]);
const page = reactive({ pageNum: 1, pageSize: 10, total: 0 });
const filters = reactive({
  username: "",
  operationType: "",
  timeRange: [],
});

// 日期格式化
function formatDate(dateStr) {
  if (!dateStr) return "";
  const d = new Date(dateStr);
  const Y = d.getFullYear();
  const M = String(d.getMonth() + 1).padStart(2, "0");
  const D = String(d.getDate()).padStart(2, "0");
  const h = String(d.getHours()).padStart(2, "0");
  const m = String(d.getMinutes()).padStart(2, "0");
  const s = String(d.getSeconds()).padStart(2, "0");
  return `${Y}-${M}-${D} ${h}:${m}:${s}`;
}

// 查询日志列表（核心：传递分页参数）
async function handleSearch() {
  loading.value = true;
  try {
    const params = {
      username: filters.username,
      operationType: filters.operationType,
      start: filters.timeRange?.[0] || "",
      end: filters.timeRange?.[1] || "",
      pageNum: page.pageNum,
      pageSize: page.pageSize, // 必须传pageSize
    };
    console.log("请求参数：", params); // 调试用，看是否正确
    const res = await getLogList(params);
    if (res.code === 200) {
      logList.value = res.data.list || [];
      page.total = res.data.total || 0;
      page.pageNum = res.data.pageNum || page.pageNum;
      page.pageSize = res.data.pageSize || page.pageSize;
      console.log("后端返回：", res.data); // 调试用，看total是否正确
    } else {
      ElMessage.error("查询日志失败：" + res.msg);
    }
  } catch (err) {
    ElMessage.error("查询日志失败：" + err.message);
  } finally {
    loading.value = false;
  }
}

// 重置查询条件
function handleReset() {
  filters.username = "";
  filters.operationType = "";
  filters.timeRange = [];
  page.pageNum = 1; // 重置页码
  handleSearch();
}

// 分页切换（核心：重新查询）
function onPageChange({ pageNum, pageSize }) {
  page.pageNum = pageNum;
  page.pageSize = pageSize;
  handleSearch(); // 切换分页后重新查询
}

// 页面加载时查询
onMounted(() => {
  handleSearch();
});
</script>

<style scoped lang="less">
.log-root {
  padding: 20px;
}
.query-form {
  margin-bottom: 16px;
  background: #fff;
  padding: 12px;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}
.list-card {
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}
.pagination-wrap {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>
