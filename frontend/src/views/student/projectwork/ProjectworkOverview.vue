<template>
  <PlaceholderPage
    tag="Student / Projectwork"
    title="学生端大作业"
    description="后续用于查看分组信息、上传大作业、查看 Agent 分析结果和最终成绩。"
    module-name="projectwork"
    audience="学生端"
    route-path="/student/projectwork"
    :next-steps="nextSteps"
  />
</template>

<script setup>
import PlaceholderPage from '@/components/base/PlaceholderPage.vue'

const nextSteps = [
  '补分组和任务说明页面',
  '补 ZIP 提交与历史记录页面',
  '补 Agent 评测报告查看页',
]
</script>
