<template>
  <PlaceholderPage
    tag="Student / Course"
    title="学生端课程信息"
    description="后续用于展示课程目标、评分构成、个人成绩汇总和达成情况。"
    module-name="course"
    audience="学生端"
    route-path="/student/course"
    :next-steps="nextSteps"
  />
</template>

<script setup>
import PlaceholderPage from '@/components/base/PlaceholderPage.vue'

const nextSteps = [
  '补课程信息和成绩展示接口',
  '补实验平均分与大作业成绩展示',
  '补课程目标达成可视化',
]
</script>
