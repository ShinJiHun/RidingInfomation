<template>
  <div id="app">
    <div class="tabs">
      <button
        v-for="tab in tabs"
        :key="tab"
        :class="{ active: currentTab === tab }"
        @click="currentTab = tab"
      >
        {{ tab }}
      </button>
    </div>

    <component :is="currentTabComponent" />
  </div>
</template>

<script>
import HelloWorld from './components/HelloWorld.vue'
import GarminMap from './components/GarminMap.vue'

export default {
  name: 'App',
  components: {
    HelloWorld,
    GarminMap,
  },
  data() {
    return {
      tabs: ['HelloWorld', 'GarminMap'],
      currentTab: 'HelloWorld',
    };
  },
  computed: {
    currentTabComponent() {
      return this.currentTab;
    },
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

.tabs button {
  padding: 10px 20px;
  font-size: 16px;
  cursor: pointer;
  border: none;
  background: #ddd;
  border-radius: 5px;
}

.tabs button.active {
  background: #42b983;
  color: white;
  font-weight: bold;
}
</style>
