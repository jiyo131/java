<template>
  <canvas ref="cv" class="captcha-canvas" title="点击刷新验证码" @click="refresh"></canvas>
</template>

<script setup>
import { onMounted, ref } from 'vue'

const CHARS = 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789' // 剔除易混淆字符
const COLORS = ['#ffd479', '#7fe7ff', '#c9a3ff', '#8affc1', '#ffb1e0']
const W = 132, H = 46

const rnd = (a, b) => a + Math.random() * (b - a)
const rndInt = (a, b) => Math.floor(rnd(a, b + 1))
const pick = (s) => s[rndInt(0, s.length - 1)]

const cv = ref(null)
const code = ref('')

function draw(codeStr) {
  const canvas = cv.value
  if (!canvas) return
  const ctx = canvas.getContext('2d')
  const dpr = Math.min(window.devicePixelRatio || 1, 2)
  canvas.width = W * dpr
  canvas.height = H * dpr
  canvas.style.width = W + 'px'
  canvas.style.height = H + 'px'
  ctx.setTransform(dpr, 0, 0, dpr, 0, 0)
  ctx.clearRect(0, 0, W, H)

  const g = ctx.createLinearGradient(0, 0, W, H)
  g.addColorStop(0, '#0b1230')
  g.addColorStop(1, '#1a1042')
  ctx.fillStyle = g
  ctx.fillRect(0, 0, W, H)

  for (let i = 0; i < 30; i++) {
    ctx.fillStyle = 'rgba(255,255,255,' + rnd(0.12, 0.7) + ')'
    ctx.fillRect(rnd(0, W), rnd(0, H), 1.3, 1.3)
  }
  for (let i = 0; i < 5; i++) {
    ctx.strokeStyle = 'rgba(' + rndInt(80, 200) + ',' + rndInt(140, 255) + ',255,' + rnd(0.12, 0.4) + ')'
    ctx.lineWidth = rnd(0.6, 1.6)
    ctx.beginPath()
    ctx.moveTo(rnd(0, W), rnd(0, H))
    ctx.bezierCurveTo(rnd(0, W), rnd(0, H), rnd(0, W), rnd(0, H), rnd(0, W), rnd(0, H))
    ctx.stroke()
  }

  const startX = 20
  const gap = (W - startX * 2) / (codeStr.length - 1)
  for (let i = 0; i < codeStr.length; i++) {
    ctx.save()
    ctx.translate(startX + i * gap, H / 2 + rnd(-3, 3))
    ctx.rotate(rnd(-0.34, 0.34))
    ctx.font = 'bold ' + rndInt(24, 30) + 'px "Segoe UI", system-ui, sans-serif'
    ctx.textAlign = 'center'
    ctx.textBaseline = 'middle'
    const color = pick(COLORS)
    ctx.fillStyle = color
    ctx.shadowColor = color
    ctx.shadowBlur = 8
    ctx.fillText(codeStr[i], 0, 0)
    ctx.restore()
  }

  for (let i = 0; i < 46; i++) {
    ctx.fillStyle = 'rgba(255,255,255,' + rnd(0.05, 0.22) + ')'
    ctx.fillRect(rnd(0, W), rnd(0, H), 1, 1)
  }
}

function refresh() {
  let c = ''
  for (let i = 0; i < 4; i++) c += pick(CHARS)
  code.value = c.toLowerCase()
  draw(c)
}

function verify(v) {
  return String(v || '').trim().toLowerCase() === code.value
}

onMounted(refresh)
defineExpose({ refresh, verify })
</script>
