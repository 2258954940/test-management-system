<template>
  <div class="task-schedule-root">
    <div class="top-card">
      <h3>创建任务</h3>
      <el-form :model="taskForm" label-width="100px" class="task-form">
        <el-form-item label="任务名称">
          <el-input v-model="taskForm.taskName" placeholder="请输入任务名称" />
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
          <el-radio-group v-model="taskForm.executeType">
            <el-radio label="immediate">立即执行</el-radio>
            <el-radio label="timing">定时执行</el-radio>
          </el-radio-group>
        </el-form-item>

        <!-- Cron表达式输入区域（纯Element Plus原生组件） -->
        <div v-if="taskForm.executeType === 'timing'" class="timed-row">
          <el-form-item label="定时规则">
            <!-- 输入框+右侧快捷选择下拉框 -->
            <div class="cron-input-wrapper">
              <el-input
                v-model="taskForm.cronExpression"
                placeholder="请输入Cron表达式，或从右侧选择常用规则"
                clearable
                size="default"
              >
                <template #append>
                  <el-select
                    v-model="selectedPreset"
                    placeholder="常用规则"
                    @change="applyCronPreset"
                    size="default"
                    style="width: 140px"
                  >
                    <el-option label="每分钟" value="0 * * * * ?" />
                    <el-option label="每5分钟" value="0 */5 * * * ?" />
                    <el-option label="每10分钟" value="0 */10 * * * ?" />
                    <el-option label="每30分钟" value="0 */30 * * * ?" />
                    <el-option label="每小时" value="0 0 * * * ?" />
                    <el-option label="每天0点" value="0 0 0 * * ?" />
                    <el-option label="每天12点" value="0 0 12 * * ?" />
                    <el-option label="每周一0点" value="0 0 0 ? * MON" />
                  </el-select>
                </template>
              </el-input>
            </div>
            <!-- 规则提示+帮助链接 -->
            <div class="cron-tip">
              <span class="tip-label">当前规则：</span>
              <el-tag
                :type="taskForm.cronExpression ? 'info' : 'warning'"
                size="small"
              >
                {{ taskForm.cronExpression || "未设置" }}
              </el-tag>
              <el-divider direction="vertical" />
              <el-link
                type="primary"
                :href="cronHelperUrl"
                target="_blank"
                underline
                style="font-size: 12px"
              >
                Cron表达式在线生成器
              </el-link>
            </div>
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
        border
      >
        <el-table-column prop="id" label="任务ID" width="100" align="center" />
        <el-table-column prop="taskName" label="任务名称" min-width="150" />
        <el-table-column prop="caseId" label="关联用例" min-width="200">
          <template #default="{ row }">
            <div class="case-names">
              <el-tag
                v-for="cid in row.caseId.split(',')"
                :key="cid"
                type="info"
                size="small"
              >
                {{ caseMap[cid]?.name || cid }}
              </el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column
          prop="execType"
          label="执行方式"
          width="120"
          align="center"
        >
          <template #default="{ row }">{{
            row.execType === "immediate" ? "立即" : "定时"
          }}</template>
        </el-table-column>
        <el-table-column
          prop="status"
          label="执行状态"
          width="120"
          align="center"
        >
          <template #default="{ row }">
            <el-tag :type="statusTag(row.status)" size="small">{{
              statusLabel(row.status)
            }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column
          prop="createTime"
          label="创建时间"
          width="200"
          align="center"
        >
          <template #default="{ row }">{{
            formatDate(row.createTime)
          }}</template>
        </el-table-column>
        <el-table-column label="操作" width="300" align="center">
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
            <!-- 删除按钮 -->
            <el-button
              type="danger"
              size="small"
              @click="deleteTask(row)"
              style="margin-left: 8px"
              icon="el-icon-delete"
              >删除</el-button
            >
          </template>
        </el-table-column>
      </el-table>

      <common-pagination
        :total="taskPage.total"
        :pageNum="taskPage.pageNum"
        :pageSize="taskPage.pageSize"
        @onPageChange="onTaskPageChange"
        style="margin-top: 16px; text-align: right"
      />
    </div>

    <!-- 日志弹窗（修正标签嵌套） -->
    <el-dialog
      v-model="logDialog.visible"
      width="800px"
      title="任务执行日志"
      append-to-body
      align-center
      :close-on-click-modal="false"
      :z-index="9999"
    >
      <div class="log-body">
        <ul>
          <li v-for="(l, idx) in logDialog.logs" :key="idx" class="log-item">
            {{ l }}
          </li>
        </ul>
      </div>
      <template #footer>
        <el-button @click="logDialog.visible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch, nextTick } from "vue";
import CommonPagination from "@/components/CommonPagination.vue";
import { ElMessage, ElMessageBox } from "element-plus";
import {
  getTaskList,
  createTask as apiCreateTask,
  runTask as apiRunTask,
  stopTask as apiStopTask,
  getTaskLog,
  deleteTask as apiDeleteTask, // 导入删除接口
} from "@/api/task";
import { getCaseList } from "@/api/case";

