<template>
  <el-form
    ref="formRef"
    :model="formData"
    :rules="rules"
    label-width="120px"
  >
    <el-form-item label="客户端名称" prop="clientName">
      <el-input
        v-model="formData.clientName"
        placeholder="请输入客户端名称"
        maxlength="50"
        show-word-limit
      />
    </el-form-item>

    <el-form-item label="客户端类型" prop="clientType">
      <el-select
        v-model="formData.clientType"
        placeholder="请选择客户端类型"
        style="width: 100%"
        @change="handleClientTypeChange"
      >
        <el-option label="QQ (NapCat)" value="qq" />
        <el-option label="微信 (ComWechat)" value="wechat" />
        <el-option label="钉钉 (DingTalk)" value="dingtalk" />
      </el-select>
    </el-form-item>

    <el-form-item label="协议类型" prop="protocolType">
      <el-checkbox-group v-model="selectedProtocols">
        <el-checkbox label="websocket">WebSocket</el-checkbox>
        <el-checkbox label="http">HTTP</el-checkbox>
        <el-checkbox label="https">HTTPS</el-checkbox>
      </el-checkbox-group>
    </el-form-item>

    <el-form-item label="连接配置" prop="connectionConfig">
      <div style="width: 100%">
        <el-input
          v-model="formData.connectionConfig.host"
          placeholder="主机地址 (如: 127.0.0.1)"
          style="margin-bottom: 10px"
        >
          <template #prepend>Host</template>
        </el-input>

        <el-input
          v-if="needsWsPort"
          v-model.number="formData.connectionConfig.wsPort"
          placeholder="WebSocket端口"
          type="number"
          style="margin-bottom: 10px"
        >
          <template #prepend>WS Port</template>
        </el-input>

        <el-input
          v-if="needsHttpPort"
          v-model.number="formData.connectionConfig.httpPort"
          placeholder="HTTP端口"
          type="number"
          style="margin-bottom: 10px"
        >
          <template #prepend>HTTP Port</template>
        </el-input>

        <el-input
          v-model="formData.connectionConfig.accessToken"
          placeholder="访问令牌 (可选)"
          type="password"
          show-password
        >
          <template #prepend>Token</template>
        </el-input>
      </div>
    </el-form-item>

    <el-form-item label="启用状态">
      <el-switch v-model="formData.enabled" />
      <span style="margin-left: 10px; color: #909399; font-size: 12px">
        {{ formData.enabled ? '已启用' : '已禁用' }}
      </span>
    </el-form-item>

    <el-form-item label="备注">
      <el-input
        v-model="formData.remark"
        type="textarea"
        :rows="3"
        placeholder="请输入备注信息"
        maxlength="200"
        show-word-limit
      />
    </el-form-item>

    <!-- JSON配置预览 (开发调试用) -->
    <el-collapse v-if="showAdvanced" style="margin-top: 20px">
      <el-collapse-item title="高级配置 (JSON)" name="advanced">
        <el-input
          v-model="configJson"
          type="textarea"
          :rows="10"
          placeholder="JSON配置"
          @blur="handleJsonChange"
        />
        <el-alert
          v-if="jsonError"
          type="error"
          :title="jsonError"
          style="margin-top: 10px"
          :closable="false"
        />
      </el-collapse-item>
    </el-collapse>
  </el-form>
</template>

<script setup>
import { ref, reactive, computed, watch, onMounted } from 'vue'
import { ElMessage } from 'element-plus'

const props = defineProps({
  client: {
    type: Object,
    default: null
  },
  isEdit: {
    type: Boolean,
    default: false
  }
})

// 表单引用
const formRef = ref(null)

// 表单数据
const formData = reactive({
  clientName: '',
  clientType: '',
  protocolType: '',
  connectionConfig: {
    host: '',
    wsPort: null,
    httpPort: null,
    accessToken: ''
  },
  enabled: true,
  remark: ''
})

// 选中的协议
const selectedProtocols = ref([])

// JSON配置
const configJson = ref('')
const jsonError = ref('')
const showAdvanced = ref(false)

