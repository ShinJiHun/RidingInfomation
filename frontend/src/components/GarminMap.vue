<template>
  <div>
    <div id="map" style="height: 1000px;"></div>
  </div>
</template>

<style scoped>
#map {
  width: 100%;
  height: 1000px;
}
</style>

<script>
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';

export default {
  data() {
    return {
      map: null,
      fileList: [],
      currentIndex: 0,
      baseUrl: '', // ✅ 여기 저장할 것
    };
  },
  async mounted() {
    // 0. Spring에서 API base URL 받아오기
    const configRes = await fetch('/api/config/base-url');
    const config = await configRes.json();
    this.baseUrl = config.baseUrl;

    // 1. 지도 초기화
    this.map = L.map('map').setView([36.35, 127.38], 7.2);
    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      attribution: '© OpenStreetMap contributors',
    }).addTo(this.map);

    // 2. 모든 파일 목록 요청
    const res = await fetch(`${this.baseUrl}/api/fit/files`);
    this.fileList = await res.json();

    // this.drawAllTracks();
  },
  methods: {
    getColorByExtension(fileName) {
      if (fileName.endsWith('.fit')) return 'red';
      if (fileName.endsWith('.gpx')) return 'blue';
      if (fileName.endsWith('.tcx')) return 'green';
      return 'black'; // fallback
    },
    async drawAllTracks() {
      if (this.currentIndex >= this.fileList.length) {
        console.log('✅ 모든 경로를 불러왔습니다.');
        return;
      }

      const fileName = this.fileList[this.currentIndex];
      const res = await fetch(`${this.baseUrl}/api/fit/map-by-file?file=${fileName}`);
      const data = await res.json();

      const coords = data.latitudes.map((lat, i) => [lat, data.longitudes[i]]);
      const color = this.getColorByExtension(fileName);

      const polyline = L.polyline(coords, { color }).addTo(this.map);
      this.map.fitBounds(polyline.getBounds());

      this.currentIndex++;
      this.drawAllTracks();
    }
  }
};
</script>