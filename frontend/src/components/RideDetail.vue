<template>
  <div class="ride-detail-wrapper" v-if="ride">
    <h2>🚴 라이딩 상세 정보</h2>

    <table class="ride-info-table">
      <tr><th>파일명</th><td>{{ ride.filename }}</td></tr>
      <tr><th>시작 시간</th><td>{{ formatDate(ride.startTime) }}</td></tr>
      <tr><th>종료 시간</th><td>{{ formatDate(ride.endTime) }}</td></tr>
      <tr><th>거리</th><td>{{ ride.totalDistance.toFixed(2) }} km</td></tr>
      <tr><th>고도 상승</th><td>{{ ride.totalAscent }} m</td></tr>
      <tr><th>칼로리</th><td>{{ ride.totalCalories }} kcal</td></tr>
      <tr><th>라이딩 시간</th><td>{{ formatTime(ride.movingTime) }}</td></tr>
      <tr><th>총 시간</th><td>{{ formatTime(ride.totalTime) }}</td></tr>
    </table>
    <!-- 구글 맵 정적 이미지 -->
    <h3>📸 라이딩 경로 이미지</h3>
    <img v-for="i in 1" :key="i" :src="getStaticMapImageUrl(ride.route, 9, 9)" />
    <br>
    <router-link to="/">← 목록으로 돌아가기</router-link>
  </div>
  <div v-else class="loading">📡 데이터를 불러오는 중입니다...</div>
</template>

<script>
export default {
  name: 'RideDetail',
  data() {
    return {
      ride: null
    };
  },
  async mounted() {
    const id = this.$route.params.id;
    try {
      const res = await fetch(`/api/rides/${id}`);
      this.ride = await res.json();
    } catch (e) {
      console.error("❌ 상세 데이터 로딩 실패", e);
    }
  },
  methods: {
    formatTime(minutes) {
      if (!minutes) return '-';
      const h = Math.floor(minutes / 60);
      const m = minutes % 60;
      return `${h}시간 ${m}분`;
    },
    formatDate(date) {
      if (!date) return '-';
      return new Date(date).toLocaleString('ko-KR', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit'
      });
    },
    getStaticMapImageUrl(route, zom = 10) {
      if (!route || route.length === 0) return '';

      const pathCoords = [];
      const R = 6371000;
      const samplingDistance = 1000 * (15 - zom); // 더 낮은 zoom일수록 간격 늘림

      function haversine(lat1, lon1, lat2, lon2) {
        const toRad = deg => deg * Math.PI / 180;
        const dLat = toRad(lat2 - lat1);
        const dLon = toRad(lon2 - lon1);
        const a = Math.sin(dLat / 2) ** 2 +
            Math.cos(toRad(lat1)) * Math.cos(toRad(lat2)) * Math.sin(dLon / 2) ** 2;
        return 2 * R * Math.asin(Math.sqrt(a));
      }

      let lastLat = route[0].latitude;
      let lastLon = route[0].longitude;
      pathCoords.push(`${lastLat},${lastLon}`);

      for (const point of route) {
        const dist = haversine(lastLat, lastLon, point.latitude, point.longitude);
        if (dist >= samplingDistance) {
          pathCoords.push(`${point.latitude},${point.longitude}`);
          lastLat = point.latitude;
          lastLon = point.longitude;
        }
      }

      const center = pathCoords[Math.floor(pathCoords.length / 2)];
      const pathStr = 'path=color:0xff0000|weight:3|' + pathCoords.join('|');
      const apiKey = "AIzaSyAQFha0v_MG6brYBqgKAT2Un9VeAS7POSQ";

      return `https://maps.googleapis.com/maps/api/staticmap?center=${center}&zoom=${zom}&size=600x300&${pathStr}&key=${apiKey}`;
    }
  }
};
</script>

<style scoped>
.ride-detail-wrapper {
  padding: 2rem;
  max-width: 800px;
  margin: auto;
}

.ride-info-table {
  width: 100%;
  margin-bottom: 1rem;
  border-collapse: collapse;
}

.ride-info-table th,
.ride-info-table td {
  border: 1px solid #ccc;
  padding: 0.5rem;
  text-align: left;
}

.loading {
  text-align: center;
  padding: 2rem;
}
</style>
