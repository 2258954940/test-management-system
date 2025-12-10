<template>
  <div class="log-root">
    <div class="top-area">
      <common-query-form
        :fields="fields"
        @onSearch="onSearch"
        @onReset="onReset"
      />
    </div>

    <el-card class="list-card">
      <el-table
        :data="pagedLogs"
        stripe
        style="width: 100%"
        :header-cell-style="headerStyle"
      >
        <el-table-column prop="id" label="日志ID" width="100" />
        <el-table-column prop="username" label="用户名" width="140" />
        <el-table-column prop="type" label="操作类型" width="160" />
        <el-table-column prop="content" label="操作内容" />
        <el-table-column prop="time" label="操作时间" width="200">
          <template #default="{ row }">{{ formatDate(row.time) }}</template>
        </el-table-column>
        <el-table-column prop="ip" label="IP地址" width="140" />
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
import { ref, reactive, computed, onMounted } from "vue";
import CommonQueryForm from "@/components/CommonQueryForm.vue";
import CommonPagination from "@/components/CommonPagination.vue";

// 查询字段配置
const fields = [
  { label: "用户名", key: "username", placeholder: "用户名" },
  {
    label: "操作类型",
    key: "opType",
    type: "select",
    options: [
      { label: "全部", value: "all" },
      { label: "新增用例", value: "新增用例" },
      { label: "执行任务", value: "执行任务" },
      { label: "修改配置", value: "修改配置" },
      { label: "删除用例", value: "删除用例" },
    ],
  },
  { label: "时间范围", key: "timeRange", type: "date-range" },
];

// 模拟日志数据
const logsAll = ref([]);
const filtered = ref([]);

const page = reactive({ pageNum: 1, pageSize: 8, total: 0 });

function randomRecentTime() {
  // 最近7天内
  return Date.now() - Math.floor(Math.random() * 7 * 24 * 3600 * 1000);
}

function genLogs(n = 20) {
  const types = ["新增用例", "执行任务", "修改配置", "删除用例"];
  const arr = [];
  for (let i = 1; i <= n; i++) {
    const t = types[i % types.length];
    arr.push({
      id: i,
      username: `user${((i - 1) % 10) + 1}`,
      type: t,
      content: `${t} 执行详情 #${i}`,
      time: randomRecentTime(),
      ip: `192.168.0.${(i % 254) + 1}`,
    });
  }
  return arr;
}

onMounted(() => {
  logsAll.value = genLogs(20);
  filtered.value = [...logsAll.value];
  page.total = filtered.value.length;
});

function formatDate(ts) {
  const d = new Date(ts);
  const Y = d.getFullYear();
  const M = String(d.getMonth() + 1).padStart(2, "0");
  const D = String(d.getDate()).padStart(2, "0");
  const h = String(d.getHours()).padStart(2, "0");
  const m = String(d.getMinutes()).padStart(2, "0");
  const s = String(d.getSeconds()).padStart(2, "0");
  return `${Y}-${M}-${D} ${h}:${m}:${s}`;
}

function onSearch(form) {
  let res = [...logsAll.value];
  if (form.username && form.username.trim()) {
    const kw = form.username.trim().toLowerCase();
    res = res.filter((l) => l.username.toLowerCase().includes(kw));
  }
  if (form.opType && form.opType !== "all") {
    res = res.filter((l) => l.type === form.opType);
  }
  if (form.timeRange && form.timeRange.length === 2) {
    const [s, e] = form.timeRange.map((d) => new Date(d).getTime());
    res = res.filter((l) => l.time >= s && l.time <= e);
  }
  filtered.value = res;
  page.total = filtered.value.length;
  page.pageNum = 1;
}

function onReset() {
  filtered.value = [...logsAll.value];
  page.total = filtered.value.length;
  page.pageNum = 1;
}

const pagedLogs = computed(() => {
  const start = (page.pageNum - 1) * page.pageSize;
  return filtered.value.slice(start, start + page.pageSize);
});

function onPageChange({ pageNum, pageSize }) {
  page.pageNum = pageNum;
  page.pageSize = pageSize;
}

const headerStyle = { background: "#f9fafb", height: "48px" };
</script>

<style scoped lang="less">
.log-root {
  padding: 20px;
}

.list-card {
  background: #fff;
  padding: 12px;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.top-area {
  margin-bottom: 16px;
}

.pagination-wrap {
  margin-top: 12px;
  display: flex;
  justify-content: flex-end;
}

@media (max-width: 768px) {
  .top-area {
    width: 100%;
  }
}
</style>
