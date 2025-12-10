<template>
  <div class="user-root">
    <div class="top-area">
      <el-form :model="filters" inline class="query-form">
        <el-form-item label="用户名">
          <el-input v-model="filters.username" placeholder="用户名"></el-input>
        </el-form-item>

        <el-form-item label="角色">
          <el-select v-model="filters.role" placeholder="请选择">
            <el-option label="全部" value="all" />
            <el-option label="管理员" value="admin" />
            <el-option label="普通用户" value="user" />
          </el-select>
        </el-form-item>

        <el-form-item label="状态">
          <el-select v-model="filters.status" placeholder="请选择">
            <el-option label="全部" value="all" />
            <el-option label="启用" value="enabled" />
            <el-option label="禁用" value="disabled" />
          </el-select>
        </el-form-item>

        <el-form-item>
          <el-button type="text" @click="handleReset">重置</el-button>
          <el-button type="primary" @click="handleSearch">查询</el-button>
        </el-form-item>
      </el-form>

      <div class="op-area">
        <el-button type="primary" @click="openAdd">新增用户</el-button>
      </div>
    </div>

    <el-card class="list-card">
      <el-table
        :data="pagedUsers"
        stripe
        style="width: 100%"
        row-key="id"
        :header-cell-style="headerStyle"
      >
        <el-table-column prop="id" label="用户ID" width="100" />
        <el-table-column prop="username" label="用户名" />
        <el-table-column prop="role" label="角色" width="140">
          <template #default="{ row }">
            <el-tag :type="row.role === 'admin' ? 'success' : 'info'">{{
              row.role === "admin" ? "管理员" : "普通用户"
            }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="120">
          <template #default="{ row }">
            <el-switch
              v-model="row.enabled"
              active-text="启用"
              inactive-text="禁用"
              @change="onToggleStatus(row)"
            />
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="200">
          <template #default="{ row }">{{
            formatDate(row.createTime)
          }}</template>
        </el-table-column>
        <el-table-column label="操作" width="220">
          <template #default="{ row }">
            <el-button type="text" @click="openEdit(row)">编辑</el-button>
            <el-button
              type="text"
              style="color: #f56c6c"
              @click="confirmDelete(row)"
              >删除</el-button
            >
            <el-button type="text" @click="confirmReset(row)"
              >重置密码</el-button
            >
          </template>
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

    <el-dialog
      v-model="dialogVisible"
      title="用户信息"
      width="500px"
      append-to-body
      destroy-on-close
    >
      <el-form
        :model="dialog.form"
        :rules="rules"
        ref="userFormRef"
        label-width="100px"
      >
        <el-form-item label="用户名" prop="username">
          <el-input v-model="dialog.form.username" maxlength="50" />
        </el-form-item>

        <el-form-item label="密码" prop="password">
          <el-input
            v-model="dialog.form.password"
            type="password"
            autocomplete="new-password"
            placeholder="新增时必填，编辑时可留空"
          />
        </el-form-item>

        <el-form-item label="角色" prop="role">
          <el-select v-model="dialog.form.role" placeholder="请选择">
            <el-option label="管理员" value="admin" />
            <el-option label="普通用户" value="user" />
          </el-select>
        </el-form-item>

        <el-form-item label="状态" prop="enabled">
          <el-switch
            v-model="dialog.form.enabled"
            active-text="启用"
            inactive-text="禁用"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitForm">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, nextTick } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import CommonPagination from "@/components/CommonPagination.vue";

// 页面数据
const usersAll = ref([]);
const filtered = ref([]);

const filters = reactive({ username: "", role: "all", status: "all" });

const page = reactive({ pageNum: 1, pageSize: 8, total: 0 });

const dialog = reactive({
  mode: "add",
  form: { id: null, username: "", password: "", role: "user", enabled: true },
});
const dialogVisible = ref(false);
const userFormRef = ref(null);
const rules = {
  username: [{ required: true, message: "用户名不能为空", trigger: "blur" }],
  password: [
    {
      validator: (rule, value, callback) => {
        if (dialog.mode === "add") {
          if (!value) return callback(new Error("密码长度不能少于6位"));
          if (value.length < 6)
            return callback(new Error("密码长度不能少于6位"));
        } else {
          if (value && value.length > 0 && value.length < 6)
            return callback(new Error("密码长度不能少于6位"));
        }
        callback();
      },
      trigger: "blur",
    },
  ],
  role: [{ required: true, message: "请选择角色", trigger: "change" }],
};

// 初始化模拟数据
function genUsers(n = 10) {
  const arr = [];
  for (let i = 1; i <= n; i++) {
    const role = i % 2 === 0 ? "admin" : "user";
    const enabled = i <= 8 ? true : false;
    const createTime =
      Date.now() - Math.floor(Math.random() * 7 * 24 * 3600 * 1000);
    arr.push({ id: i, username: `user${i}`, role, enabled, createTime });
  }
  return arr;
}

