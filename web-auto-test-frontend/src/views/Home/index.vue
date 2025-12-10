<template>
  <div class="home-root">
    <div class="page-header">
      <h2>平台概览</h2>
    </div>

    <div class="stats-grid">
      <el-card class="stat-card">
        <div class="card-content">
          <div class="card-left">
            <HomeFilled class="stat-icon" />
          </div>
          <div class="card-right">
            <div class="stat-value">{{ stats.todayTask }}</div>
            <div class="stat-label">今日执行任务数</div>
          </div>
        </div>
      </el-card>

      <el-card class="stat-card">
        <div class="card-content">
          <div class="card-left">
            <Files class="stat-icon" />
          </div>
          <div class="card-right">
            <div class="stat-value">{{ stats.totalCase }}</div>
            <div class="stat-label">累计用例数</div>
          </div>
        </div>
      </el-card>

      <el-card class="stat-card">
        <div class="card-content">
          <div class="card-left">
            <PieChart class="stat-icon" />
          </div>
          <div class="card-right">
            <div class="stat-value">{{ stats.passRate }}%</div>
            <div class="stat-label">今日通过率</div>
          </div>
        </div>
      </el-card>

      <el-card class="stat-card">
        <div class="card-content">
          <div class="card-left">
            <Timer class="stat-icon" />
          </div>
          <div class="card-right">
            <div class="stat-value">{{ stats.pendingTask }}</div>
            <div class="stat-label">待执行任务数</div>
          </div>
        </div>
      </el-card>
    </div>

    <div class="charts-area">
      <el-card class="chart-card">
        <div class="chart-title">近7天执行趋势</div>
        <div ref="lineChartRef" class="chart-container" />
      </el-card>

      <el-card class="chart-card">
        <div class="chart-title">各模块用例占比</div>
        <div ref="pieChartRef" class="chart-container" />
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount } from "vue";
import * as echarts from "echarts";

// 模拟统计数据
const stats = ref({
  todayTask: 24,
  totalCase: 1320,
  passRate: 92,
  pendingTask: 8,
});

// 模拟图表数据
const last7Days = getLast7Days();
const lineData = [12, 18, 20, 15, 22, 30, 24];
const pieData = [
  { name: "登录模块", value: 120 },
  { name: "订单模块", value: 300 },
  { name: "支付模块", value: 150 },
  { name: "用户模块", value: 200 },
];

const lineChartRef = ref(null);
const pieChartRef = ref(null);
let lineChart = null;
let pieChart = null;

function initLineChart() {
  if (!lineChartRef.value) return;
  lineChart = echarts.init(lineChartRef.value);
  const option = {
    tooltip: { trigger: "axis" },
    xAxis: {
      type: "category",
      data: last7Days,
    },
    yAxis: { type: "value" },
    series: [
      {
        name: "执行数",
        type: "line",
        data: lineData,
        smooth: true,
        areaStyle: {},
      },
    ],
  };
  lineChart.setOption(option);
}

function initPieChart() {
  if (!pieChartRef.value) return;
  pieChart = echarts.init(pieChartRef.value);
  const option = {
    tooltip: { trigger: "item" },
    legend: { orient: "vertical", left: "left" },
    series: [
      {
        name: "用例占比",
        type: "pie",
        radius: "50%",
        data: pieData,
        emphasis: {
          itemStyle: {
            shadowBlur: 10,
            shadowOffsetX: 0,
            shadowColor: "rgba(0, 0, 0, 0.5)",
          },
        },
      },
    ],
  };
  pieChart.setOption(option);
}

function resizeCharts() {
  try {
    if (lineChart) lineChart.resize();
    if (pieChart) pieChart.resize();
  } catch (e) {
    // ignore
  }
}

onMounted(() => {
  initLineChart();
  initPieChart();
  window.addEventListener("resize", resizeCharts);
});

onBeforeUnmount(() => {
  window.removeEventListener("resize", resizeCharts);
  if (lineChart) {
    lineChart.dispose();
    lineChart = null;
  }
  if (pieChart) {
    pieChart.dispose();
    pieChart = null;
  }
});

function getLast7Days() {
  const days = [];
  const now = new Date();
  for (let i = 6; i >= 0; i--) {
    const d = new Date(now.getTime() - i * 24 * 60 * 60 * 1000);
    days.push(`${d.getMonth() + 1}/${d.getDate()}`);
  }
  return days;
}
</script>

<style scoped lang="less">
.home-root {
  padding: 16px;
}

.page-header h2 {
  font-size: 18px;
  font-weight: 600;
  margin: 0 0 12px 0;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 20px;
  margin-bottom: 20px;
}

.stat-card {
  min-width: 200px;
  padding: 16px;
}

.card-content {
  display: flex;
  align-items: center;
}

.card-left {
  margin-right: 12px;
}

.stat-icon {
  font-size: 28px;
  color: #1989fa;
}

.stat-value {
  font-size: 20px;
  font-weight: 600;
}

.stat-label {
  color: #909399;
  margin-top: 6px;
}

.charts-area {
  display: flex;
  gap: 20px;
  align-items: stretch;
}

.chart-card {
  flex: 1;
  padding: 16px;
}

.chart-title {
  font-weight: 600;
  margin-bottom: 12px;
}

.chart-container {
  width: 100%;
  height: 300px;
}

@media (max-width: 768px) {
  .stats-grid {
    grid-template-columns: 1fr;
  }
  .charts-area {
    flex-direction: column;
  }
}
</style>
