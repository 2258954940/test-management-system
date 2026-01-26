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

      <div class="action-wrap">
        <el-button type="primary" @click="handleExportExcel">
          <el-icon style="margin-right: 6px"><Download /></el-icon>
          导出Excel报告
        </el-button>
      </div>
    </div>

    <!-- 任务概览列表 -->
    <div class="task-overview">
      <el-card class="area-card">
        <div class="area-header">
          <h4>任务概览</h4>
        </div>
        <el-table
          :data="finishedTasks"
          border
          height="300"
          style="width: 100%"
          v-loading="isLoading"
        >
          <el-table-column prop="id" label="任务ID" width="100" />
          <el-table-column prop="taskName" label="任务名称" />
          <el-table-column label="执行时间" width="180">
            <template #default="{ row }">{{
              formatDate(row.createTime)
            }}</template>
          </el-table-column>
          <el-table-column prop="totalCase" label="总用例数" width="100" />
          <el-table-column prop="successCase" label="成功数" width="100" />
          <el-table-column prop="failCase" label="失败数" width="100" />
          <el-table-column prop="successRate" label="成功率" width="100" />
          <el-table-column label="操作" width="140" align="center">
            <template #default="{ row }">
              <el-button
                type="primary"
                size="small"
                @click="viewTaskDetails(row)"
                >查看详情</el-button
              >
            </template>
          </el-table-column>
        </el-table>
      </el-card>
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
        <el-card class="area-card" v-loading="isLoading">
          <div class="area-header">
            <h4>执行结果</h4>
          </div>
          <template v-if="filteredReports.length > 0">
            <el-table :data="pagedReports" stripe style="width: 100%">
              <el-table-column prop="caseId" label="用例ID" width="100" />
              <el-table-column prop="caseName" label="用例名称" />
              <el-table-column prop="status" label="执行状态" width="120">
                <template #default="{ row }">
                  <el-tag
                    :type="row.status === 'success' ? 'success' : 'danger'"
                  >
                    {{ row.status === "success" ? "成功" : "失败" }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="duration" label="耗时(ms)" width="140" />
              <el-table-column prop="error" label="错误信息">
                <template #default="{ row }">{{ row.error || "-" }}</template>
              </el-table-column>
            </el-table>
          </template>
          <template v-else>
            <el-empty
              description="请选择上方任务概览的「查看详情」加载用例执行结果"
            />
          </template>
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
  nextTick,
} from "vue";
import CommonQueryForm from "@/components/CommonQueryForm.vue";
import CommonPagination from "@/components/CommonPagination.vue";
import * as echarts from "echarts";
import { ElMessage } from "element-plus";
import { exportExcel } from "@/api/report";
import { Download } from "@element-plus/icons-vue";
// 1. 新增导入：真实任务列表接口
import { getFinishedTasks } from "@/api/task";
import service from "@/utils/request";
import { formatDate } from "@/utils/date";

// 2. 真实任务与加载状态
const finishedTasks = ref([]);
const selectedTaskId = ref(null);
const isLoading = ref(false);

// 3. 修改导出方法：传递选中的taskId给后端
async function handleExportExcel() {
  if (!selectedTaskId.value) {
    ElMessage.warning("请先选择一个已完成任务！");
    return;
  }
  try {
    const blob = await exportExcel(selectedTaskId.value); // 传递taskId
    const url = window.URL.createObjectURL(new Blob([blob]));
    const link = document.createElement("a");
    link.href = url;
    link.download = `测试报告-${new Date().toISOString().slice(0, 10)}-任务${
      selectedTaskId.value
    }.xlsx`;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    window.URL.revokeObjectURL(url);
    ElMessage.success("导出成功");
  } catch (err) {
    ElMessage.error("导出失败，请稍后重试");
  }
}

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

// 4. 修改onMounted：优先加载真实任务
onMounted(async () => {
  // 调用后端接口获取真实已完成任务
  try {
    const res = await getFinishedTasks();
    finishedTasks.value = res.data || [];
  } catch (err) {
    ElMessage.error("获取任务列表失败，使用模拟数据");
    finishedTasks.value = [];
  }
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

// 5. 真实数据：查看任务详情并拉取用例执行结果
async function viewTaskDetails(row) {
  if (!row || !row.id) return;
  selectedTaskId.value = row.id;
  await generateReportsForSelected(row.id);
}

async function generateReportsForSelected(taskId) {
  isLoading.value = true;
  try {
    // 清空旧数据
    allReports.value = [];
    filteredReports.value = [];
    page.total = 0;

    // 请求真实接口 /api/results/by-task
    const res = await service.get("/api/results/by-task", {
      params: { taskId },
    });
    const list = Array.isArray(res.data) ? res.data : [];
    const taskInfo = finishedTasks.value.find((t) => t.id === taskId) || {
      taskName: "",
    };

    // 转换数据为页面需要的结构
    const mapped = list.map((r, idx) => ({
      id: r.id ?? `${taskId}-${idx}`,
      taskId: r.taskId ?? taskId,
      taskName: taskInfo.taskName,
      caseId: r.caseId,
      caseName: `用例-${r.caseId}`,
      status: r.status === "PASS" ? "success" : "failed",
      duration: r.durationMs ?? r.duration ?? 0,
      error: r.status === "FAILED" ? r.message || "" : "",
      execTime: r.runTime ? new Date(r.runTime).getTime() : Date.now(),
    }));

    allReports.value = mapped;
    filteredReports.value = [...mapped];
    page.total = filteredReports.value.length;
    updateStats();
    nextTick(() => updateCharts());
  } catch (err) {
    ElMessage.error("加载任务用例结果失败");
  } finally {
    isLoading.value = false;
  }
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

/* 移除旧下拉区域 */

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

/* 新增：任务概览与下方模块间距 */
.task-overview {
  margin-bottom: 20px;
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
}

@media (max-width: 600px) {
  .stats-grid {
    grid-template-columns: 1fr;
  }
}
</style>
