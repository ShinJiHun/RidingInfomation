// src/router.js
import { createRouter, createWebHistory } from 'vue-router';
import GarminTable from './components/GarminTable.vue';
import GarminMap from './components/GarminMap.vue';
import RideDetail from './components/RideDetail.vue'; // ✅ 반드시 존재해야 함

const routes = [
    { path: '/', name: 'GarminTable', component: GarminTable },
    { path: '/map', name: 'GarminMap', component: GarminMap },
    { path: '/ride/:filename', name: 'RideDetail', component: RideDetail, props: true } // ✅ 여기도 포함되어야 함
];

const router = createRouter({
    history: createWebHistory(),
    routes
});

export default router;
