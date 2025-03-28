<template>
  <div>
    <div id="map" style="height: 1000px;"></div>
  </div>
</template>

<script>
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';

export default {
  data() {
    return {
      map: null,
      fitFiles: [],
      currentIndex: 0,
    };
  },
  async mounted() {
    // 1. 지도 초기화
    this.map = L.map('map').setView([36.35, 127.38], 8);
    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      attribution: '© OpenStreetMap contributors',
    }).addTo(this.map);

    // 2. 파일 목록 요청
    const res = await fetch('http://localhost:8085/api/fit/files');
    this.fitFiles = await res.json();

    // 3. 자동으로 하나씩 경로 그리기 시작
    this.drawAllTracks();
  },
  methods: {
    async drawAllTracks() {
      if (this.currentIndex >= this.fitFiles.length) {
        console.log('✅ 모든 경로를 불러왔습니다.');
        return;
      }

      const fileName = this.fitFiles[this.currentIndex];
      const res = await fetch(`http://localhost:8085/api/fit/map-by-file?file=${fileName}`);
      const data = await res.json();

      const coords = data.latitudes.map((lat, i) => [lat, data.longitudes[i]]);
      L.polyline(coords, {color: 'red'}).addTo(this.map);

      this.currentIndex++;

      // 약간의 딜레이를 주고 다음 트랙 로드 (너무 빠르면 API 연속 호출 문제 발생 가능)
      setTimeout(() => {
        this.drawAllTracks();
      }, 300); // 0.3초 간격으로 다음 경로 로드
    }
  }
};
</script>

<style scoped>
#map {
  width: 100%;
  height: 1000px;
}
</style>
