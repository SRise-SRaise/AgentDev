<template>
  <PlaceholderPage
    tag="Student / Vue Lab"
    title="学生端 Vue 实验"
    description="后续用于查看实验任务、上传 Vue 项目压缩包、查看运行截图和评分结果。"
    module-name="vuelab"
    audience="学生端"
    route-path="/student/experiment/vue"
    :next-steps="nextSteps"
  />
</template>

<script setup>
import PlaceholderPage from '@/components/base/PlaceholderPage.vue'

const nextSteps = [
  '补任务列表与说明查看',
  '补压缩包上传入口',
  '补运行截图和成绩结果页',
]
</script>
