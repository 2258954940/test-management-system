<template>
  <div class="case-manage-root">
    <div class="top-area">
      <common-query-form
        :fields="queryFields"
        @onSearch="handleSearch"
        @onReset="handleReset"
      />
      <div class="action-area">
        <el-button type="primary" @click="openAdd">新增用例</el-button>
      </div>
    </div>

    <div ref="tableWrap" class="table-wrap">
      <el-table
        :data="displayList"
        stripe
        style="width: 100%"
        class="case-table"
        row-key="id"
        :loading="loading"
      >
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="name" label="用例名称" min-width="160" />
        <el-table-column
          prop="url"
          label="测试URL"
          min-width="200"
          show-overflow-tooltip
        />
        <el-table-column prop="needLogin" label="是否需要登录" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="row.needLogin ? 'success' : 'info'">
              {{ row.needLogin ? "是" : "否" }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column
          prop="siteCode"
          label="关联网站"
          width="120"
          show-overflow-tooltip
        >
          <template #default="{ row }">
            {{ getSiteNameByCode(row.siteCode) || "-" }}
          </template>
        </el-table-column>
        <el-table-column prop="creator" label="创建人" width="120" />
        <el-table-column prop="createTime" label="创建时间" width="180">
          <template #default="{ row }">{{
            formatDate(row.createTime)
          }}</template>
        </el-table-column>
        <el-table-column prop="updateTime" label="更新时间" width="180">
          <template #default="{ row }">{{
            formatDate(row.updateTime)
          }}</template>
        </el-table-column>
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <el-button type="text" size="small" @click="openEdit(row)"
              >编辑</el-button
            >
            <el-button type="text" size="small" @click="handleDelete(row.id)"
              >删除</el-button
            >
            <el-button type="text" size="small" @click="openRunCaseDialog(row)"
              >执行</el-button
            >
          </template>
        </el-table-column>
      </el-table>
    </div>

    <common-pagination
      :total="pageParams.total"
      :pageNum="pageParams.pageNum"
      :pageSize="pageParams.pageSize"
      :disabled="loading"
      @onPageChange="handlePageChange"
    />

    <!-- 新增/编辑用例弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="560px"
      style="z-index: 9999"
    >
      <el-form
        ref="formRef"
        :model="formModel"
        :rules="formRules"
        label-width="100px"
        class="dialog-form"
      >
        <el-form-item label="用例名称" prop="name">
          <el-input v-model="formModel.name" placeholder="请输入用例名称" />
        </el-form-item>
        <el-form-item label="测试URL" prop="url">
          <el-input v-model="formModel.url" placeholder="请输入测试URL" />
        </el-form-item>
        <el-form-item label="步骤" prop="description">
          <el-input
            v-model="formModel.description"
            type="textarea"
            :rows="4"
            placeholder="请输入执行步骤（支持多行）"
          />
        </el-form-item>
        <el-form-item label="输入数据" prop="inputData">
          <el-input
            v-model="formModel.inputData"
            placeholder="请输入要在输入框中填写的内容（如：自动化测试）"
          />
        </el-form-item>
        <el-form-item label="预期结果" prop="expectedResult">
          <el-input
            v-model="formModel.expectedResult"
            placeholder="请输入预期结果（如：显示“自动化测试”搜索结果）"
          />
        </el-form-item>
        <el-form-item label="创建人" prop="creator">
          <el-input
            v-model="formModel.creator"
            placeholder="请输入创建人"
            disabled
          />
        </el-form-item>
        <!-- 新增：登录相关配置 -->
        <el-form-item label="是否需要登录" prop="needLogin">
          <el-switch
            v-model="formModel.needLogin"
            active-text="是"
            inactive-text="否"
          />
        </el-form-item>
        <el-form-item
          label="关联测试网站"
          prop="siteCode"
          v-if="formModel.needLogin"
        >
          <el-select v-model="formModel.siteCode" placeholder="请选择测试网站">
            <el-option
              v-for="config in siteConfigs"
              :key="config.siteCode"
              :label="config.siteName"
              :value="config.siteCode"
            ></el-option>
          </el-select>
        </el-form-item>
        <!-- 新增：测试账号输入框 -->
        <el-form-item
          label="测试账号"
          prop="testAccount"
          v-if="formModel.needLogin"
        >
          <el-input
            v-model="formModel.testAccount"
            placeholder="请输入测试账号（执行时自动填充）"
          />
        </el-form-item>
        <!-- 新增：测试密码输入框 -->
        <el-form-item
          label="测试密码"
          prop="testPassword"
          v-if="formModel.needLogin"
        >
          <el-input
            v-model="formModel.testPassword"
            type="password"
            placeholder="请输入测试密码（执行时自动填充）"
            show-password
          />
        </el-form-item>
        <!-- 断言配置字段 -->
        <el-form-item label="断言类型" prop="assertType">
          <el-select
            v-model="formModel.assertType"
            placeholder="请选择断言类型"
          >
            <el-option label="文本断言" value="TEXT" />
            <el-option label="元素存在断言" value="EXISTS" />
            <el-option label="通用搜索断言" value="SEARCH" />
          </el-select>
        </el-form-item>
        <el-form-item label="断言定位类型" prop="assertLocatorType">
          <el-select
            v-model="formModel.assertLocatorType"
            placeholder="请选择定位类型"
          >
            <el-option label="ID" value="id" />
            <el-option label="XPath" value="xpath" />
            <el-option label="Name" value="name" />
            <el-option label="CSS选择器" value="css" />
          </el-select>
        </el-form-item>
        <el-form-item label="断言定位值" prop="assertLocatorValue">
          <el-input
            v-model="formModel.assertLocatorValue"
            placeholder="请输入断言元素的定位值（百度搜索示例：//*[contains(text(),'自动化测试')]）"
          />
        </el-form-item>
        <el-form-item label="断言预期值" prop="assertExpectedValue">
          <el-input
            v-model="formModel.assertExpectedValue"
            placeholder="请输入断言预期结果（文本断言必填）"
          />
        </el-form-item>
        <el-form-item label="关联元素">
          <el-select
            v-model="formModel.elementIds"
            multiple
            placeholder="请选择要关联的元素（多选）"
            style="width: 100%"
          >
            <el-option
              v-for="item in elementList"
              :key="item.id"
              :label="item.elementName"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button
          type="primary"
          :loading="submitLoading"
          @click="handleSubmit"
        >
          确定
        </el-button>
      </template>
    </el-dialog>

    <!-- 执行用例弹窗（自动回填保存的账号密码） -->
    <el-dialog
      v-model="runCaseDialogVisible"
      title="执行用例"
      width="500px"
      style="z-index: 9999"
    >
      <el-form
        ref="runCaseFormRef"
        :model="runCaseForm"
        :rules="runCaseRules"
        label-width="100px"
      >
        <el-form-item label="用例ID" prop="caseId">
          <el-input v-model="runCaseForm.caseId" disabled />
        </el-form-item>
        <!-- 新增：浏览器选择，默认Edge -->
        <el-form-item label="浏览器" prop="browserType">
          <el-select
            v-model="runCaseForm.browserType"
            placeholder="请选择浏览器"
          >
            <el-option label="Edge" value="edge" />
            <el-option label="Chrome" value="chrome" />
            <el-option label="Firefox" value="firefox" />
          </el-select>
        </el-form-item>
        <!-- 仅当用例需要登录时显示账号密码（自动回填） -->
        <el-form-item
          label="测试账号"
          prop="username"
          v-if="currentCase.needLogin"
        >
          <el-input
            v-model="runCaseForm.username"
            placeholder="留空则使用用例中保存的账号"
          />
        </el-form-item>
        <el-form-item
          label="测试密码"
          prop="password"
          v-if="currentCase.needLogin"
        >
          <el-input
            v-model="runCaseForm.password"
            type="password"
            placeholder="留空则使用用例中保存的密码"
            show-password
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="runCaseDialogVisible = false">取消</el-button>
        <el-button
          type="primary"
          :loading="runCaseLoading"
          @click="handleRunCaseSubmit"
          :disabled="runCaseLoading"
        >
          执行用例
        </el-button>
      </template>
    </el-dialog>

    <!-- 验证详情弹窗 -->
    <el-dialog
      v-model="verifyDetailDialogVisible"
      title="验证详情"
      width="600px"
    >
      <el-table :data="verifyDetailList" border style="width: 100%">
        <el-table-column prop="type" label="验证类型" width="120" />
        <el-table-column prop="result" label="验证结果" />
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch, onUnmounted } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import CommonQueryForm from "@/components/CommonQueryForm.vue";
import CommonPagination from "@/components/CommonPagination.vue";
import {
  addCase,
  deleteCase,
  getCaseList,
  updateCase,
  runCase,
  getCaseById, // 新增：获取单个用例详情
} from "@/api/case";
import { useUserStore } from "@/store";
import { getElementList } from "@/api/element";
import { getSiteConfigs } from "@/api/siteConfig";

