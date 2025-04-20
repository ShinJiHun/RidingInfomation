import { createApp } from 'vue';
import App from './App.vue';
import router from './router'; // 👈 추가

const app = createApp(App);
app.use(router); // 👈 필수
app.mount('#app');
