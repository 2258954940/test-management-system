<template>
  <div class="report-root">
    <div class="top-row">
      <div class="query-wrap">
        <common-query-form
          :fields="queryFields"
          @onSearch="handleSearch"
          @onReset="handleReset"
        />
      </div>

      <div class="select-wrap">
        <el-form label-width="120px">
          <el-form-item label="选择已完成任务">
            <el-select v-model="selectedTaskId" placeholder="请选择已完成任务">
              <el-option
                v-for="t in finishedTasks"
                :key="t.id"
                :label="t.name"
                :value="t.id"
              />
            </el-select>
          </el-form-item>
        </el-form>
      </div>
    </div>

    <div class="stats-grid">
      <el-card class="stat-card">
        <div class="card-inner">
          <el-icon class="card-icon"><Numbers /></el-icon>
          <div class="card-body">
            <div class="card-title">总用例数</div>
            <div class="card-value">{{ stats.total }}</div>
          </div>
        </div>
      </el-card>

      <el-card class="stat-card">
        <div class="card-inner">
          <el-icon class="card-icon success"><CheckFilled /></el-icon>
          <div class="card-body">
            <div class="card-title">成功数</div>
            <div class="card-value">{{ stats.success }}</div>
          </div>
        </div>
      </el-card>

      <el-card class="stat-card">
        <div class="card-inner">
          <el-icon class="card-icon danger"><CloseFilled /></el-icon>
          <div class="card-body">
            <div class="card-title">失败数</div>
            <div class="card-value">{{ stats.failed }}</div>
          </div>
        </div>
      </el-card>

      <el-card class="stat-card">
        <div class="card-inner">
          <el-icon :class="['card-icon', pctColorClass]"
            ><PercentFilled
          /></el-icon>
          <div class="card-body">
            <div class="card-title">成功率</div>
            <div class="card-value">{{ stats.rateText }}</div>
          </div>
        </div>
      </el-card>
    </div>

    <div class="bottom-row">
      <div class="table-area">
        <el-card class="area-card">
          <div class="area-header">
            <h4>执行结果</h4>
          </div>
          <el-table :data="pagedReports" stripe style="width: 100%">
            <el-table-column prop="caseId" label="用例ID" width="100" />
            <el-table-column prop="caseName" label="用例名称" />
            <el-table-column prop="status" label="执行状态" width="120">
              <template #default="{ row }">
                <el-tag
                  :type="row.status === 'success' ? 'success' : 'danger'"
                  >{{ row.status === "success" ? "成功" : "失败" }}</el-tag
                >
              </template>
            </el-table-column>
            <el-table-column prop="duration" label="耗时(ms)" width="140" />
            <el-table-column prop="error" label="错误信息">
              <template #default="{ row }">{{ row.error || "-" }}</template>
            </el-table-column>
          </el-table>
        </el-card>
      </div>

      <div class="charts-area">
        <el-card class="area-card chart-card">
          <div class="chart-title">执行结果占比</div>
          <div ref="pieRef" class="chart-canvas" />
        </el-card>

        <el-card class="area-card chart-card">
          <div class="chart-title">用例执行耗时趋势</div>
          <div ref="lineRef" class="chart-canvas" />
        </el-card>
      </div>
    </div>

    <div class="pager-wrap">
      <common-pagination
        :total="page.total"
        :pageNum="page.pageNum"
        :pageSize="page.pageSize"
        @onPageChange="onPageChange"
      />
    </div>
  </div>
</template>

<script setup>
import {
  ref,
  reactive,
  computed,
  onMounted,
  onBeforeUnmount,
  watch,
  nextTick,
} from "vue";
import CommonQueryForm from "@/components/CommonQueryForm.vue";
import CommonPagination from "@/components/CommonPagination.vue";
import * as echarts from "echarts";
// import { ElMessage } from "element-plus";

// 模拟用例数据（与用例管理保持一致）
const caseOptions = ref([]);
const caseMap = reactive({});

function genCaseOptions(n = 15) {
  const arr = [];
  for (let i = 1; i <= n; i++) arr.push({ id: i, name: `用例-${i}` });
  return arr;
}

function initCases() {
  caseOptions.value = genCaseOptions(15);
  caseOptions.value.forEach((c) => {
    caseMap[c.id] = c;
  });
}

