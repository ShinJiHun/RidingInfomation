import { createRouter, createWebHistory } from 'vue-router';
import RideSummaryTable from '@/components/GarminTable.vue';
import RideDetail from '@/components/RideDetail.vue';
import FitUpload from '@/components/FitUpload.vue'; // ✅ 추가

const routes = [
    {
        path: '/detail/:filename',   // ✅ 먼저 선언 (더 구체적인 경로가 항상 위)
        name: 'RideDetail',
        component: RideDetail
    },
    {
        path: '/upload',
        name: 'FitUpload',
        component: FitUpload
    },
    {
        path: '/',
        name: 'Home',
        component: RideSummaryTable
    },
];


const router = createRouter({
    history: createWebHistory(),
    routes
});

export default router;
