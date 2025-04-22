const path = require('path');

module.exports = {
  outputDir: path.resolve(__dirname, '../src/main/resources/static'),
  devServer: {
    port: 8080,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      }
    }
  }
};
