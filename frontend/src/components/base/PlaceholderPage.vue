<template>
  <div class="placeholder-page">
    <div class="placeholder-page__hero">
      <span class="placeholder-page__tag">{{ tag }}</span>
      <h1>{{ title }}</h1>
      <p>{{ description }}</p>
    </div>

    <div class="placeholder-page__grid">
      <SectionCard title="当前状态" description="该页面已完成目录归位和基础占位，后续可直接补真实业务逻辑。">
        <ul>
          <li>页面路径：{{ routePath }}</li>
          <li>所属模块：{{ moduleName }}</li>
          <li>适用角色：{{ audience }}</li>
        </ul>
      </SectionCard>

      <SectionCard title="后续开发建议" description="建议优先从接口联通、列表展示、提交流程开始补功能。">
        <ul>
          <li v-for="item in nextSteps" :key="item">{{ item }}</li>
        </ul>
      </SectionCard>
    </div>
  </div>
</template>

<script setup>
import SectionCard from './SectionCard.vue'

defineProps({
  tag: {
    type: String,
    required: true,
  },
  title: {
    type: String,
    required: true,
  },
  description: {
    type: String,
    required: true,
  },
  moduleName: {
    type: String,
    required: true,
  },
  audience: {
    type: String,
    required: true,
  },
  routePath: {
    type: String,
    required: true,
  },
  nextSteps: {
    type: Array,
    default: () => [],
  },
})
</script>

<style scoped>
.placeholder-page {
  display: grid;
  gap: 24px;
}

.placeholder-page__hero {
  padding: 28px;
  border-radius: 20px;
  background: linear-gradient(135deg, #13233f, #1b63d1);
  color: #ffffff;
}

.placeholder-page__tag {
  display: inline-flex;
  padding: 4px 10px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.16);
  font-size: 0.85rem;
}

.placeholder-page__hero h1 {
  margin: 14px 0 8px;
  font-size: 2rem;
}

.placeholder-page__hero p {
  margin: 0;
  max-width: 680px;
  color: rgba(255, 255, 255, 0.9);
}

.placeholder-page__grid {
  display: grid;
  gap: 20px;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
}

ul {
  margin: 0;
  padding-left: 18px;
  color: #33415d;
}

li + li {
  margin-top: 10px;
}
</style>
