import { createRouter, createWebHistory } from 'vue-router';
import RideSummaryTable from '@/components/RideList.vue';
import RideDetail from '@/components/RideDetail.vue';
import FitUpload from '@/components/FitUpload.vue';

const routes = [
    {
        path: '/ride/:id',   // ✅ id 기반으로 수정
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
    }
];

const router = createRouter({
    history: createWebHistory(),
    routes
});

export default router;