const tableWrap = ref(null);
let resizeObserver = null;

const queryFields = [
  {
    label: "用例名称",
    key: "name",
    type: "input",
    placeholder: "按名称搜索",
  },
  {
    label: "创建人",
    key: "creator",
    type: "input",
    placeholder: "按创建人搜索",
  },
];

const caseList = ref([]);
const loading = ref(false);
const submitLoading = ref(false);
const elementList = ref([]);
const siteConfigs = ref([]);

// 当前选中的用例（用于执行弹窗）
const currentCase = reactive({
  id: "",
  needLogin: false,
  siteCode: "",
  assertExpectedValue: "",
  testAccount: "", // 新增：保存的测试账号
  testPassword: "", // 新增：保存的测试密码
});

// 执行用例相关
const runCaseDialogVisible = ref(false);
const runCaseLoading = ref(false);
const runCaseFormRef = ref(null);
const runCaseForm = reactive({
  caseId: "",
  username: "",
  password: "",
  browserType: "edge", // 新增：浏览器选择，默认Edge
});

// 执行用例表单规则（修改：账号密码改为非必填）
const runCaseRules = reactive({
  browserType: [
    { required: true, message: "请选择执行用例的浏览器", trigger: "change" },
  ],
  username: [
    {
      required: false, // 修改：非必填
      message: "请输入测试账号",
      trigger: "blur",
    },
  ],
  password: [
    {
      required: false, // 修改：非必填
      message: "请输入测试密码",
      trigger: "blur",
    },
  ],
});

