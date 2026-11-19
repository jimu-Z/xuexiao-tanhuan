'use strict'
const path = require('path')

function resolve(dir) {
  return path.join(__dirname, dir)
}

const name = '学生谈心谈话系统'

const baseUrl = process.env.VUE_APP_PROXY_TARGET || 'http://127.0.0.1:6060'

module.exports = {
  publicPath: '/',
  outputDir: 'dist',
  assetsDir: 'static',
  productionSourceMap: false,
  devServer: {
    host: '0.0.0.0',
    port: 6090,
    open: true,
    proxy: {
      [process.env.VUE_APP_BASE_API]: {
        target: baseUrl,
        changeOrigin: true,
        pathRewrite: {
          ['^' + process.env.VUE_APP_BASE_API]: ''
        }
      },
      '^/ruoyi-system': {
        target: baseUrl,
        changeOrigin: true
      }
    },
    disableHostCheck: true
  },
  css: {
    loaderOptions: {
      sass: {
        sassOptions: { outputStyle: 'expanded' }
      }
    }
  },
  configureWebpack: {
    name: name,
    resolve: {
      alias: {
        '@': resolve('src')
      }
    }
  },
  chainWebpack(config) {
    config.plugins.delete('preload')
    config.plugins.delete('prefetch')
  }
}
