<template>
  <section class="section-card">
    <header class="section-card__header">
      <h2>{{ title }}</h2>
      <p v-if="description">{{ description }}</p>
    </header>
    <div class="section-card__body">
      <slot />
    </div>
  </section>
</template>

<script setup>
defineProps({
  title: {
    type: String,
    required: true,
  },
  description: {
    type: String,
    default: '',
  },
})
</script>

<style scoped>
.section-card {
  background: #ffffff;
  border: 1px solid #d8e0ef;
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 12px 30px rgba(31, 45, 61, 0.06);
}

.section-card__header h2 {
  margin: 0;
  font-size: 1.2rem;
}

.section-card__header p {
  margin: 8px 0 0;
  color: #61708a;
}

.section-card__body {
  margin-top: 20px;
}
</style>
