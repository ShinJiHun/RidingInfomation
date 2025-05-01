<template>
  <div>
    <div id="map" style="height: 400px;"></div>
  </div>
</template>

<script>
import L from 'leaflet';

export default {
  name: 'DetailPage',
  data() {
    return {
      polyline: '}_wFf~cjVvJdJhAfE`BzCtAfCvDhDxA|BxCpC...'  // 서버에서 받아온 Polyline 문자열
    };
  },
  mounted() {
    this.initMap();
  },
  methods: {
    initMap() {
      // 지도 생성
      const map = L.map('map').setView([37.4219999, -122.0840575], 13); // 중심 좌표는 임의로 설정
      L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png').addTo(map);
      const decodedPolyline = this.decodePolyline(this.polyline);
      L.polyline(decodedPolyline, { color: 'blue' }).addTo(map);
    },

    decodePolyline(polyline) {
      const polylinePoints = [];
      let index = 0, len = polyline.length;
      let lat = 0, lng = 0;

      while (index < len) {
        let result = 0, shift = 0, byte;
        do {
          byte = polyline.charCodeAt(index++) - 63;
          result |= (byte & 0x1f) << shift;
          shift += 5;
        } while (byte >= 0x20);
        let deltaLat = (result & 1) ? ~(result >> 1) : (result >> 1);
        lat += deltaLat;

        result = 0;
        shift = 0;
        do {
          byte = polyline.charCodeAt(index++) - 63;
          result |= (byte & 0x1f) << shift;
          shift += 5;
        } while (byte >= 0x20);
        let deltaLng = (result & 1) ? ~(result >> 1) : (result >> 1);
        lng += deltaLng;

        polylinePoints.push([lat * 1e-5, lng * 1e-5]);  // 위도, 경도 값 추가 (단위 변환)
      }
      return polylinePoints;
    }
  }
};
</script>

<style scoped>
#map {
  width: 100%;
  height: 400px;
}
</style>