// 验证详情
const verifyDetailDialogVisible = ref(false);
const verifyDetailList = ref([]);

const filterParams = reactive({
  name: "",
  creator: "",
});

const pageParams = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0,
});

const filteredList = computed(() => {
  const nameKw = (filterParams.name || "").trim().toLowerCase();
  const creatorKw = (filterParams.creator || "").trim().toLowerCase();
  return caseList.value.filter((item) => {
    const nameMatch = nameKw
      ? String(item.name || "")
          .toLowerCase()
          .includes(nameKw)
      : true;
    const creatorMatch = creatorKw
      ? String(item.creator || "")
          .toLowerCase()
          .includes(creatorKw)
      : true;
    return nameMatch && creatorMatch;
  });
});

watch(
  () => filteredList.value.length,
  (len) => {
    pageParams.total = len;
    const maxPage = Math.max(1, Math.ceil((len || 1) / pageParams.pageSize));
    if (pageParams.pageNum > maxPage) {
      pageParams.pageNum = maxPage;
    }
  },
  { immediate: true }
);

const displayList = computed(() => {
  const start = (pageParams.pageNum - 1) * pageParams.pageSize;
  return filteredList.value.slice(start, start + pageParams.pageSize);
});

// 根据网站编码获取网站名称
const getSiteNameByCode = (siteCode) => {
  if (!siteCode) return "";
  const site = siteConfigs.value.find((item) => item.siteCode === siteCode);
  return site?.siteName || siteCode;
};

// 获取当前登录用户（优先级：store > localStorage）
function getDefaultCreator() {
  const store = useUserStore();
  if (store?.username) return store.username;
  return localStorage.getItem("username") || "未知用户";
}

async function fetchCaseList() {
  loading.value = true;
  try {
    const res = await getCaseList();
    const list = Array.isArray(res?.list)
      ? res.list
      : Array.isArray(res)
      ? res
      : Array.isArray(res?.data)
      ? res.data
      : [];
    caseList.value = list;
    pageParams.total = filteredList.value.length;
  } catch (err) {
    caseList.value = [];
    pageParams.total = 0;
    ElMessage.error("获取用例列表失败");
  } finally {
    loading.value = false;
  }
}

async function fetchElementList() {
  try {
    const res = await getElementList();
    elementList.value = res.data || res || [];
  } catch (err) {
    const errMsg =
      err?.response?.data?.msg ||
      err?.response?.statusText ||
      "加载元素列表失败";
    ElMessage.error(`加载元素列表失败：${errMsg}`);
    elementList.value = [];
  }
}

