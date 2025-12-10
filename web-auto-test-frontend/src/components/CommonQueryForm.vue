<template>
  <el-form :model="form" class="cqf-form" label-width="100px">
    <div class="fields">
      <template v-for="field in fields" :key="field.key">
        <el-form-item :label="field.label" :prop="field.key" class="field-item">
          <template v-if="field.type === 'select'">
            <el-select
              v-model="form[field.key]"
              :placeholder="field.placeholder || '请选择'"
            >
              <el-option
                v-for="opt in field.options || []"
                :key="opt.value"
                :label="opt.label"
                :value="opt.value"
              />
            </el-select>
          </template>
          <template v-else-if="field.type === 'date-range'">
            <el-date-picker
              v-model="form[field.key]"
              type="daterange"
              range-separator="至"
              start-placeholder="开始日期"
              end-placeholder="结束日期"
              :placeholder="field.placeholder || '选择日期范围'"
              style="width: 100%"
            />
          </template>
          <template v-else>
            <el-input
              v-model="form[field.key]"
              :placeholder="field.placeholder || '请输入'"
            />
          </template>
        </el-form-item>
      </template>
    </div>

    <div class="actions">
      <el-button type="text" @click="handleReset">重置</el-button>
      <el-button type="primary" @click="handleSearch">查询</el-button>
    </div>
  </el-form>
</template>

<script setup>
import { reactive, toRefs, watch, onBeforeMount } from "vue";

/**
 * fields: Array<{ label: string, key: string, type?: 'input'|'select', options?: Array<{label,value}>, placeholder?: string }>
 */
const props = defineProps({
  fields: {
    type: Array,
    default: () => [],
  },
});

const emit = defineEmits(["onSearch", "onReset"]);

// 初始化表单对象，确保每个字段都有初始值
const form = reactive({});

function initForm() {
  props.fields.forEach((f) => {
    // 如果已有值则保持，否则初始化为空字符串
    if (!(f.key in form)) {
      form[f.key] = "";
    }
  });
}

onBeforeMount(() => {
  initForm();
});

// 当 fields 发生变化时（运行时动态调整），重新初始化缺失字段
watch(
  () => props.fields,
  () => {
    initForm();
  },
  { deep: true }
);

function handleSearch() {
  // 传递表单数据的浅拷贝
  emit("onSearch", { ...form });
}

function handleReset() {
  Object.keys(form).forEach((k) => {
    form[k] = "";
  });
  emit("onReset", {});
}

// 导出用于模板绑定
const { fields } = toRefs(props);
</script>

<style scoped lang="less">
.cqf-form {
  padding: 16px;
  background: #ffffff;
}

.fields {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: 12px 16px;
  align-items: center;
}

.field-item {
  margin-bottom: 0;
}

.el-form-item__label {
  width: 100px;
}

.actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 12px;
}

@media (max-width: 768px) {
  .fields {
    grid-template-columns: 1fr;
  }
}
</style>
