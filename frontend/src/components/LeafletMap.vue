<template>
  <div id="map" ref="mapContainer" class="map-container"></div>
</template>

<script>
import L from 'leaflet';

export default {
  name: 'LeafletMap',
  props: {
    points: {
      type: Array,
      required: true // [{ lat, lon }, ...]
    }
  },
  data() {
    return {
      map: null,
      polyline: null
    };
  },
  mounted() {
    this.initMap();
  },
  methods: {
    initMap() {
      if (!this.points.length) return;

      // 지도 초기화
      this.map = L.map(this.$refs.mapContainer).setView(
          [this.points[0].lat, this.points[0].lon],
          13
      );

      // OpenStreetMap 타일 추가
      L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
        attribution: '&copy; OpenStreetMap contributors'
      }).addTo(this.map);

      // 폴리라인 그리기
      const latlngs = this.points.map(p => [p.lat, p.lon]);
      this.polyline = L.polyline(latlngs, { color: 'blue' }).addTo(this.map);

      // 전체 경로가 보이도록 맞춤
      this.map.fitBounds(this.polyline.getBounds());
    }
  }
};
</script>

<style scoped>
.map-container {
  height: 500px;
  width: 100%;
  margin-top: 1rem;
  border: 1px solid #ccc;
}
</style>
