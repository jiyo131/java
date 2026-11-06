import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  server: {
    host: true,
    port: 5173,
    open: true,
    proxy: {
      // /api 请求转发到 Spring Boot 后端。
      // 用 127.0.0.1 而非硬编码局域网 IP：vite 与后端在同一台机器上,
      // 代理转发由 vite 服务端发起, 回环地址永远可达, 本机 IP 变化(DHCP)不再影响。
      // 局域网设备照常通过 http://<本机IP>:5173 访问, 代理在服务端完成。
      '/api': {
        target: 'http://127.0.0.1:8081',
        changeOrigin: true
      }
    }
  }
})