// Cron快捷选择相关
const selectedPreset = ref("");
const cronHelperUrl = ref("https://cron.qqe2.com/");

// 应用快捷选择的Cron规则
const applyCronPreset = (value) => {
  taskForm.cronExpression = value;
  selectedPreset.value = "";
};

// 用例列表
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

// 任务表单 & 列表
const taskForm = reactive({
  taskName: "",
  caseIds: [],
  executeType: "immediate",
  cronExpression: "",
});
const tasks = ref([]);
const taskPage = reactive({ pageNum: 1, pageSize: 5, total: 0 });
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
        execType: i % 2 === 0 ? "immediate" : "timing",
        status: i % 4 === 0 ? "finished" : "pending",
        createTime: new Date(Date.now() - i * 3600 * 1000).toISOString(),
      });
    }
    tasks.value = mockTasks;
    taskPage.total = mockTasks.length;
  }
}

// 分页计算
const pagedTasks = computed(() => {
  const start = (taskPage.pageNum - 1) * taskPage.pageSize;
  return tasks.value.slice(start, start + taskPage.pageSize);
});

// 日期格式化
function formatDate(ts) {
  if (!ts) return "-";
  try {
    const d = new Date(ts);
    const y = d.getFullYear();
    const m = String(d.getMonth() + 1).padStart(2, "0");
    const day = String(d.getDate()).padStart(2, "0");
    const hh = String(d.getHours()).padStart(2, "0");
    const mm = String(d.getMinutes()).padStart(2, "0");
    const ss = String(d.getSeconds()).padStart(2, "0");
    return `${y}-${m}-${day} ${hh}:${mm}:${ss}`;
  } catch (e) {
    return "-";
  }
}

// 状态映射
function statusLabel(s) {
  const statusMap = {
    pending: "待执行",
    running: "执行中",
    finished: "已完成",
    failed: "失败",
  };
  return statusMap[s] || s;
}

function statusTag(s) {
  const tagMap = {
    pending: "warning",
    running: "info",
    finished: "success",
    failed: "danger",
  };
  return tagMap[s] || "";
}

// 创建任务
// 创建任务
// 创建任务
async function createTask() {
  // 表单校验
  if (!taskForm.taskName.trim()) {
    ElMessage.error("任务名称不能为空！");
    return;
  }
  if (!taskForm.caseIds.length) {
    ElMessage.error("请至少选择一个关联用例！");
    return;
  }
  if (taskForm.executeType === "timing" && !taskForm.cronExpression.trim()) {
    ElMessage.error("定时执行需填写有效的Cron表达式！");
    return;
  }

  try {
    // 1. 提交创建任务，拿到返回的新任务ID
    const createRes = await apiCreateTask({
      taskName: taskForm.taskName.trim(),
      caseIds: taskForm.caseIds.join(","),
      executeType: taskForm.executeType,
      cronExpression: taskForm.cronExpression.trim(),
    });
    const newTaskId = createRes.data?.id;

    ElMessage.success("任务创建成功！");

    // 2. 立即执行：必须拿到 newTaskId 才调用执行接口
    if (taskForm.executeType === "immediate" && newTaskId) {
      await apiRunTask(newTaskId);
      ElMessage.success("立即执行任务已触发！");
    } else if (taskForm.executeType === "immediate" && !newTaskId) {
      ElMessage.warning("任务创建成功，但未获取到任务ID，无法自动执行");
    }

    // 3. 重置表单 + 刷新列表（加延迟确保后端数据已落库）
    resetTaskForm();
    setTimeout(async () => {
      await loadTasks();
    }, 500);
  } catch (err) {
    ElMessage.error(`创建任务失败：${err.message || "未知错误"}`);
  }
}
// 重置任务表单
function resetTaskForm() {
  taskForm.taskName = "";
  taskForm.caseIds = [];
  taskForm.executeType = "immediate";
  taskForm.cronExpression = "";
  selectedPreset.value = "";
}

// 刷新任务列表
async function refreshTasks() {
  await loadTasks();
  ElMessage.success("任务列表已刷新！");
}

// 执行任务
async function runTask(row) {
  try {
    await apiRunTask(row.id);
    ElMessage.success(`任务 ${row.taskName} 开始执行！`);
    setTimeout(() => loadTasks(), 1000);
  } catch (err) {
    ElMessage.error(`执行任务失败：${err.message || "未知错误"}`);
  }
}

// 终止任务
async function stopTask(row) {
  try {
    await apiStopTask(row.id);
    ElMessage.warning(`任务 ${row.taskName} 已终止！`);
    setTimeout(() => loadTasks(), 1000);
  } catch (err) {
    ElMessage.error(`终止任务失败：${err.message || "未知错误"}`);
  }
}

