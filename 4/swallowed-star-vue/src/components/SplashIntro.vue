<template>
  <div class="splash" :class="{ leaving }" @click="finish" aria-hidden="true">
    <!-- 底层：暗幕 + 月晕 -->
    <div class="splash-vignette"></div>
    <div class="splash-moon"></div>

    <!-- 飘落桃花瓣 -->
    <span v-for="i in 7" :key="'p' + i" class="splash-petal" :style="petalStyle(i)"></span>

    <!-- 中央：红线牵引动画 -->
    <div class="splash-stage">
      <svg class="splash-svg" viewBox="0 0 680 300" fill="none">
        <!-- 月晕中心光点 -->
        <defs>
          <radialGradient id="spHeartGlow" cx="50%" cy="50%" r="50%">
            <stop offset="0%" stop-color="rgba(255,92,122,.85)"/>
            <stop offset="45%" stop-color="rgba(255,158,181,.35)"/>
            <stop offset="100%" stop-color="rgba(255,92,122,0)"/>
          </radialGradient>
        </defs>
        <circle class="sp-glow" cx="340" cy="150" r="90" fill="url(#spHeartGlow)"/>

        <!-- 左侧红线（凡人） -->
        <path class="sp-thread sp-left" pathLength="1"
              d="M -520 430 C -150 410, 150 258, 300 142 C 312 134, 318 128, 322 124"/>
        <!-- 右侧红线（妖） -->
        <path class="sp-thread sp-right" pathLength="1"
              d="M 1200 430 C 830 410, 530 258, 380 142 C 368 134, 362 128, 358 124"/>
        <!-- 红线牵结 · 桃心 -->
        <path class="sp-heart" pathLength="1"
              d="M 340 172 C 340 172, 312 150, 312 130 C 312 114, 328 106, 338 115 C 340 117, 340 121, 340 121 C 340 121, 340 117, 342 115 C 352 106, 368 114, 368 130 C 368 150, 340 172, 340 172 Z"/>
        <!-- 牵结光点 -->
        <circle class="sp-spark" cx="340" cy="121" r="4"/>
      </svg>

      <h1 class="splash-title">
        <span v-for="(ch, i) in title" :key="i" :style="{ animationDelay: (1.9 + i * 0.09) + 's' }">{{ ch }}</span>
      </h1>
      <p class="splash-sub">{{ subtitle }}</p>

      <!-- 台词：赌上全部妖力的约定 -->
      <div class="splash-quote" aria-hidden="true">
        <p class="q-line" style="--d: 3.0s">「 这一世，我赌上全部妖力</p>
        <p class="q-line" style="--d: 3.7s">只为与你来世相见 」</p>
      </div>
    </div>

    <span class="splash-skip">点 击 任 意 处 跳 过</span>
  </div>
</template>

<script setup>
import { ref, onBeforeUnmount } from 'vue'

// 标题/副标题可按场景覆盖：登录页开屏用默认值，登录成功过场可传自定义副标题
defineProps({
  title: { type: String, default: '狐妖小红娘' },
  subtitle: { type: String, default: '涂 山 红 线 · 月 老 认 证 入 口' }
})

const emit = defineEmits(['done'])
const leaving = ref(false)
const timers = []

// 总时长约 5.8s 自动收场（台词需要阅读时间）；点击可立即跳过
timers.push(setTimeout(finish, 5800))

function finish() {
  if (leaving.value) return
  leaving.value = true
  timers.push(setTimeout(() => emit('done'), 680)) // 与 splashOut 动画时长一致
}

function petalStyle(i) {
  return {
    left: (6 + i * 13) % 96 + '%',
    animationDelay: (i * 0.45) + 's',
    animationDuration: (3.4 + (i % 3) * 0.7) + 's',
    transform: `scale(${0.7 + (i % 4) * 0.16})`
  }
}

onBeforeUnmount(() => timers.forEach(clearTimeout))
</script>

