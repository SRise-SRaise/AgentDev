<script setup>
import { computed } from 'vue'
import { RouterLink, RouterView, useRoute } from 'vue-router'

const route = useRoute()

const navItems = computed(() => route.meta?.navItems ?? [])
const section = computed(() => route.meta?.section ?? 'workspace')
</script>

<template>
  <div class="app-shell">
    <aside class="app-shell__sidebar">
      <div>
        <p class="app-shell__eyebrow">Web Advanced Platform</p>
        <h1>{{ section === 'teacher' ? '教师端' : '学生端' }}</h1>
      </div>
      <nav class="app-shell__nav">
        <RouterLink
          v-for="item in navItems"
          :key="item.to"
          :to="item.to"
          class="app-shell__link"
          active-class="is-active"
        >
          {{ item.label }}
        </RouterLink>
      </nav>
    </aside>

    <main class="app-shell__content">
      <RouterView />
    </main>
  </div>
</template>

<style scoped>
.app-shell {
  min-height: 100vh;
  display: grid;
  grid-template-columns: 280px 1fr;
}

.app-shell__sidebar {
  padding: 28px;
  background: #0f172a;
  color: #ffffff;
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.app-shell__eyebrow {
  margin: 0;
  color: #8fb7ff;
  font-size: 0.85rem;
  text-transform: uppercase;
  letter-spacing: 0.08em;
}

.app-shell__sidebar h1 {
  margin: 8px 0 0;
  font-size: 1.8rem;
}

.app-shell__nav {
  display: grid;
  gap: 10px;
}

.app-shell__link {
  padding: 12px 14px;
  border-radius: 12px;
  color: #d7e3ff;
  background: rgba(255, 255, 255, 0.04);
}

.app-shell__link.is-active {
  background: #1d4ed8;
  color: #ffffff;
}

.app-shell__content {
  padding: 28px;
}

@media (max-width: 960px) {
  .app-shell {
    grid-template-columns: 1fr;
  }

  .app-shell__sidebar {
    padding-bottom: 18px;
  }

  .app-shell__content {
    padding-top: 20px;
  }
}
</style>