// 查看日志
async function viewLog(row) {
  if (!row || !row.id) {
    ElMessage.warning("任务ID无效，无法获取日志！");
    return;
  }

  logDialog.logs = []; // 清空原有日志
  try {
    const res = await Promise.race([
      getTaskLog(row.id),
      new Promise((_, reject) => {
        setTimeout(() => reject(new Error("请求超时：5秒未响应")), 5000);
      }),
    ]);
    logDialog.logs = res.data?.logs?.length
      ? res.data.logs
      : [`任务 ${row.taskName} 暂无执行日志`];
  } catch (err) {
    ElMessage.warning(`获取日志失败：${err.message}，展示模拟日志`);
    // 生成模拟日志
    const caseIds = row.caseId ? row.caseId.split(",") : [];
    logDialog.logs = [
      `[${new Date().toLocaleString()}] 开始执行任务：${row.taskName}`,
    ];
    caseIds.forEach((cid, index) => {
      const cname = caseMap[cid]?.name || `用例-${cid}`;
      logDialog.logs.push(
        `[${new Date().toLocaleString()}] 执行用例 ${index + 1}/${
          caseIds.length
        }：${cname} → 操作成功`
      );
    });
    logDialog.logs.push(
      `[${new Date().toLocaleString()}] 任务 ${row.taskName} 执行结束`
    );
  }
  nextTick(() => {
    logDialog.visible = true;
  });
}

// 删除任务
async function deleteTask(row) {
  // 校验：执行中的任务不能删除
  if (row.status === "running") {
    ElMessage.warning("执行中的任务无法删除，请先终止任务！");
    return;
  }

  // 确认弹窗
  try {
    await ElMessageBox.confirm(
      `确定要删除任务【${row.taskName}】吗？删除后不可恢复！`,
      "删除确认",
      {
        confirmButtonText: "确认删除",
        cancelButtonText: "取消",
        type: "warning",
        draggable: true,
      }
    );

    // 调用删除接口
    await apiDeleteTask(row.id);
    ElMessage.success(`任务 ${row.taskName} 删除成功！`);

    // 刷新列表
    await loadTasks();
  } catch (err) {
    // 区分“用户取消”和“接口报错”
    if (err !== "cancel") {
      ElMessage.error(`删除任务失败：${err.message || "未知错误"}`);
    }
  }
}

// 分页切换
function onTaskPageChange({ pageNum, pageSize }) {
  taskPage.pageNum = pageNum;
  taskPage.pageSize = pageSize;
  loadTasks();
}

// 初始化
onMounted(async () => {
  await initCases();
  await loadTasks();
});

// 监听任务列表变化，更新分页总数
watch(
  () => tasks.value,
  (newTasks) => {
    taskPage.total = newTasks.length;
  },
  { immediate: true }
);
</script>

<style scoped lang="less">
.task-schedule-root {
  padding: 20px;
  background-color: #f5f7fa;
  min-height: 100vh;
}

.top-card,
.list-card {
  background: #fff;
  padding: 20px;
  border-radius: 8px;
  margin-bottom: 16px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.04);
}

.top-card h3 {
  margin: 0 0 16px 0;
  font-size: 18px;
  color: #1f2937;
  font-weight: 600;
}

.task-form .el-form-item {
  margin-bottom: 16px;
}

.timed-row {
  margin-top: 8px;
}

// Cron输入区域样式优化
.cron-input-wrapper {
  width: 100%;
  :deep(.el-input) {
    width: 100%;
    :deep(.el-input-group__append) {
      padding: 0;
      border-left: none;
      background-color: #f9fafb;
      :deep(.el-select) {
        width: 100%;
      }
    }
  }
}

.cron-tip {
  margin-top: 10px;
  font-size: 12px;
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;

  .tip-label {
    color: #666;
    white-space: nowrap;
  }

  .el-tag {
    flex-shrink: 0;
  }

  .el-link {
    margin-left: auto;
    white-space: nowrap;
  }
}

// 列表区域样式优化
.list-actions {
  margin-bottom: 16px;
  display: flex;
  justify-content: flex-end;
}

.case-names {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}

.task-table {
  --el-table-header-text-color: #666;
  --el-table-row-hover-bg-color: #f9fafb;
}

// 日志弹窗样式优化
.log-body {
  max-height: 500px;
  overflow-y: auto;
  padding: 10px 0;
  background-color: #f9fafb;
  border-radius: 4px;
  margin: 8px 0;

  ul {
    margin: 0;
    padding-left: 20px;
  }

  .log-item {
    line-height: 1.8;
    font-size: 14px;
    color: #333;
    padding: 2px 0;
    border-bottom: 1px dashed #eee;
    &:last-child {
      border-bottom: none;
    }
  }
}

// 响应式适配
@media (max-width: 768px) {
  .task-schedule-root {
    padding: 10px;
  }

  .top-card,
  .list-card {
    padding: 16px;
  }

  .timed-row {
    flex-direction: column;
    align-items: stretch;
  }

  .cron-tip {
    flex-direction: column;
    align-items: flex-start;
    gap: 4px;

    .el-link {
      margin-left: 0;
      margin-top: 4px;
    }
  }

  // 移动端操作列适配
  .task-table :deep(.el-table-column--operation) {
    .el-button {
      margin-left: 4px !important;
      padding: 4px 8px;
      font-size: 12px;
    }
  }
}
</style>