<style scoped>
.splash {
  position: fixed;
  inset: 0;
  z-index: 60;
  overflow: hidden;
  cursor: pointer;
  display: grid;
  place-items: center;
  background:
    radial-gradient(90% 70% at 50% 42%, rgba(255,158,181,.06), transparent 62%),
    linear-gradient(180deg, rgba(22,9,16,.93) 0%, rgba(26,10,18,.95) 55%, rgba(28,13,22,.92) 100%),
    url("/bg-huyao.jpeg") center 36% / cover no-repeat #180a12;
  animation: splashIn .5s ease both;
}
@keyframes splashIn { from { opacity: 0 } to { opacity: 1 } }
.splash.leaving { animation: splashOut .68s cubic-bezier(.4,0,.6,1) forwards; }
@keyframes splashOut {
  to { opacity: 0; transform: scale(1.06); filter: blur(4px); }
}

/* 暗幕四周收边，聚焦中央 */
.splash-vignette {
  position: absolute; inset: 0; pointer-events: none;
  background: radial-gradient(120% 90% at 50% 46%, transparent 42%, rgba(10,3,7,.72) 100%);
}
/* 中央月晕 */
.splash-moon {
  position: absolute; left: 50%; top: 44%;
  width: 340px; height: 340px; margin: -170px 0 0 -170px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(255,217,160,.14), rgba(255,158,181,.07) 45%, transparent 70%);
  animation: moonBreath 2.6s ease-in-out infinite;
}
@keyframes moonBreath { 50% { transform: scale(1.12); opacity: .8 } }

