# 狐妖小红娘 · 前端（Vue 3 + Vite）

涂山红线登录 / 注册页面，苦情树桃花古风风格，对接 `springBootTest` 后端。

## 技术栈

- Vue 3 + vue-router 4
- Vite 5
- 无重型 UI 库：手写玻璃拟态面板 + Canvas 桃花瓣飘落 / 萤光粒子 / 图形验证码
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

- `/login` — 登录：涂山账号 + 姻缘密钥（密码）+ 记住我（**无**图形验证码）
- `/register` — 注册：账号 / 密码 / 确定密码 / 图形验证码（Canvas，点击刷新）
- `/home` — **涂山人物志（人物介绍首页）**：需登录，路由守卫校验 token

### 首页功能（2026-10-16）

| 模块 | 说明 |
| --- | --- |
| 导航栏 | 档案分类筛选（全体档案 / 人物 / 妖族 / 圣地 / 法宝 / 组织），带数量角标；窄屏改为横向滑动 |
| 搜索栏 | 按 姓名 / 称号 / 简介 / 分类 / 势力 / 标签 模糊检索，可与分类叠加；带 ✕ 一键清空 |
| 人物卡片 | 头像、姓名、称号、分类角标、简介、关键词标签、所属势力 |
| 人物志浮层 | 点击卡片查看 人物简介 / 关键词 / 经典台词 / 红线羁绊；Esc、✕、点遮罩均可关闭 |
| 顶栏右侧 | 实时时钟 + 登录用户 + 退出登录 |

> 首页目前为**纯前端演示数据**（`Home.vue` 内 `DATABASE` 常量，14 条）。
> 后端检索接口尚未实现、已预留：接入时只需把 `results` 的数据源换成接口返回即可。
> 「姻缘簿 / 妖族图鉴 / 妖馨斋 / 苦情修炼」等其他页面暂未开发，详情统一以浮层呈现。

## 目录结构

```
src/
├─ api/auth.js           后端认证请求封装（login/register/me/logout）
├─ composables/
│  ├─ useAuth.js         登录态管理（localStorage + token）
│  └─ useToast.js        全局单例提示
├─ components/
│  ├─ PetalRain.vue      Canvas 桃花瓣飘落（背景）
│  ├─ Captcha.vue        Canvas 图形验证码（暴露 verify / refresh）
│  └─ Toast.vue
├─ views/
│  ├─ Login.vue
│  ├─ Register.vue
│  └─ Home.vue           涂山人物志（人物介绍首页）
├─ router/index.js       路由 + 登录守卫 + 页面标题
├─ App.vue               全局背景 + 花瓣装饰 + <router-view> + 全局 Toast
├─ style.css             全局样式（登录页 + 人物志首页 + 弹窗）
└─ main.js               挂载 + 注册路由
public/bg-huyao.jpeg      背景图（body 背景）
```

## 演示账号

`admin / 123456`（由后端首次启动时自动创建）。
本地需先启动后端并完成建库（执行后端 `star_user.sql`）。
