<template>
  <div class="cp-root">
    <div class="cp-total">共 {{ total }} 条</div>
    <div class="cp-pagination" :class="{ disabled: disabled }">
      <el-pagination
        :current-page="localPageNum"
        :page-size="localPageSize"
        :page-sizes="pageSizes"
        layout="sizes, prev, pager, next, jumper"
        :total="total"
        @current-change="onCurrentChange"
        @size-change="onSizeChange"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, watch } from "vue";

const props = defineProps({
  total: {
    type: Number,
    required: true,
  },
  pageNum: {
    type: Number,
    default: 1,
  },
  pageSize: {
    type: Number,
    default: 10,
  },
  disabled: {
    type: Boolean,
    default: false,
  },
});

const emit = defineEmits(["onPageChange"]);

const pageSizes = [10, 20, 50, 100];

// 本地状态用于与 ElPagination 双向绑定
const localPageNum = ref(props.pageNum);
const localPageSize = ref(props.pageSize);

// 当父组件修改 props 时同步本地值
watch(
  () => props.pageNum,
  (v) => {
    localPageNum.value = v || 1;
  }
);

watch(
  () => props.pageSize,
  (v) => {
    localPageSize.value = v || 10;
  }
);

let debounceTimer = null;
function emitPageChangeDebounced(pageNum, pageSize) {
  if (debounceTimer) {
    clearTimeout(debounceTimer);
  }
  debounceTimer = setTimeout(() => {
    emit("onPageChange", { pageNum, pageSize });
  }, 300);
}

function onCurrentChange(page) {
  if (props.disabled) return;
  localPageNum.value = page;
  emitPageChangeDebounced(localPageNum.value, localPageSize.value);
}

function onSizeChange(size) {
  if (props.disabled) return;
  localPageSize.value = size;
  // 切换每页条数时通常重置到第 1 页
  localPageNum.value = 1;
  emitPageChangeDebounced(localPageNum.value, localPageSize.value);
}
</script>

<style scoped lang="less">
.cp-root {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 16px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.cp-total {
  margin-right: auto;
  padding-left: 0;
}

.cp-pagination {
  display: flex;
  align-items: center;
}

.cp-pagination.disabled {
  pointer-events: none;
  opacity: 0.6;
}

@media (max-width: 480px) {
  .cp-total {
    width: 100%;
    margin-bottom: 8px;
  }
}
</style>