// 模拟任务调度数据（与 TaskSchedule 保持相近结构）
function genTasks(n = 12) {
  const modes = ["immediate", "timed"];
  const statuses = ["pending", "running", "finished", "failed"];
  const arr = [];
  for (let i = 1; i <= n; i++) {
    arr.push({
      id: i,
      name: `任务-${i}`,
      caseIds: [((i - 1) % 15) + 1, ((i + 2) % 15) + 1, ((i + 5) % 15) + 1],
      mode: modes[i % modes.length],
      status: i % 3 === 0 ? "finished" : statuses[i % statuses.length],
      createTime: Date.now() - i * 3600 * 1000,
    });
  }
  return arr;
}

const tasks = ref([]);

// 选择的已完成任务 id
const selectedTaskId = ref(null);

// 报告列表（当前任务的所有记录）
const allReports = ref([]);
const filteredReports = ref([]);

// 分页
const page = reactive({ pageNum: 1, pageSize: 8, total: 0 });

const pieRef = ref(null);
const lineRef = ref(null);
let pieChart = null;
let lineChart = null;

const queryFields = [
  { label: "任务名称", key: "taskName", placeholder: "任务名称" },
  { label: "执行时间", key: "timeRange", type: "date-range" },
];

onMounted(() => {
  initCases();
  tasks.value = genTasks(12);
  // 选中第一个已完成任务
  const f = finishedTasks.value[0];
  selectedTaskId.value = f ? f.id : null;
  if (selectedTaskId.value) generateReportsForSelected();
  nextTick(() => {
    initCharts();
    updateCharts();
  });
  window.addEventListener("resize", handleResize);
});

onBeforeUnmount(() => {
  window.removeEventListener("resize", handleResize);
  if (pieChart) pieChart.dispose();
  if (lineChart) lineChart.dispose();
});

const finishedTasks = computed(() =>
  tasks.value.filter((t) => t.status === "finished")
);

watch(selectedTaskId, () => {
  generateReportsForSelected();
  page.pageNum = 1;
});

function generateReportsForSelected() {
  const t = tasks.value.find((x) => x.id === selectedTaskId.value);
  if (!t) {
    allReports.value = [];
    filteredReports.value = [];
    page.total = 0;
    return;
  }
  allReports.value = genReportForTask(t);
  // 初始不过滤
  filteredReports.value = [...allReports.value];
  page.total = filteredReports.value.length;
  updateStats();
  nextTick(() => updateCharts());
}

function genReportForTask(task) {
  const totalCases = caseOptions.value.length;
  const arr = [];
  const baseTime = task.createTime || Date.now();
  for (let i = 1; i <= 12; i++) {
    const cid = ((task.id + i - 2) % totalCases) + 1;
    const cname = caseMap[cid] ? caseMap[cid].name : `用例-${cid}`;
    const success = Math.random() < 0.75;
    const duration = Math.floor(Math.random() * 1800) + 100;
    arr.push({
      id: `${task.id}-${i}`,
      taskId: task.id,
      taskName: task.name,
      caseId: cid,
      caseName: cname,
      status: success ? "success" : "failed",
      duration,
      error: success ? "" : `元素未找到错误: 元素 #btn-${cid}`,
      execTime: baseTime + i * 60 * 1000,
    });
  }
  return arr;
}

const stats = reactive({
  total: 0,
  success: 0,
  failed: 0,
  rate: 0,
  rateText: "0%",
});

function updateStats() {
  const list = filteredReports.value;
  stats.total = list.length;
  stats.success = list.filter((r) => r.status === "success").length;
  stats.failed = list.filter((r) => r.status === "failed").length;
  stats.rate = stats.total
    ? Math.round((stats.success / stats.total) * 100)
    : 0;
  stats.rateText = `${stats.rate}%`;
}

const pctColorClass = computed(() => {
  if (stats.rate >= 90) return "success";
  if (stats.rate >= 60) return "warning";
  return "danger";
});

function handleSearch(form) {
  // form: { taskName, timeRange }
  let res = [...allReports.value];
  if (form.taskName && form.taskName.trim()) {
    const kw = form.taskName.trim().toLowerCase();
    res = res.filter(
      (r) =>
        r.caseName.toLowerCase().includes(kw) ||
        r.taskName.toLowerCase().includes(kw)
    );
  }
  if (form.timeRange && form.timeRange.length === 2) {
    const [s, e] = form.timeRange.map((d) => new Date(d).getTime());
    res = res.filter((r) => r.execTime >= s && r.execTime <= e);
  }
  filteredReports.value = res;
  page.total = res.length;
  page.pageNum = 1;
  updateStats();
  nextTick(() => updateCharts());
}

