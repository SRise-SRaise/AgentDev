<template>
  <PlaceholderPage
    tag="Teacher / Vue Lab"
    title="教师端 Vue 实验"
    description="后续用于实验任务发布、运行配置管理、提交审查、截图查看与评分。"
    module-name="vuelab"
    audience="教师端"
    route-path="/teacher/experiment/vue"
    :next-steps="nextSteps"
  />
</template>

<script setup>
import PlaceholderPage from '@/components/base/PlaceholderPage.vue'

const nextSteps = [
  '接入实验任务列表接口',
  '补运行配置与 ZIP 提交审查',
  '补截图查看与教师评分流程',
]
</script>
