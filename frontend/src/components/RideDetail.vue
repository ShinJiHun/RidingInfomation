<template>
  <div class="feed" v-if="rideInfo">
    <h2>📍 상세 정보</h2>
    <div class="meta">
      <p>파일 이름: {{ rideInfo.activityCoreVO.filename }}</p>
      <p>시작 시간: {{ formatDate(rideInfo.activityCoreVO.startTime) }}</p>
      <p>운동 유형: 자전거</p>
    </div>

    <div class="feed-card">
      <img src="https://via.placeholder.com/600x300" alt="Ride Snapshot" />
      <div class="feed-content">
        <p><strong>거리:</strong> {{ rideInfo.activityCoreVO.totalDistance.toFixed(2) }}km</p>
        <p><strong>고도 상승:</strong> {{ rideInfo.activityCoreVO.totalAscent }}m</p>
        <p><strong>시간:</strong> {{ formatTime(rideInfo.activityCoreVO.movingTime) }}</p>
        <p>오늘도 열심히 달렸습니다! 날씨가 정말 좋았고, 업힐도 재미있었어요 🚴‍♂️☀️</p>
      </div>
    </div>
  </div>
  <div v-else class="feed">
    ⏳ 상세 데이터를 불러오는 중입니다...
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue';
import { useRoute } from 'vue-router';

const route = useRoute();
const filename = route.params.filename;
const rideInfo = ref(null);

onMounted(async () => {
  try {
    const res = await fetch(`/api/fit/detail/${filename}`);
    rideInfo.value = await res.json();
    console.log('✅ rideInfo:', rideInfo.value);
  } catch (e) {
    console.error('❌ 상세 데이터 로딩 실패', e);
  }
});

const formatDate = (dateStr) => {
  if (!dateStr) return '-';
  const date = new Date(dateStr);
  return date.toLocaleString('ko-KR', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  });
};

const formatTime = (minutes) => {
  if (minutes == null) return '-';
  const h = Math.floor(minutes / 60);
  const m = minutes % 60;
  return `${h}시간 ${m}분`;
};
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
