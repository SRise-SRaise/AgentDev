<template>
  <PlaceholderPage
    tag="Teacher / Course"
    title="课程闭环评价与成绩管理"
    description="用于课程大纲导入、作业成绩导入、实验成绩同步、总成绩计算与课程达成度分析。"
    module-name="course"
    audience="教师端"
    route-path="/teacher/course"
    :next-steps="nextSteps"
  />
</template>

<script setup>
import PlaceholderPage from '@/components/base/PlaceholderPage.vue'

const nextSteps = [
  '接入课程目标和评分项接口',
  '补成绩导入和汇总页面',
  '补报表导出流程',
]
</script>
