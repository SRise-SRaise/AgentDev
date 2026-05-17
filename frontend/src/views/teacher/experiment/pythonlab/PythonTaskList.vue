<template>
  <PlaceholderPage
    tag="Teacher / Python Lab"
    title="教师端 Python 实验"
    description="后续用于实验任务发布、测试用例管理、提交记录查看与评分复核。"
    module-name="pythonlab"
    audience="教师端"
    route-path="/teacher/experiment/python"
    :next-steps="nextSteps"
  />
</template>

<script setup>
import PlaceholderPage from '@/components/base/PlaceholderPage.vue'

const nextSteps = [
  '接入实验任务列表接口',
  '补测试用例管理页面',
  '补评分复核与运行日志查看',
]
</script>
