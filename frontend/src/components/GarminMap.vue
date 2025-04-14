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
      fileList: [],
      currentIndex: 0,
    };
  },
  async mounted() {
    // 1. 지도 초기화
    this.map = L.map('map').setView([36.35, 127.38], 7.2);
    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      attribution: '© OpenStreetMap contributors',
    }).addTo(this.map);

    // 2. 모든 파일 목록 요청 (fit, gpx, tcx 포함)
    const res = await fetch('http://localhost:8085/api/fit/files');
    this.fileList = await res.json();

    // 3. 경로 그리기 시작
    this.drawAllTracks();
  },
  methods: {
    getColorByExtension(fileName) {
      if (fileName.endsWith('.fit')) return 'red';
      if (fileName.endsWith('.gpx')) return 'red';
      if (fileName.endsWith('.tcx')) return 'red';
      return 'black'; // fallback
    },
    async drawAllTracks() {
      if (this.currentIndex >= this.fileList.length) {
        console.log('✅ 모든 경로를 불러왔습니다.');
        return;
      }

      const fileName = this.fileList[this.currentIndex];
      const res = await fetch(`http://localhost:8085/api/fit/map-by-file?file=${fileName}`);
      const data = await res.json();

      const coords = data.latitudes.map((lat, i) => [lat, data.longitudes[i]]);
      const color = this.getColorByExtension(fileName);

      const polyline = L.polyline(coords, { color }).addTo(this.map);

      // 🟡 경로 기준으로 화면 자동 맞춤 (전체 지도 X)
      this.map.fitBounds(polyline.getBounds());

      this.currentIndex++;
      this.drawAllTracks(); // 다음 경로 그리기
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
