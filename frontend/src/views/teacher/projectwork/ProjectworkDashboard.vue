<template>
  <PlaceholderPage
    tag="Teacher / Projectwork"
    title="教师端大作业管理"
    description="后续用于大作业发布、分组管理、提交审查、Agent 报告查看和教师确认成绩。"
    module-name="projectwork"
    audience="教师端"
    route-path="/teacher/projectwork"
    :next-steps="nextSteps"
  />
</template>

<script setup>
import PlaceholderPage from '@/components/base/PlaceholderPage.vue'

const nextSteps = [
  '补大作业列表与分组页面',
  '补 ZIP 解压和 Agent 报告展示',
  '补教师复核评分流程',
]
</script>
