<template>
  <div class="task-schedule-root">
    <div class="top-card">
      <h3>创建任务</h3>
      <el-form :model="taskForm" label-width="100px" class="task-form">
        <el-form-item label="任务名称">
          <el-input v-model="taskForm.name" placeholder="请输入任务名称" />
        </el-form-item>

        <el-form-item label="关联用例">
          <el-select
            v-model="taskForm.caseIds"
            multiple
            placeholder="请选择用例"
          >
            <el-option
              v-for="c in caseOptions"
              :key="c.id"
              :label="c.name"
              :value="c.id"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="执行方式">
          <el-radio-group v-model="taskForm.mode">
            <el-radio label="immediate">立即执行</el-radio>
            <el-radio label="timed">定时执行</el-radio>
          </el-radio-group>
        </el-form-item>

        <div v-if="taskForm.mode === 'timed'" class="timed-row">
          <el-form-item label="执行日期">
            <el-date-picker
              v-model="taskForm.date"
              type="date"
              placeholder="选择日期"
            />
          </el-form-item>
          <el-form-item label="执行时间">
            <el-time-picker v-model="taskForm.time" placeholder="选择时间" />
          </el-form-item>
        </div>

        <el-form-item>
          <el-button type="primary" @click="createTask">提交任务</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="list-card">
      <div class="list-actions">
        <el-button type="primary" @click="refreshTasks">刷新列表</el-button>
      </div>

      <el-table
        :data="pagedTasks"
        stripe
        style="width: 100%"
        class="task-table"
      >
        <el-table-column prop="id" label="任务ID" width="100" />
        <el-table-column prop="taskName" label="任务名称" />
        <el-table-column prop="caseId" label="关联用例">
          <template #default="{ row }">
            <div class="case-names">
              <el-tag
                v-for="cid in row.caseId.split(',')"
                :key="cid"
                type="info"
              >
                {{ caseMap[cid]?.name || cid }}
              </el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="execType" label="执行方式" width="120">
          <template #default="{ row }">{{
            row.execType === "immediate" ? "立即" : "定时"
          }}</template>
        </el-table-column>
        <el-table-column prop="status" label="执行状态" width="120">
          <template #default="{ row }">
            <el-tag :type="statusTag(row.status)">{{
              statusLabel(row.status)
            }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="200">
          <template #default="{ row }">{{
            formatDate(row.createTime)
          }}</template>
        </el-table-column>
        <el-table-column label="操作" width="240">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="runTask(row)"
              >执行</el-button
            >
            <el-button
              type="warning"
              size="small"
              @click="stopTask(row)"
              style="margin-left: 8px"
              >终止</el-button
            >
            <el-button
              type="text"
              size="small"
              @click="viewLog(row)"
              style="margin-left: 8px"
              >查看日志</el-button
            >
          </template>
        </el-table-column>
      </el-table>

      <common-pagination
        :total="taskPage.total"
        :pageNum="taskPage.pageNum"
        :pageSize="taskPage.pageSize"
        @onPageChange="onTaskPageChange"
      />
    </div>

    <!-- 日志弹窗 终极修复版：强制显示+最高层级+body挂载+遮罩层正常 -->
    <el-dialog
      v-model="logDialog.visible"
      width="600px"
      title="任务执行日志"
      append-to-body
      align-center
      :close-on-click-modal="false"
      :z-index="9999"
    >
      <div class="log-body">
        <ul>
          <li v-for="(l, idx) in logDialog.logs" :key="idx">{{ l }}</li>
        </ul>
      </div>
      <template #footer>
        <el-button @click="logDialog.visible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
// 必须导入nextTick，用于强制触发视图更新
import { ref, reactive, computed, onMounted, watch, nextTick } from "vue";
import CommonPagination from "@/components/CommonPagination.vue";
import { ElMessage } from "element-plus";
// 导入真实接口
import {
  getTaskList,
  createTask as apiCreateTask,
  runTask as apiRunTask,
  stopTask as apiStopTask,
  getTaskLog,
} from "@/api/task";
import { getCaseList } from "@/api/case";

// 用例列表（真实接口）
const caseOptions = ref([]);
const caseMap = reactive({});

