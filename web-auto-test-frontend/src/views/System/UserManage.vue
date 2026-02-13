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
        v-loading="loading"
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
// 导入用户管理接口
import { getUserList, addUser, updateUser, deleteUser } from "@/api/user";

// 加载状态
const loading = ref(false);
// 页面数据（替换模拟数据）
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

// 格式化日期（适配后端LocalDateTime）
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

// 加载用户列表（真实接口）
async function loadUserList() {
  loading.value = true;
  try {
    const params = {
      pageNum: page.pageNum,
      pageSize: page.pageSize,
    };
    const res = await getUserList(params);
    if (res.code === 200) {
      // 适配后端返回格式：list + total
      usersAll.value = res.data.list.map((item) => ({
        ...item,
        // 后端status是1/0，前端用enabled（true/false）
        enabled: item.status === 1,
      }));
      page.total = res.data.total;
      filtered.value = [...usersAll.value];
    } else {
      ElMessage.error("加载用户列表失败：" + res.msg);
    }
  } catch (err) {
    ElMessage.error("加载用户列表失败：" + err.message);
  } finally {
    loading.value = false;
  }
}

// 页面加载时调用
onMounted(() => {
  loadUserList();
});

// 搜索
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

// 重置搜索
function handleReset() {
  filters.username = "";
  filters.role = "all";
  filters.status = "all";
  filtered.value = [...usersAll.value];
  page.total = filtered.value.length;
  page.pageNum = 1;
}

// 分页数据
const pagedUsers = computed(() => {
  const start = (page.pageNum - 1) * page.pageSize;
  return filtered.value.slice(start, start + page.pageSize);
});

// 分页切换
function onPageChange({ pageNum, pageSize }) {
  page.pageNum = pageNum;
  page.pageSize = pageSize;
  loadUserList(); // 分页切换重新加载数据
}

// 新增用户
function openAdd() {
  dialog.mode = "add";
  dialog.form = {
    id: null,
    username: "",
    password: "",
    role: "user",
    enabled: true,
  };
  nextTick(() => {
    dialogVisible.value = true;
  });
}

// 编辑用户
function openEdit(row) {
  dialog.mode = "edit";
  dialog.form = {
    id: row.id,
    username: row.username,
    password: "",
    role: row.role,
    enabled: row.enabled,
  };
  nextTick(() => {
    dialogVisible.value = true;
  });
}

// 提交表单（新增/编辑）
async function submitForm() {
  if (!userFormRef.value) return;

  userFormRef.value.validate(async (valid) => {
    if (!valid) return;

    const formData = {
      username: dialog.form.username.trim(),
      password: dialog.form.password,
      role: dialog.form.role,
      status: dialog.form.enabled ? 1 : 0, // 转换为后端的1/0
    };

    try {
      if (dialog.mode === "add") {
        // 新增用户
        const res = await addUser(formData);
        if (res.code === 200) {
          ElMessage.success("新增用户成功");
          dialogVisible.value = false;
          loadUserList(); // 重新加载列表
        } else {
          ElMessage.error("新增失败：" + res.msg);
        }
      } else {
        // 编辑用户
        const res = await updateUser(dialog.form.id, formData);
        if (res.code === 200) {
          ElMessage.success("编辑用户成功");
          dialogVisible.value = false;
          loadUserList(); // 重新加载列表
        } else {
          ElMessage.error("编辑失败：" + res.msg);
        }
      }
    } catch (err) {
      ElMessage.error("操作失败：" + err.message);
    }
  });
}

// 删除用户
async function confirmDelete(row) {
  try {
    await ElMessageBox.confirm(`确认删除用户 ${row.username} 吗？`, "提示", {
      type: "warning",
    });
    const res = await deleteUser(row.id);
    if (res.code === 200) {
      ElMessage.success("删除用户成功");
      loadUserList(); // 重新加载列表
    } else {
      ElMessage.error("删除失败：" + res.msg);
    }
  } catch (err) {
    // 取消删除不提示
  }
}

// 重置密码（模拟）
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

// ========== 核心修改：状态切换调用后端接口 ==========
async function onToggleStatus(row) {
  // 1. 转换状态：前端enabled(true/false) → 后端status(1/0)
  const newStatus = row.enabled ? 1 : 0;
  try {
    // 2. 调用后端updateUser接口更新状态（仅传status字段即可）
    const res = await updateUser(row.id, { status: newStatus });
    if (res.code === 200) {
      ElMessage.success(`用户 ${row.username} 状态更新成功`);
      // 3. 重新加载列表，确保数据和数据库一致
      loadUserList();
    } else {
      ElMessage.error(`状态更新失败：${res.msg}`);
      // 4. 接口失败时，回滚开关状态（避免前端显示和数据库不一致）
      row.enabled = !row.enabled;
    }
  } catch (err) {
    ElMessage.error(`状态更新失败：${err.message}`);
    // 5. 网络错误时，回滚开关状态
    row.enabled = !row.enabled;
  }
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