async function fetchSiteConfigs() {
  try {
    const res = await getSiteConfigs();
    siteConfigs.value = res.data || res || [];
  } catch (err) {
    const errMsg =
      err?.response?.data?.msg ||
      err?.response?.statusText ||
      "加载网站配置失败";
    ElMessage.error(`加载网站配置失败：${errMsg}`);
    siteConfigs.value = [];
  }
}

function handleSearch(params) {
  filterParams.name = params.name || "";
  filterParams.creator = params.creator || "";
  pageParams.pageNum = 1;
  fetchCaseList();
}

function handleReset() {
  filterParams.name = "";
  filterParams.creator = "";
  pageParams.pageNum = 1;
  fetchCaseList();
}

function handlePageChange({ pageNum, pageSize }) {
  pageParams.pageNum = pageNum;
  pageParams.pageSize = pageSize;
}

const dialogVisible = ref(false);
const isEdit = ref(false);
const formRef = ref();
const formModel = reactive({
  id: null,
  name: "",
  url: "",
  description: "",
  creator: "",
  elementIds: [],
  inputData: "",
  expectedResult: "",
  // 断言配置字段
  assertType: "TEXT",
  assertLocatorType: "id",
  assertLocatorValue: "",
  assertExpectedValue: "",
  // 登录相关配置
  needLogin: false,
  siteCode: "",
  testAccount: "", // 新增：测试账号
  testPassword: "", // 新增：测试密码
});

const formRules = {
  name: [{ required: true, message: "请输入用例名称", trigger: "blur" }],
  url: [{ required: true, message: "请输入测试URL", trigger: "blur" }],
  description: [{ required: true, message: "请输入用例步骤", trigger: "blur" }],
  creator: [{ required: true, message: "创建人不能为空", trigger: "blur" }],
  inputData: [
    { required: true, message: "请输入要填写的内容", trigger: "blur" },
  ],
  expectedResult: [
    { required: true, message: "请输入预期结果", trigger: "blur" },
  ],
  assertType: [
    { required: true, message: "请选择断言类型", trigger: "change" },
  ],
  assertLocatorType: [
    { required: true, message: "请选择断言定位类型", trigger: "change" },
  ],
  assertLocatorValue: [
    { required: true, message: "请输入断言定位值", trigger: "blur" },
  ],
  assertExpectedValue: [
    {
      required: false,
      message: "文本断言需填写预期值",
      trigger: "blur",
      validator: (rule, value, callback) => {
        if (formModel.assertType === "TEXT" && !value) {
          callback(new Error(rule.message));
        } else {
          callback();
        }
      },
    },
  ],
  siteCode: [
    {
      required: true,
      message: "请选择关联测试网站",
      trigger: "change",
      validator: (rule, value, callback) => {
        if (formModel.needLogin && !value) {
          callback(new Error(rule.message));
        } else {
          callback();
        }
      },
    },
  ],
  testAccount: [
    {
      required: false, // 非必填（但建议填写）
      message: "请输入测试账号",
      trigger: "blur",
      validator: (rule, value, callback) => {
        if (formModel.needLogin && !value) {
          ElMessage.warning("建议填写测试账号，执行时需手动输入");
        }
        callback();
      },
    },
  ],
  testPassword: [
    {
      required: false, // 非必填（但建议填写）
      message: "请输入测试密码",
      trigger: "blur",
      validator: (rule, value, callback) => {
        if (formModel.needLogin && !value) {
          ElMessage.warning("建议填写测试密码，执行时需手动输入");
        }
        callback();
      },
    },
  ],
};

const dialogTitle = computed(() => (isEdit.value ? "编辑用例" : "新增用例"));

function openAdd() {
  isEdit.value = false;
  formModel.id = null;
  formModel.name = "";
  formModel.url = "";
  formModel.description = "";
  formModel.creator = getDefaultCreator(); // 核心修改：自动填充当前登录用户
  formModel.inputData = "";
  formModel.expectedResult = "";
  formModel.elementIds = [];
  // 初始化断言字段
  formModel.assertType = "TEXT";
  formModel.assertLocatorType = "id";
  formModel.assertLocatorValue = "";
  formModel.assertExpectedValue = "";
  // 初始化登录配置
  formModel.needLogin = false;
  formModel.siteCode = "";
  formModel.testAccount = ""; // 初始化测试账号
  formModel.testPassword = ""; // 初始化测试密码
  dialogVisible.value = true;
  setTimeout(() => {
    if (formRef.value) formRef.value.clearValidate();
  }, 100);
}

