<template>
  <PlaceholderPage
    tag="Student / Python Lab"
    title="学生端 Python 实验"
    description="后续用于查看实验任务、提交 Python 代码、查看运行结果和成绩反馈。"
    module-name="pythonlab"
    audience="学生端"
    route-path="/student/experiment/python"
    :next-steps="nextSteps"
  />
</template>

<script setup>
import PlaceholderPage from '@/components/base/PlaceholderPage.vue'

const nextSteps = [
  '补实验任务列表与详情页',
  '补代码提交表单',
  '补运行结果和得分反馈展示',
]
</script>
