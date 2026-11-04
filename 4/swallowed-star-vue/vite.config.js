import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  server: {
    host: true,
    port: 5173,
    open: true,
    proxy: {
      // 开发环境把 /api 请求转发到 Spring Boot 后端（本机以太网 IPv4:8081）
      '/api': {
        target: 'http://192.168.68.72:8081',
        changeOrigin: true
      }
    }
  }
})