function openEdit(row) {
  isEdit.value = true;
  formModel.id = row.id;
  formModel.name = row.name || "";
  formModel.url = row.url || "";
  formModel.description = row.description || "";
  formModel.creator = row.creator || getDefaultCreator(); // 核心修改：用原创建人，无则取当前用户
  formModel.inputData = row.inputData || "";
  formModel.expectedResult = row.expectedResult || "";
  formModel.elementIds = row.elementIds
    ? row.elementIds.split(",").map((id) => Number(id))
    : [];
  // 回显断言字段
  formModel.assertType = row.assertType || "TEXT";
  formModel.assertLocatorType = row.assertLocatorType || "id";
  formModel.assertLocatorValue = row.assertLocatorValue || "";
  formModel.assertExpectedValue = row.assertExpectedValue || "";
  // 回显登录配置
  formModel.needLogin = row.needLogin || false;
  formModel.siteCode = row.siteCode || "";
  formModel.testAccount = row.testAccount || ""; // 回显测试账号
  formModel.testPassword = row.testPassword || ""; // 回显测试密码
  dialogVisible.value = true;
  setTimeout(() => {
    if (formRef.value) formRef.value.clearValidate();
  }, 100);
}

async function openRunCaseDialog(row) {
  // 保存当前选中的用例信息
  currentCase.id = row.id;
  currentCase.needLogin = row.needLogin || false;
  currentCase.siteCode = row.siteCode || "";
  currentCase.assertExpectedValue = row.assertExpectedValue || "";

  // 获取用例详情，拿到保存的账号密码
  try {
    const res = await getCaseById(row.id);
    if (res.data) {
      currentCase.testAccount = res.data.testAccount || "";
      currentCase.testPassword = res.data.testPassword || "";
    }
  } catch (err) {
    // 优化错误提示，只打印日志，不弹警告（避免用户困惑）
    console.log("获取用例详情失败，使用列表数据：", err);
    currentCase.testAccount = row.testAccount || "";
    currentCase.testPassword = row.testPassword || "";
  }

  // 初始化执行表单（自动回填保存的账号密码）
  runCaseForm.caseId = row.id;
  runCaseForm.username = currentCase.testAccount; // 自动回填账号
  runCaseForm.password = currentCase.testPassword; // 自动回填密码
  runCaseForm.browserType = "edge"; // 默认Edge

  runCaseDialogVisible.value = true;
  setTimeout(() => {
    if (runCaseFormRef.value) runCaseFormRef.value.clearValidate();
  }, 100);
}

async function handleRunCaseSubmit() {
  if (!runCaseFormRef.value) return;
  try {
    await runCaseFormRef.value.validate();
  } catch (err) {
    ElMessage.error("请填写所有必填字段");
    return;
  }

  runCaseLoading.value = true;
  try {
    ElMessage.info(`正在执行用例ID: ${runCaseForm.caseId}`);

    // 构造执行请求体（仅传必要字段）
    const requestData = {
      caseId: runCaseForm.caseId,
      username: runCaseForm.username || currentCase.testAccount, // 优先用手动输入，没有则用保存的
      password: runCaseForm.password || currentCase.testPassword, // 优先用手动输入，没有则用保存的
      browserType: runCaseForm.browserType,
    };

    const res = await runCase(requestData);
    console.log("执行结果：", res);

    // 解析验证详情
    if (res.data?.verifyDetail) {
      try {
        const detailMap = JSON.parse(res.data.verifyDetail);
        verifyDetailList.value = Object.entries(detailMap).map(
          ([type, result]) => ({
            type,
            result,
          })
        );
        verifyDetailDialogVisible.value = true;
      } catch (e) {
        verifyDetailList.value = [
          { type: "解析失败", result: "无法解析验证详情" },
        ];
        verifyDetailDialogVisible.value = true;
      }
    }

    // 状态提示
    const execStatus = res.data?.status || "UNKNOWN";
    if (execStatus === "SUCCESS" || execStatus === "PASS") {
      ElMessage.success(`用例执行成功 ✅，状态：${execStatus}`);
    } else if (execStatus === "FAILED") {
      ElMessage.error(
        `用例执行失败 ❌，状态：${execStatus}（可查看后端日志或截图）`
      );
    } else if (execStatus === "UNKNOWN") {
      ElMessage.warning(`用例执行完成（未验证结果），状态：${execStatus}`);
    } else {
      ElMessage.info(`用例执行完成，状态：${execStatus}`);
    }

    runCaseDialogVisible.value = false;
  } catch (err) {
    const errMsg = err?.response?.data?.msg || err?.message || "执行异常";
    ElMessage.error(`执行失败：${errMsg}`);
  } finally {
    runCaseLoading.value = false;
  }
}