/* 桃花瓣 */
.splash-petal {
  position: absolute; top: -8%;
  width: 14px; height: 14px; pointer-events: none;
  background: radial-gradient(circle at 30% 30%, #ffd7e2, var(--pink, #ff9eb5) 55%, var(--rose, #ff5c7a));
  border-radius: 12px 2px 12px 2px;
  opacity: 0;
  animation: petalFall linear infinite;
}
@keyframes petalFall {
  0%   { transform: translateY(0) translateX(0) rotate(0deg); opacity: 0 }
  12%  { opacity: .9 }
  100% { transform: translateY(108vh) translateX(-9vw) rotate(320deg); opacity: .15 }
}

/* 舞台 */
.splash-stage { position: relative; z-index: 1; text-align: center; }
/* 文字底衬：让穿过标题的红线在此处自然隐去，保证可读性 */
.splash-stage::before {
  content: "";
  position: absolute;
  left: 50%; bottom: -4%;
  width: min(620px, 96vw); height: 240px;
  transform: translateX(-50%);
  pointer-events: none;
  z-index: -1;
  background: radial-gradient(62% 54% at 50% 50%, rgba(22,9,16,.82), rgba(22,9,16,.45) 46%, transparent 74%);
}
.splash-svg {
  width: min(680px, 92vw);
  height: auto;
  display: block;
  margin: 0 auto;
  overflow: visible; /* 红线延伸到视口之外，从画面两端牵入 */
}

/* 红线：从两侧绘出 */
.sp-thread {
  stroke: var(--rose, #ff5c7a);
  stroke-width: 2.4;
  stroke-linecap: round;
  stroke-dasharray: 1;
  stroke-dashoffset: 1;
  filter: drop-shadow(0 0 7px rgba(255,92,122,.65));
  animation: threadDraw 1.15s cubic-bezier(.55,.06,.3,.95) .25s both;
}
.sp-right { animation-delay: .35s; }
@keyframes threadDraw { to { stroke-dashoffset: 0 } }

/* 桃心牵结 */
.sp-heart {
  stroke: var(--pink, #ff9eb5);
  stroke-width: 2.6;
  stroke-linecap: round;
  fill: rgba(255,92,122,.16);
  stroke-dasharray: 1;
  stroke-dashoffset: 1;
  filter: drop-shadow(0 0 10px rgba(255,92,122,.75));
  animation: threadDraw .8s ease .42s both,
             heartBeat 1.6s ease-in-out 1.5s infinite;
  animation-delay: .42s, 1.6s;
}
@keyframes heartBeat {
  0%, 100% { transform: scale(1) }
  12% { transform: scale(1.09) }
  24% { transform: scale(1) }
}
.sp-heart { transform-origin: 340px 143px; }

/* 中央光晕呼吸 */
.sp-glow { animation: glowIn 1.2s ease .5s both, glowBreath 2.2s ease-in-out 1.7s infinite; }
@keyframes glowIn { from { opacity: 0 } to { opacity: 1 } }
@keyframes glowBreath { 50% { opacity: .55 } }

/* 牵结火花 */
.sp-spark {
  fill: #fff;
  filter: drop-shadow(0 0 8px rgba(255,217,160,.95));
  animation: sparkPop .5s cubic-bezier(.2,.8,.2,1) 1.35s both;
  transform-origin: 340px 121px;
}
@keyframes sparkPop {
  from { opacity: 0; transform: scale(0) }
  60% { opacity: 1; transform: scale(1.7) }
  to { opacity: .9; transform: scale(1) }
}

/* 标题：逐字点亮 */
.splash-title {
  margin-top: -34px;
  font-size: clamp(30px, 6vw, 44px);
  font-weight: 800;
  letter-spacing: 10px;
  text-indent: 10px;
}
.splash-title span {
  display: inline-block;
  opacity: 0;
  background: linear-gradient(90deg, #fff, var(--pink, #ff9eb5) 58%, var(--gold, #ffd9a0));
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
  filter: drop-shadow(0 0 16px rgba(255,158,181,.35));
  animation: charIn .55s cubic-bezier(.2,.8,.2,1) both;
}
@keyframes charIn {
  from { opacity: 0; transform: translateY(16px) scale(.86); filter: blur(5px) }
  to   { opacity: 1; transform: none; filter: blur(0) }
}

/* 副标题 */
.splash-sub {
  margin-top: 14px;
  font-size: 12px;
  letter-spacing: 6px;
  color: var(--muted, #c79aa9);
  opacity: 0;
  animation: subIn .8s ease 2.45s both;
}
@keyframes subIn {
  from { opacity: 0; letter-spacing: 14px }
  to   { opacity: .95; letter-spacing: 6px }
}

/* 台词：赌上全部妖力的约定 */
.splash-quote {
  position: relative;
  margin-top: 6px;
  padding-top: 18px;
}
/* 台词上方的红线分隔（用 margin 居中，避免动画的 transform 覆盖定位） */
.splash-quote::before {
  content: "";
  position: absolute;
  top: 0; left: 0; right: 0;
  margin: 0 auto;
  width: 170px; height: 1px;
  background: linear-gradient(90deg, transparent, rgba(255,92,122,.6), transparent);
  opacity: 0;
  animation: qFade 1s ease 2.85s both;
}
@keyframes qFade {
  from { opacity: 0 }
  to   { opacity: 1 }
}
.splash-quote .q-line {
  margin: 0;
  font-size: 15px;
  line-height: 2.05;
  letter-spacing: 3px;
  color: transparent;
  background: linear-gradient(90deg, #ffe6c7, var(--gold, #ffd9a0) 52%, var(--pink, #ff9eb5));
  -webkit-background-clip: text;
  background-clip: text;
  opacity: 0;
  animation: qIn 1.15s cubic-bezier(.2,.8,.2,1) var(--d) both;
}
@keyframes qIn {
  from { opacity: 0; transform: translateY(10px); filter: blur(6px) }
  to   { opacity: .95; transform: none; filter: blur(0) }
}

/* 跳过提示 */
.splash-skip {
  position: absolute;
  left: 50%; bottom: 5%;
  transform: translateX(-50%);
  font-size: 11px;
  letter-spacing: 3px;
  color: rgba(199,154,169,.75);
  opacity: 0;
  animation: subIn .8s ease 1.4s both;
}

@media (max-width: 600px) {
  .splash-title { letter-spacing: 6px; text-indent: 6px; font-size: 30px; }
  .splash-sub { letter-spacing: 3px; font-size: 11px; }
  .splash-stage::before { height: 230px; }
  .splash-quote .q-line { font-size: 13px; letter-spacing: 2px; line-height: 1.9; }
}

@media (prefers-reduced-motion: reduce) {
  .splash { animation-duration: .01s; }
}
</style>
