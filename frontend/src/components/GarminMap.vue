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
    const res = await fetch('http://localhost:8080/api/fit/files');
    this.fileList = await res.json();

    this.drawAllTracks();
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

      const fileObj = this.fileList[this.currentIndex];
      const fileName = fileObj.activityCoreVO?.filename;

      if (!fileName) {
        console.warn("❌ 유효하지 않은 파일 객체입니다:", fileObj);
        this.currentIndex++;
        this.drawAllTracks();
        return;
      }


      console.log("file name : ", fileName);
      const res = await fetch(`http://localhost:8080/api/fit/map-by-file?file=${encodeURIComponent(fileName)}`);
      const data = await res.json();

      if (!Array.isArray(data) || data.length === 0) {
        console.warn(`⚠️ 데이터 없음: ${fileName}`);
        this.currentIndex++;
        this.drawAllTracks();
        return;
      }

      const coords = data.map(point => [point.latitude, point.longitude]);
      const color = this.getColorByExtension(fileName);

      L.polyline(coords, { color }).addTo(this.map);
      this.currentIndex++;
      this.drawAllTracks();
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
