<template>
  <canvas ref="cv" class="captcha-canvas" title="点击刷新验证码" @click="refresh"></canvas>
</template>

<script setup>
import { onMounted, ref } from 'vue'

const CHARS = 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789' // 剔除易混淆字符
const COLORS = ['#ff9eb5', '#ffd479', '#7fe7c9', '#ff5c7a', '#e09bff']
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
  g.addColorStop(0, '#3a1024')
  g.addColorStop(1, '#5a1a30')
  ctx.fillStyle = g
  ctx.fillRect(0, 0, W, H)

  for (let i = 0; i < 30; i++) {
    ctx.fillStyle = 'rgba(255,255,255,' + rnd(0.12, 0.7) + ')'
    ctx.fillRect(rnd(0, W), rnd(0, H), 1.3, 1.3)
  }
  for (let i = 0; i < 5; i++) {
    ctx.strokeStyle = 'rgba(' + rndInt(220, 255) + ',' + rndInt(90, 170) + ',' + rndInt(130, 200) + ',' + rnd(0.12, 0.4) + ')'
    ctx.lineWidth = rnd(0.6, 1.6)
    ctx.beginPath()
    ctx.moveTo(rnd(0, W), rnd(0, H))
    ctx.bezierCurveTo(rnd(0, W), rnd(0, H), rnd(0, W), rnd(0, H), rnd(0, W), rnd(0, H))
    ctx.stroke()
  }

  // 自适应排布：按实测字符宽度计算间距与字号，保证字符（含发光）不超出画布
  const PAD = 12
  const fontOf = (fs) => 'bold ' + fs + 'px "Segoe UI", system-ui, sans-serif'
  let fs = 26
  ctx.font = fontOf(fs)
  let widths = codeStr.split('').map((ch) => ctx.measureText(ch).width)
  const avail = W - PAD * 2
  let total = widths.reduce((a, b) => a + b, 0)
  if (total > avail) {
    fs = Math.max(15, Math.floor(fs * (avail / total)))
    ctx.font = fontOf(fs)
    widths = codeStr.split('').map((ch) => ctx.measureText(ch).width)
    total = widths.reduce((a, b) => a + b, 0)
  }
  const gap = (avail - total) / Math.max(codeStr.length - 1, 1)
  for (let i = 0; i < codeStr.length; i++) {
    const cx = PAD + widths.slice(0, i).reduce((a, b) => a + b, 0) + gap * i + widths[i] / 2
    ctx.save()
    ctx.translate(cx, H / 2 + rnd(-2, 2))
    ctx.rotate(rnd(-0.22, 0.22))
    ctx.font = fontOf(fs)
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
