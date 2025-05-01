<template>
  <div class="table-wrapper">
    <h2>라이딩 요약</h2>
    <div v-if="loading">🚴 데이터를 불러오는 중입니다...</div>
    <table v-else class="ride-table">
      <thead>
      <tr class="table-row total-row">
        <th>오늘 기준</th>
        <th>거리 (km)</th>
        <th>고도 (m)</th>
        <th>칼로리 (kcal)</th>
        <th>⏱ 라이딩 시간</th>
        <th>⏳ 총 시간</th>
      </tr>
      </thead>
      <tbody>
      <tr class="table-row total-row">
        <td>{{ formatDate(new Date()) }}</td>
        <td>{{ totalSummary.totalDistance.toFixed(2) }}</td>
        <td>{{ totalSummary.totalAscent }}</td>
        <td>{{ totalSummary.totalCalories }}</td>
        <td>{{ formatTime(totalSummary.movingTime) }}</td>
        <td>{{ formatTime(totalSummary.totalTime) }}</td>
      </tr>
      </tbody>
      <thead>
      <tr>
        <th @click="sortBy('startTime')">라이딩 일시</th>
        <th @click="sortBy('totalDistance')">거리 (km)</th>
        <th @click="sortBy('totalAscent')">고도 (m)</th>
        <th @click="sortBy('totalCalories')">칼로리 (kcal)</th>
        <th @click="sortBy('movingTime')">⏱ 라이딩 시간</th>
        <th @click="sortBy('totalTime')">⏳ 총 시간</th>
      </tr>
      </thead>
      <tbody>
      <tr
          v-for="item in sortedList"
          :key="item.id"
          class="clickable-row"
          @click="onRowClick(item.id)"
      >
        <td>{{ formatDate(item.startTime) }}</td>
        <td>{{ item.totalDistance?.toFixed(2) }}</td>
        <td>{{ item.totalAscent }}</td>
        <td>{{ item.totalCalories }}</td>
        <td>{{ formatTime(item.movingTime) }}</td>
        <td>{{ formatTime(item.totalTime) }}</td>
      </tr>
      </tbody>
    </table>
  </div>
</template>

<script>
export default {
  name: 'RideSummaryTable',
  data() {
    return {
      loading: true,
      rideList: [],
      totalSummary: {
        totalDistance: 0,
        totalAscent: 0,
        totalCalories: 0,
        movingTime: 0,
        totalTime: 0
      },
      currentSortKey: '',
      currentSortAsc: true
    };
  },
  computed: {
    sortedList() {
      const key = this.currentSortKey;
      if (!key) return this.rideList;
      return [...this.rideList].sort((a, b) => {
        const valA = a?.[key];
        const valB = b?.[key];
        if (valA === valB) return 0;
        if (key === 'startTime') {
          return this.currentSortAsc
              ? new Date(valA) - new Date(valB)
              : new Date(valB) - new Date(valA);
        }
        return this.currentSortAsc
            ? (valA > valB ? 1 : -1)
            : (valA < valB ? 1 : -1);
      });
    }
  },
  methods: {
    formatTime(minutes) {
      if (minutes === undefined || minutes === null) return '-';
      const h = Math.floor(minutes / 60);
      const m = minutes % 60;
      return `${h}시간 ${m}분`;
    },
    formatDate(date) {
      if (!date) return '-';
      return new Date(date).toLocaleDateString('ko-KR', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit'
      });
    },
    sortBy(key) {
      if (this.currentSortKey === key) {
        this.currentSortAsc = !this.currentSortAsc;
      } else {
        this.currentSortKey = key;
        this.currentSortAsc = true;
      }
    },
    async fetchRideData() {
      try {
        const res = await fetch('/api/rides', {
          headers: {
            'Content-Type': 'application/json'
          }
        });

        if (!res.ok) {
          throw new Error(`HTTP error! status: ${res.status}`);
        }

        const data = await res.json();
        this.rideList = data;

        this.totalSummary = {
          totalDistance: this.rideList.reduce((sum, r) => sum + (r.totalDistance || 0), 0),
          totalAscent: this.rideList.reduce((sum, r) => sum + (r.totalAscent || 0), 0),
          totalCalories: this.rideList.reduce((sum, r) => sum + (r.totalCalories || 0), 0),
          movingTime: this.rideList.reduce((sum, r) => sum + (r.movingTime || 0), 0),
          totalTime: this.rideList.reduce((sum, r) => sum + (r.totalTime || 0), 0)
        };
      } catch (e) {
        console.error('❌ 데이터 불러오기 실패', e);
      } finally {
        this.loading = false;
      }
    },
    onRowClick(id) {
      this.$router.push(`/ride/${id}`);  // ✅ filename → id 기반 URL로 수정
    }
  },
  mounted() {
    this.fetchRideData();
  }
};
</script>

<style scoped>
.table-wrapper {
  padding: 1rem;
  max-width: 1000px;
  margin: auto;
}

.ride-table {
  width: 100%;
  border-collapse: collapse;
  text-align: center;
}

.ride-table th, .ride-table td {
  border: 1px solid #ccc;
  padding: 0.5rem;
}

.ride-table th {
  background-color: #f4f4f4;
  cursor: pointer;
}

.table-row.total-row {
  font-weight: bold;
  background-color: #e8f5e9;
}

.clickable-row {
  cursor: pointer;
}
</style>