// 初始化用例列表
async function initCases() {
  try {
    const res = await getCaseList();
    caseOptions.value = res.data || [];
    caseOptions.value.forEach((c) => {
      caseMap[c.id] = c;
    });
  } catch (err) {
    ElMessage.error("获取用例列表失败，使用模拟数据");
    const mockCases = [];
    for (let i = 1; i <= 15; i++) {
      mockCases.push({ id: i, name: `用例-${i}` });
    }
    caseOptions.value = mockCases;
    mockCases.forEach((c) => {
      caseMap[c.id] = c;
    });
  }
}

// 任务列表（真实接口）
const tasks = ref([]);
const taskForm = reactive({
  name: "",
  caseIds: [],
  mode: "immediate",
  date: null,
  time: null,
});
const taskPage = reactive({ pageNum: 1, pageSize: 5, total: 0 });
// 核心恢复：改回最初的reactive定义，彻底解决渲染报错
const logDialog = reactive({ visible: false, logs: [] });

// 获取任务列表
async function loadTasks() {
  try {
    const res = await getTaskList({
      pageNum: taskPage.pageNum,
      pageSize: taskPage.pageSize,
    });
    tasks.value = res.data.records || [];
    taskPage.total = res.data.total || 0;
  } catch (err) {
    ElMessage.error("获取任务列表失败，使用模拟数据");
    const mockTasks = [];
    for (let i = 1; i <= 10; i++) {
      mockTasks.push({
        id: i,
        taskName: `任务-${i}`,
        caseId: [((i - 1) % 15) + 1].join(","),
        execType: i % 2 === 0 ? "immediate" : "timed",
        status: i % 4 === 0 ? "finished" : "pending",
        createTime: new Date(Date.now() - i * 3600 * 1000).toISOString(),
      });
    }
    tasks.value = mockTasks;
    taskPage.total = mockTasks.length;
  }
}

onMounted(async () => {
  await initCases();
  await loadTasks();
});

// 监听任务列表变化，同步分页总数
watch(
  () => tasks.value,
  (newTasks) => {
    taskPage.total = newTasks.length;
  },
  { immediate: true }
);

// 分页计算
const pagedTasks = computed(() => {
  const start = (taskPage.pageNum - 1) * taskPage.pageSize;
  return tasks.value.slice(start, start + taskPage.pageSize);
});

// 日期格式化
function formatDate(ts) {
  if (!ts) return "-";
  const d = new Date(ts);
  const y = d.getFullYear();
  const m = String(d.getMonth() + 1).padStart(2, "0");
  const day = String(d.getDate()).padStart(2, "0");
  const hh = String(d.getHours()).padStart(2, "0");
  const mm = String(d.getMinutes()).padStart(2, "0");
  const ss = String(d.getSeconds()).padStart(2, "0");
  return `${y}-${m}-${day} ${hh}:${mm}:${ss}`;
}

// 状态标签映射
function statusLabel(s) {
  if (s === "pending") return "待执行";
  if (s === "running") return "执行中";
  if (s === "finished") return "已完成";
  if (s === "failed") return "失败";
  return s;
}
function statusTag(s) {
  if (s === "pending") return "";
  if (s === "running") return "info";
  if (s === "finished") return "success";
  if (s === "failed") return "danger";
  return "";
}

// 创建任务
async function createTask() {
  if (!taskForm.name || !taskForm.name.trim()) {
    ElMessage.error("任务名称必填");
    return;
  }
  if (!taskForm.caseIds || !taskForm.caseIds.length) {
    ElMessage.error("请至少选择一个用例");
    return;
  }
  try {
    await apiCreateTask({
      name: taskForm.name.trim(),
      caseIds: taskForm.caseIds.join(","),
      mode: taskForm.mode,
      date: taskForm.date,
      time: taskForm.time,
    });
    ElMessage.success("创建任务成功");
    taskForm.name = "";
    taskForm.caseIds = [];
    taskForm.mode = "immediate";
    taskForm.date = null;
    taskForm.time = null;
    await loadTasks();
  } catch (err) {
    ElMessage.error("创建任务失败：" + (err.message || "未知错误"));
  }
}

