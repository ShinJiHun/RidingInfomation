// vue.config.js
module.exports = {
  publicPath: './',        // 상대 경로로 지정 (jar 배포 시 필수)
  outputDir: 'dist',       // 빌드 결과물이 저장될 폴더
  assetsDir: '',           // 기본값 (js, css가 index.html과 같은 경로에 배치됨)
  productionSourceMap: false, // sourcemap 제거 (배포 최적화)

  // 개발용 서버 설정 (npm run serve 시만 적용됨)
  devServer: {
    port: 8080,
    proxy: {
      '/api': {
        target: 'http://localhost:9090', // 실제 Spring Boot 포트에 맞춰줌
        changeOrigin: true,
      },
    },
  },
};
