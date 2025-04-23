<template>
  <div class="feed">
    <h2>📍 상세 정보</h2>
    <div class="meta">
      <p>파일 이름: {{ filename }}</p>
      <p>업로드 날짜: 2025-04-23</p>
      <p>운동 유형: 자전거</p>
    </div>

    <div class="feed-card">
      <img src="https://via.placeholder.com/600x300" alt="Ride Snapshot" />
      <div class="feed-content">
        <p><strong>거리:</strong> 42.5km</p>
        <p><strong>고도 상승:</strong> 530m</p>
        <p><strong>시간:</strong> 1시간 35분</p>
        <p>오늘도 열심히 달렸습니다! 날씨가 정말 좋았고, 업힐도 재미있었어요 🚴‍♂️☀️</p>
      </div>
    </div>

    <div class="feed-card">
      <img src="https://via.placeholder.com/600x300" alt="Ride Snapshot" />
      <div class="feed-content">
        <p><strong>거리:</strong> 28.2km</p>
        <p><strong>고도 상승:</strong> 210m</p>
        <p><strong>시간:</strong> 1시간 10분</p>
        <p>간단한 리커버리 라이딩. 평지를 위주로 천천히 달렸어요. 기분 좋게 마무리!</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref, getCurrentInstance } from 'vue';
import { useRoute } from 'vue-router';

const route = useRoute();
const filename = route.params.filename;
const rideInfo = ref(null);

const { proxy } = getCurrentInstance(); // 👈 this.$axios 사용을 위해 getCurrentInstance() 사용

onMounted(async () => {
  const res = await proxy.$axios.get(`/api/fit/detail/${filename}`);
  rideInfo.value = res.data;

  console.log(rideInfo.value);
});
</script>

<style scoped>
.feed {
  max-width: 800px;
  margin: 0 auto;
  text-align: left;
}
.meta {
  background: #f0f0f0;
  padding: 15px;
  border-radius: 8px;
  margin-bottom: 20px;
}
.feed-card {
  background: #ffffff;
  border-radius: 10px;
  padding: 20px;
  margin-bottom: 20px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
}
.feed-card img {
  width: 100%;
  border-radius: 10px;
  margin-bottom: 10px;
}
.feed-content p {
  margin: 5px 0;
}
</style>