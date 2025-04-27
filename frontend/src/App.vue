<template>
  <div id="app">
    <div class="tabs">
      <router-link
          v-for="tab in tabs"
          :key="tab.path"
          :to="tab.path"
          class="tab-button"
          :class="{ active: $route.path === tab.path }"
      >
        {{ tab.label }}
      </router-link>
    </div>
    <router-view/>
  </div>
</template>

<script>
export default {
  name: 'App',
  data() {
    return {
      tabs: [
        {label: 'Record Save Page', path: '/upload'}, // ✅ 업로드 페이지 경로 수정
        {label: 'Garmin Table', path: '/'},            // ✅ 홈
        {label: 'Garmin Map', path: '/map'},           // ✅ 지도
      ],
    };
  },
};
</script>


<style>
#app {
  font-family: Avenir, Helvetica, Arial, sans-serif;
  text-align: center;
  color: #2c3e50;
  margin-top: 60px;
}

.tabs {
  display: flex;
  justify-content: center;
  gap: 20px;
  margin-bottom: 30px;
}

.tab-button {
  padding: 10px 20px;
  font-size: 16px;
  cursor: pointer;
  text-decoration: none;
  border: none;
  background: #ddd;
  border-radius: 5px;
  color: black;
}

.tab-button.active {
  background: #42b983;
  color: white;
  font-weight: bold;
}
</style>