// 表单验证规则
const rules = {
  clientName: [
    { required: true, message: '请输入客户端名称', trigger: 'blur' },
    { min: 2, max: 50, message: '长度在 2 到 50 个字符', trigger: 'blur' }
  ],
  clientType: [
    { required: true, message: '请选择客户端类型', trigger: 'change' }
  ],
  protocolType: [
    { required: true, message: '请选择至少一个协议类型', trigger: 'change' }
  ]
}

// 计算是否需要端口配置
const needsWsPort = computed(() => {
  return selectedProtocols.value.includes('websocket')
})

const needsHttpPort = computed(() => {
  return selectedProtocols.value.includes('http') ||
         selectedProtocols.value.includes('https')
})

// 监听协议变化
watch(selectedProtocols, (newVal) => {
  formData.protocolType = newVal.join(',')
  updateConfigJson()
}, { deep: true })

// 监听表单数据变化
watch(() => formData.connectionConfig, () => {
  updateConfigJson()
}, { deep: true })

// 客户端类型变化
const handleClientTypeChange = (type) => {
  // 根据客户端类型设置默认协议
  if (type === 'qq') {
    selectedProtocols.value = ['websocket', 'http']
    formData.connectionConfig.wsPort = 3001
    formData.connectionConfig.httpPort = 3000
  } else if (type === 'wechat') {
    selectedProtocols.value = ['websocket']
    formData.connectionConfig.wsPort = 8080
  } else if (type === 'dingtalk') {
    selectedProtocols.value = ['http']
    formData.connectionConfig.httpPort = 8080
  }
}

// 更新JSON配置
const updateConfigJson = () => {
  try {
    const config = {
      ...formData,
      protocolType: selectedProtocols.value.join(',')
    }
    configJson.value = JSON.stringify(config, null, 2)
    jsonError.value = ''
  } catch (error) {
    jsonError.value = '生成JSON失败: ' + error.message
  }
}

// 处理JSON变化
const handleJsonChange = () => {
  try {
    const config = JSON.parse(configJson.value)
    Object.assign(formData, config)
    if (config.protocolType) {
      selectedProtocols.value = config.protocolType.split(',')
    }
    jsonError.value = ''
    ElMessage.success('JSON配置已更新')
  } catch (error) {
    jsonError.value = 'JSON格式错误: ' + error.message
  }
}

// 验证表单
const validate = async () => {
  try {
    await formRef.value.validate()

    // 验证端口配置
    if (needsWsPort.value && !formData.connectionConfig.wsPort) {
      ElMessage.error('请配置WebSocket端口')
      return null
    }
    if (needsHttpPort.value && !formData.connectionConfig.httpPort) {
      ElMessage.error('请配置HTTP端口')
      return null
    }

    // 验证主机地址
    if (!formData.connectionConfig.host) {
      ElMessage.error('请配置主机地址')
      return null
    }

    return {
      ...formData,
      protocolType: selectedProtocols.value.join(',')
    }
  } catch (error) {
    console.error('Form validation failed:', error)
    return null
  }
}

// 初始化表单
const initForm = () => {
  if (props.client) {
    Object.assign(formData, {
      clientName: props.client.clientName || '',
      clientType: props.client.clientType || '',
      protocolType: props.client.protocolType || '',
      connectionConfig: props.client.connectionConfig || {
        host: '',
        wsPort: null,
        httpPort: null,
        accessToken: ''
      },
      enabled: props.client.enabled !== undefined ? props.client.enabled : true,
      remark: props.client.remark || ''
    })

    // 解析协议类型
    if (props.client.protocolType) {
      selectedProtocols.value = props.client.protocolType.split(',').map(p => p.trim())
    }

    updateConfigJson()
  }
}

// 暴露验证方法
defineExpose({
  validate
})

// 初始化
onMounted(() => {
  initForm()
})
</script>

<style scoped>
:deep(.el-form-item__label) {
  font-weight: 500;
}

:deep(.el-input-group__prepend) {
  width: 100px;
  text-align: center;
}
</style>
