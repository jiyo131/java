# 吞噬星空 · 前端（Vue 3 + Vite）

星域登录 / 注册页面，深空星云风格，对接 `springBootTest` 后端。

## 技术栈

- Vue 3 + vue-router 4
- Vite 5
- 无重型 UI 库：手写玻璃拟态面板 + Canvas 星空粒子 / 流星 / 图形验证码
- ESLint 9（flat config）+ Prettier 3

## 快速开始

```bash
npm install
npm run dev      # http://localhost:5173
```

开发服务器已配置 `/api` 代理到 `http://127.0.0.1:8081`（后端地址），
因此前端直接请求 `/api/...` 即可，无需处理跨域。

## 脚本

| 命令               | 说明                                  |
| ------------------ | ------------------------------------- |
| `npm run dev`      | 启动开发服务器（端口 5173）            |
| `npm run build`    | 生产构建，产物在 `dist/`               |
| `npm run preview`  | 本地预览构建产物                       |
| `npm run lint`     | ESLint 检查并自动修复                  |
| `npm run format`   | Prettier 格式化 `src`                 |

## 页面与字段

- `/login` — 登录：星域账号 + 星核密钥（密码）+ 记住我（**无**图形验证码）
- `/register` — 注册：账号 / 密码 / 确定密码 / 图形验证码（Canvas，点击刷新）
- `/home` — 星域主页（需登录，路由守卫校验 token）

## 目录结构

```
src/
├─ api/auth.js           后端认证请求封装（login/register/me/logout）
├─ composables/
│  ├─ useAuth.js         登录态管理（localStorage + token）
│  └─ useToast.js        全局单例提示
├─ components/
│  ├─ Starfield.vue      Canvas 星空粒子 + 流星
│  ├─ Captcha.vue        Canvas 图形验证码（暴露 verify / refresh）
│  └─ Toast.vue
├─ views/
│  ├─ Login.vue
│  ├─ Register.vue
│  └─ Home.vue
├─ router/index.js       路由 + 登录守卫
├─ App.vue               全局背景 + 网格装饰 + <router-view> + 全局 Toast
├─ style.css             全局深空样式
└─ main.js               挂载 + 注册路由
public/bg.jpeg           背景图（罗峰立绘）
```

## 演示账号

`admin / 123456`（由后端首次启动时自动创建）。
本地需先启动后端并完成建库（执行后端 `star_user.sql`）。