function handleReset() {
  filteredReports.value = [...allReports.value];
  page.total = filteredReports.value.length;
  page.pageNum = 1;
  updateStats();
  nextTick(() => updateCharts());
}

const pagedReports = computed(() => {
  const start = (page.pageNum - 1) * page.pageSize;
  return filteredReports.value.slice(start, start + page.pageSize);
});

function onPageChange({ pageNum, pageSize }) {
  page.pageNum = pageNum;
  page.pageSize = pageSize;
}

function initCharts() {
  if (pieRef.value) pieChart = echarts.init(pieRef.value);
  if (lineRef.value) lineChart = echarts.init(lineRef.value);
}

function updateCharts() {
  if (!pieChart || !lineChart) return;
  const succ = filteredReports.value.filter(
    (r) => r.status === "success"
  ).length;
  const fail = filteredReports.value.filter(
    (r) => r.status === "failed"
  ).length;
  const pieOption = {
    tooltip: { trigger: "item" },
    legend: { bottom: 0 },
    series: [
      {
        name: "执行结果",
        type: "pie",
        radius: ["40%", "70%"],
        avoidLabelOverlap: false,
        label: { show: false },
        emphasis: { label: { show: true, fontSize: 14, fontWeight: "bold" } },
        data: [
          { value: succ, name: "成功" },
          { value: fail, name: "失败" },
        ],
      },
    ],
  };

  const names = filteredReports.value.map((r) => r.caseName);
  const durations = filteredReports.value.map((r) => r.duration);
  const lineOption = {
    tooltip: { trigger: "axis" },
    xAxis: { type: "category", data: names },
    yAxis: { type: "value" },
    series: [{ data: durations, type: "line", smooth: true }],
  };

  pieChart.setOption(pieOption);
  lineChart.setOption(lineOption);
}

function handleResize() {
  if (pieChart) pieChart.resize();
  if (lineChart) lineChart.resize();
}

// function formatTimestamp(ts) {
//   const d = new Date(ts);
//   const Y = d.getFullYear();
//   const M = String(d.getMonth() + 1).padStart(2, "0");
//   const D = String(d.getDate()).padStart(2, "0");
//   const h = String(d.getHours()).padStart(2, "0");
//   const m = String(d.getMinutes()).padStart(2, "0");
//   const s = String(d.getSeconds()).padStart(2, "0");
//   return `${Y}-${M}-${D} ${h}:${m}:${s}`;
// }
</script>

<style scoped lang="less">
.report-root {
  padding: 20px;
}

.top-row {
  display: flex;
  gap: 16px;
  align-items: flex-start;
  flex-wrap: wrap;
  margin-bottom: 16px;
}

.query-wrap {
  flex: 1 1 600px;
}

.select-wrap {
  width: 320px;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-top: 16px;
  margin-bottom: 16px;
}

.stat-card {
  background: #fff;
  padding: 16px;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.card-inner {
  display: flex;
  align-items: center;
  gap: 12px;
}

.card-icon {
  font-size: 24px;
}

.card-icon.success {
  color: #67c23a;
}

.card-icon.danger {
  color: #f56c6c;
}

.card-icon.warning {
  color: #e6a23c;
}

.card-body .card-title {
  color: #909399;
  font-size: 12px;
}

.card-body .card-value {
  font-weight: 700;
  font-size: 20px;
}

.bottom-row {
  display: flex;
  gap: 16px;
  align-items: flex-start;
  flex-wrap: wrap;
}

.table-area {
  flex: 1 1 50%;
}

.charts-area {
  flex: 1 1 50%;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.area-card {
  background: #fff;
  padding: 16px;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.chart-card {
  height: 300px;
  display: flex;
  flex-direction: column;
}

.chart-canvas {
  flex: 1 1 auto;
  min-height: 180px;
}

.chart-title {
  font-weight: 600;
  margin-bottom: 8px;
}

.pager-wrap {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

@media (max-width: 900px) {
  .stats-grid {
    grid-template-columns: 1fr 1fr;
  }
  .bottom-row {
    flex-direction: column;
  }
  .select-wrap {
    width: 100%;
  }
}

@media (max-width: 600px) {
  .stats-grid {
    grid-template-columns: 1fr;
  }
}
</style>
