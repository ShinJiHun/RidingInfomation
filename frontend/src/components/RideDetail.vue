<template>
  <div>
    <h2>📍 상세 정보</h2>
    <p>파일 이름: {{ filename }}</p>
  </div>
</template>

<script setup>
import { useRoute } from 'vue-router';
const route = useRoute();
const filename = route.params.filename;
</script>