onMounted(() => {
  usersAll.value = genUsers(10);
  filtered.value = [...usersAll.value];
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

function handleSearch() {
  let res = [...usersAll.value];
  if (filters.username && filters.username.trim()) {
    const kw = filters.username.trim().toLowerCase();
    res = res.filter((u) => u.username.toLowerCase().includes(kw));
  }
  if (filters.role !== "all") {
    res = res.filter((u) => u.role === filters.role);
  }
  if (filters.status !== "all") {
    const want = filters.status === "enabled";
    res = res.filter((u) => u.enabled === want);
  }
  filtered.value = res;
  page.total = filtered.value.length;
  page.pageNum = 1;
}

function handleReset() {
  filters.username = "";
  filters.role = "all";
  filters.status = "all";
  filtered.value = [...usersAll.value];
  page.total = filtered.value.length;
  page.pageNum = 1;
}

const pagedUsers = computed(() => {
  const start = (page.pageNum - 1) * page.pageSize;
  return filtered.value.slice(start, start + page.pageSize);
});

function onPageChange({ pageNum, pageSize }) {
  page.pageNum = pageNum;
  page.pageSize = pageSize;
}

function openAdd() {
  console.log("=== 点击了【新增用户】按钮 ===");
  dialog.mode = "add";
  dialog.form = {
    id: null,
    username: "",
    password: "",
    role: "user",
    enabled: true,
  };
  // 确保在 DOM 更新后再展示弹窗，避免 ref 未初始化导致校验或聚焦问题
  nextTick(() => {
    console.log("=== 执行nextTick，设置dialogVisible为true ===");
    dialogVisible.value = true;
  });
}

function openEdit(row) {
  console.log("=== 点击了【编辑用户】按钮 ===", row); // 新增日志
  dialog.mode = "edit";
  dialog.form = {
    id: row.id,
    username: row.username,
    password: "",
    role: row.role,
    enabled: row.enabled,
  };
  // 等待 DOM 更新后再显示弹窗
  nextTick(() => {
    dialogVisible.value = true;
  });
}

function submitForm() {
  const doSave = () => {
    if (dialog.mode === "add") {
      const newId = usersAll.value.length
        ? Math.max(...usersAll.value.map((u) => u.id)) + 1
        : 1;
      usersAll.value.unshift({
        id: newId,
        username: dialog.form.username.trim(),
        role: dialog.form.role,
        enabled: !!dialog.form.enabled,
        createTime: Date.now(),
      });
      ElMessage.success("新增用户成功");
    } else {
      const idx = usersAll.value.findIndex((u) => u.id === dialog.form.id);
      if (idx !== -1) {
        usersAll.value[idx].username = dialog.form.username.trim();
        usersAll.value[idx].role = dialog.form.role;
        usersAll.value[idx].enabled = !!dialog.form.enabled;
      }
      ElMessage.success("编辑用户成功");
    }
    dialogVisible.value = false;
    // 重新应用筛选
    handleSearch();
  };

  if (userFormRef.value && typeof userFormRef.value.validate === "function") {
    userFormRef.value.validate((valid) => {
      if (!valid) return;
      doSave();
    });
  } else {
    // 回退校验，避免 ref 未初始化导致异常
    if (!dialog.form.username || !dialog.form.username.trim()) {
      ElMessage.error("用户名不能为空");
      return;
    }
    if (
      dialog.mode === "add" &&
      (!dialog.form.password || dialog.form.password.length < 6)
    ) {
      ElMessage.error("密码长度不能少于6位");
      return;
    }
    doSave();
  }
}

function confirmDelete(row) {
  ElMessageBox.confirm(`确认删除用户 ${row.username} 吗？`, "提示", {
    type: "warning",
  })
    .then(() => {
      usersAll.value = usersAll.value.filter((u) => u.id !== row.id);
      ElMessage.success("删除用户成功");
      handleSearch();
    })
    .catch(() => {});
}

function confirmReset(row) {
  ElMessageBox.confirm(
    `确认重置 ${row.username} 的密码为 123456 吗？`,
    "提示",
    { type: "warning" }
  )
    .then(() => {
      ElMessage.success("密码已重置为123456（模拟）");
    })
    .catch(() => {});
}

function onToggleStatus(row) {
  // row.enabled 已经被修改
  ElMessage.success(`用户 ${row.username} 状态更新成功`);
}

const headerStyle = { background: "#f9fafb", height: "48px" };
</script>

<style scoped lang="less">
.user-root {
  padding: 20px;
}

.top-area {
  display: flex;
  gap: 12px;
  align-items: center;
  flex-wrap: wrap;
  margin-bottom: 16px;
}

.query-form {
  background: #fff;
  padding: 12px;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.op-area {
  margin-left: auto;
  position: relative;
  z-index: 3;
}

.list-card {
  background: #fff;
  padding: 12px;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.pagination-wrap {
  margin-top: 12px;
  display: flex;
  justify-content: flex-end;
}

/* 弹窗样式 */
.el-dialog__body .el-form-item {
  margin-bottom: 12px;
}

@media (max-width: 768px) {
  .query-form {
    width: 100%;
  }
  .op-area {
    width: 100%;
    display: flex;
    justify-content: flex-end;
  }
}

@media (max-width: 600px) {
  .top-area {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