// 刷新任务列表
async function refreshTasks() {
  await loadTasks();
  ElMessage.info("任务列表已刷新");
}

// 执行任务
async function runTask(row) {
  try {
    await apiRunTask(row.id);
    ElMessage.success(`任务 ${row.taskName} 开始执行`);
    setTimeout(() => loadTasks(), 1000);
  } catch (err) {
    ElMessage.error("执行任务失败：" + (err.message || "未知错误"));
  }
}

// 终止任务
async function stopTask(row) {
  try {
    await apiStopTask(row.id);
    ElMessage.warning(`任务 ${row.taskName} 已终止`);
    setTimeout(() => loadTasks(), 1000);
  } catch (err) {
    ElMessage.error("终止任务失败：" + (err.message || "未知错误"));
  }
}

// 查看日志【全链路调试+nextTick优化+强制错误捕获+请求超时兜底】
async function viewLog(row) {
  console.log("===== 开始执行查看日志 =====");
  console.log("任务行数据：", row);
  if (!row || !row.id) {
    ElMessage.warning("任务ID无效，无法获取日志");
    console.log("===== 查看日志终止：任务ID无效 =====");
    return;
  }

  console.log(`开始请求后端日志接口：/api/task/log/${row.id}`);
  try {
    // 强制设置请求超时（防止后端卡死，前端一直等）
    const res = await Promise.race([
      getTaskLog(row.id),
      new Promise((_, reject) => {
        setTimeout(
          () => reject(new Error("请求超时：后端接口5秒未响应")),
          5000
        );
      }),
    ]);
    // 打印后端返回的「完整响应数据」
    console.log("后端接口响应成功，完整数据：", res);
    const realLogs = res.data?.logs || [];
    logDialog.logs =
      realLogs.length > 0 ? realLogs : [`任务 ${row.taskName} 暂无执行日志`];
    console.log("日志数据赋值完成：", logDialog.logs);
  } catch (err) {
    // 打印「完整错误信息」（包括超时、404、500、网络错误）
    console.error("后端接口请求失败/超时，错误详情：", err);
    ElMessage.warning(
      `获取日志失败：${err.message || "未知错误"}，展示模拟日志`
    );
    const caseIds = row.caseId ? row.caseId.split(",") : [];
    logDialog.logs = [`开始执行 ${row.taskName}`];
    caseIds.forEach((cid) => {
      const cname = caseMap[cid] ? caseMap[cid].name : `用例-${cid}`;
      logDialog.logs.push(`执行用例 ${cname}：定位元素成功 → 操作成功`);
    });
    logDialog.logs.push(`任务执行结束`);
    console.log("模拟日志赋值完成：", logDialog.logs);
  }

  // 优化nextTick：把弹窗设置放在回调里，100%确保视图更新
  nextTick(() => {
    logDialog.visible = true;
    console.log("弹窗状态已设置为true：", logDialog.visible);
    console.log("===== 查看日志执行结束 =====");
  });
}

// 分页切换
function onTaskPageChange({ pageNum, pageSize }) {
  taskPage.pageNum = pageNum;
  taskPage.pageSize = pageSize;
  loadTasks();
}
</script>

<style scoped lang="less">
.task-schedule-root {
  padding: 20px;
}

.top-card,
.list-card {
  background: #fff;
  padding: 16px;
  border-radius: 8px;
  margin-bottom: 16px;
}

.task-form .el-form-item {
  margin-bottom: 12px;
}

.timed-row {
  display: flex;
  gap: 12px;
  align-items: center;
}

@media (max-width: 768px) {
  .timed-row {
    flex-direction: column;
    align-items: stretch;
  }
}

.list-actions {
  margin-bottom: 12px;
}

.case-names .el-tag {
  margin-right: 6px;
}

// 日志弹窗样式优化，加滚动条避免日志溢出
.log-body {
  max-height: 400px;
  overflow-y: auto;
  padding: 10px 0;
  ul {
    margin: 0;
    padding-left: 20px;
    li {
      line-height: 1.8;
      font-size: 14px;
    }
  }
}
</style>
