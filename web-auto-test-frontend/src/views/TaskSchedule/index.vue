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
        <el-table-column prop="name" label="任务名称" />
        <el-table-column prop="caseIds" label="关联用例">
          <template #default="{ row }">
            <div class="case-names">
              <el-tag v-for="cid in row.caseIds" :key="cid" type="info">{{
                caseMap[cid]?.name || cid
              }}</el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="mode" label="执行方式" width="120">
          <template #default="{ row }">{{
            row.mode === "immediate" ? "立即" : "定时"
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

    <el-dialog
      v-model:visible="logDialog.visible"
      width="600px"
      title="任务执行日志"
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
// 1. 新增导入 watch（解决watch未定义问题）
import { ref, reactive, computed, onMounted, watch } from "vue";
import CommonPagination from "@/components/CommonPagination.vue";
import { ElMessage } from "element-plus";

// 引用用例管理的模拟用例数据（若未生成，这里也做本地模拟）
const caseOptions = ref([]);
const caseMap = reactive({});

function genCaseOptions(n = 15) {
  const arr = [];
  for (let i = 1; i <= n; i++) {
    arr.push({ id: i, name: `用例-${i}` });
  }
  return arr;
}

// 构建映射
function initCases() {
  caseOptions.value = genCaseOptions(15);
  caseOptions.value.forEach((c) => {
    caseMap[c.id] = c;
  });
}

// 任务数据模拟
function genTasks(n = 10) {
  const modes = ["immediate", "timed"];
  const statuses = ["pending", "running", "finished", "failed"];
  const arr = [];
  for (let i = 1; i <= n; i++) {
    arr.push({
      id: i,
      name: `任务-${i}`,
      caseIds: [((i - 1) % 15) + 1],
      mode: modes[i % modes.length],
      status: statuses[i % statuses.length],
      createTime: Date.now() - i * 3600 * 1000,
    });
  }
  return arr;
}

const tasks = ref([]);

const taskForm = reactive({
  name: "",
  caseIds: [],
  mode: "immediate",
  date: null,
  time: null,
});

const taskPage = reactive({ pageNum: 1, pageSize: 5, total: 0 });

const logDialog = reactive({ visible: false, logs: [] });

onMounted(() => {
  initCases();
  tasks.value = genTasks(10);
  taskPage.total = tasks.value.length;
});

// 2. 新增watch：监听tasks变化，同步更新total（解决computed副作用报错）
watch(
  () => tasks.value,
  (newTasks) => {
    taskPage.total = newTasks.length;
  },
  { immediate: true }
);

// 3. 修复computed：只做纯计算，不修改外部变量
const pagedTasks = computed(() => {
  const start = (taskPage.pageNum - 1) * taskPage.pageSize;
  return tasks.value.slice(start, start + taskPage.pageSize);
});

function formatDate(ts) {
  const d = new Date(ts);
  const y = d.getFullYear();
  const m = String(d.getMonth() + 1).padStart(2, "0");
  const day = String(d.getDate()).padStart(2, "0");
  const hh = String(d.getHours()).padStart(2, "0");
  const mm = String(d.getMinutes()).padStart(2, "0");
  const ss = String(d.getSeconds()).padStart(2, "0");
  return `${y}-${m}-${day} ${hh}:${mm}:${ss}`;
}

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

function createTask() {
  if (!taskForm.name || !taskForm.name.trim()) {
    ElMessage.error("任务名称必填");
    return;
  }
  if (!taskForm.caseIds || !taskForm.caseIds.length) {
    ElMessage.error("请至少选择一个用例");
    return;
  }
  const newId = tasks.value.length
    ? Math.max(...tasks.value.map((t) => t.id)) + 1
    : 1;
  const createTime = Date.now();
  tasks.value.unshift({
    id: newId,
    name: taskForm.name.trim(),
    caseIds: [...taskForm.caseIds],
    mode: taskForm.mode,
    status: "pending",
    createTime,
  });
  ElMessage.success("创建任务成功（模拟）");
  // reset form
  taskForm.name = "";
  taskForm.caseIds = [];
  taskForm.mode = "immediate";
  taskForm.date = null;
  taskForm.time = null;
}

function refreshTasks() {
  ElMessage.info("已刷新（模拟）");
}

function runTask(row) {
  const idx = tasks.value.findIndex((t) => t.id === row.id);
  if (idx === -1) return;
  tasks.value[idx].status = "running";
  // 模拟执行，2s 后完成
  setTimeout(() => {
    tasks.value[idx].status = "finished";
    ElMessage.success(`任务 ${row.name} 执行完成（模拟）`);
  }, 2000);
}

function stopTask(row) {
  const idx = tasks.value.findIndex((t) => t.id === row.id);
  if (idx === -1) return;
  tasks.value[idx].status = "failed";
  ElMessage.warning(`任务 ${row.name} 已终止（模拟）`);
}

// 4. 修复未使用变量警告：删掉forEach里未使用的i参数
function viewLog(row) {
  // 模拟日志
  logDialog.logs = [`开始执行 ${row.name}`];
  row.caseIds.forEach((cid) => {
    // 删掉了未使用的i
    const cname = caseMap[cid] ? caseMap[cid].name : `用例-${cid}`;
    logDialog.logs.push(`执行用例 ${cname}：定位元素成功 → 操作成功`);
  });
  logDialog.logs.push(`任务执行结束`);
  logDialog.visible = true;
}

function onTaskPageChange({ pageNum, pageSize }) {
  taskPage.pageNum = pageNum;
  taskPage.pageSize = pageSize;
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
</style>