async function handleSubmit() {
  if (!formRef.value) return;
  try {
    await formRef.value.validate();
  } catch (err) {
    ElMessage.error("请填写所有必填字段（用例名称/URL/步骤/创建人）");
    return;
  }

  submitLoading.value = true;
  try {
    const submitData = {
      ...formModel,
      elementIds: formModel.elementIds.join(",") || "",
      // 传递断言字段
      assertType: formModel.assertType,
      assertLocatorType: formModel.assertLocatorType,
      assertLocatorValue: formModel.assertLocatorValue,
      assertExpectedValue: formModel.assertExpectedValue,
      // 传递登录配置
      needLogin: formModel.needLogin,
      siteCode: formModel.siteCode,
      testAccount: formModel.testAccount, // 新增：传递测试账号
      testPassword: formModel.testPassword, // 新增：传递测试密码
    };

    if (isEdit.value) {
      await updateCase(submitData);
      ElMessage.success("编辑用例成功");
    } else {
      await addCase(submitData); // 无需再手动赋值creator，已经提前填充
      ElMessage.success("新增用例成功");
    }
    dialogVisible.value = false;
    fetchCaseList();
  } catch (err) {
    ElMessage.error(err?.response?.data?.msg || "操作失败");
  } finally {
    submitLoading.value = false;
  }
}

function handleDelete(id) {
  if (!id) {
    ElMessage.error("缺少用例ID");
    return;
  }
  ElMessageBox.confirm("确认删除该用例吗？", "提示", { type: "warning" })
    .then(async () => {
      await deleteCase(id);
      ElMessage.success("删除用例成功");
      fetchCaseList();
    })
    .catch(() => {});
}

function formatDate(val) {
  if (!val) return "";
  val = typeof val === "string" ? val.replace("T", " ") : val;
  const d = new Date(val);
  if (Number.isNaN(d.getTime())) return val;
  const y = d.getFullYear();
  const m = String(d.getMonth() + 1).padStart(2, "0");
  const day = String(d.getDate()).padStart(2, "0");
  const hh = String(d.getHours()).padStart(2, "0");
  const mm = String(d.getMinutes()).padStart(2, "0");
  const ss = String(d.getSeconds()).padStart(2, "0");
  return `${y}-${m}-${day} ${hh}:${mm}:${ss}`;
}

function initTableResizeObserver() {
  if (!tableWrap.value) return;
  resizeObserver = new ResizeObserver((entries) => {
    setTimeout(() => {
      const wrap = entries[0].target;
      if (wrap.clientHeight < 400) {
        wrap.style.minHeight = "400px";
      } else {
        wrap.style.height = "auto";
      }
    }, 100);
  });
  resizeObserver.observe(tableWrap.value);
}

function destroyTableResizeObserver() {
  if (resizeObserver) {
    resizeObserver.disconnect();
    resizeObserver = null;
  }
}

onMounted(() => {
  fetchCaseList();
  fetchElementList();
  fetchSiteConfigs();
  initTableResizeObserver();
});

onUnmounted(() => {
  destroyTableResizeObserver();
});
</script>

<style scoped lang="less">
.case-manage-root {
  padding: 20px;
  background: transparent;
}

.top-area {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  gap: 12px;
}

.action-area {
  flex-shrink: 0;
}

.table-wrap {
  width: 100%;
  min-height: 400px;
  overflow: auto;
  box-sizing: border-box;
  margin-bottom: 16px;
}

.case-table {
  background: #fff;
  border-radius: 8px;
  position: relative;
}

.steps-cell {
  white-space: normal;
  line-height: 1.4;
}

.el-table__header th {
  background: #f9fafb !important;
}

:deep(.el-loading-mask) {
  background-color: rgba(255, 255, 255, 0.65);
}

.dialog-form {
  padding-top: 4px;
}

@media (max-width: 768px) {
  .top-area {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
