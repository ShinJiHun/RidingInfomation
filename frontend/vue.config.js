module.exports = {
  devServer: {
    port: 9090,
    proxy: {
      '/api': {
        target: 'http://localhost:9090',
        changeOrigin: true
      }
    }
  }
};
