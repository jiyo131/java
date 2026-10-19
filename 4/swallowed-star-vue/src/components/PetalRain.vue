<template>
  <canvas id="petals" ref="cv"></canvas>
</template>

<script setup>
import { onMounted, onBeforeUnmount, ref } from 'vue'

const cv = ref(null)
let rafId = 0
let sparkTimer = 0
let removeResize = null

function init(canvas) {
  const ctx = canvas.getContext('2d')
  let w, h, dpr, petals = [], sparks = []
  const rnd = (a, b) => a + Math.random() * (b - a)
  const PETAL_HUES = [
    '255,158,181', // 桃花粉
    '255,92,122',  // 玫瑰红
    '255,217,160', // 暖金
    '255,238,242'  // 暖白
  ]

  function initPetals() {
    const count = Math.round((window.innerWidth * window.innerHeight) / 16000)
    petals = []
    for (let i = 0; i < count; i++) {
      petals.push({
        x: Math.random() * w,
        y: Math.random() * h,
        size: (Math.random() * 6 + 4) * dpr,
        vy: (Math.random() * 0.6 + 0.25) * dpr,
        sway: Math.random() * Math.PI * 2,
        swaySpeed: Math.random() * 0.012 + 0.004,
        swayAmp: (Math.random() * 30 + 14) * dpr,
        rot: Math.random() * Math.PI * 2,
        rotSpeed: (Math.random() - 0.5) * 0.02,
        hue: PETAL_HUES[Math.floor(Math.random() * PETAL_HUES.length)],
        alpha: Math.random() * 0.35 + 0.4
      })
    }
  }

  function resize() {
    dpr = Math.min(window.devicePixelRatio || 1, 2)
    w = canvas.width = window.innerWidth * dpr
    h = canvas.height = window.innerHeight * dpr
    canvas.style.width = window.innerWidth + 'px'
    canvas.style.height = window.innerHeight + 'px'
    initPetals()
  }

  // 萤火光点（苦情树萤光）
  function spawnSpark() {
    sparks.push({
      x: Math.random() * w,
      y: Math.random() * h * 0.85 + h * 0.1,
      r: (Math.random() * 1.8 + 0.8) * dpr,
      vx: (Math.random() - 0.5) * 0.4 * dpr,
      vy: -(Math.random() * 0.35 + 0.1) * dpr,
      life: 1,
      decay: Math.random() * 0.006 + 0.003,
      hue: Math.random() < 0.5 ? '255,217,160' : '255,158,181'
    })
  }

  function drawPetal(p) {
    ctx.save()
    ctx.translate(p.x, p.y)
    ctx.rotate(p.rot)
    ctx.beginPath()
    // 花瓣形：两条贝塞尔曲线勾出叶状轮廓
    ctx.moveTo(0, -p.size)
    ctx.bezierCurveTo(p.size * 0.9, -p.size * 0.4, p.size * 0.7, p.size * 0.6, 0, p.size)
    ctx.bezierCurveTo(-p.size * 0.7, p.size * 0.6, -p.size * 0.9, -p.size * 0.4, 0, -p.size)
    ctx.closePath()
    const grad = ctx.createLinearGradient(0, -p.size, 0, p.size)
    grad.addColorStop(0, 'rgba(' + p.hue + ',' + p.alpha + ')')
    grad.addColorStop(1, 'rgba(' + p.hue + ',' + (p.alpha * 0.45) + ')')
    ctx.fillStyle = grad
    ctx.shadowColor = 'rgba(' + p.hue + ',0.35)'
    ctx.shadowBlur = 6 * dpr
    ctx.fill()
    ctx.restore()
  }

  function draw() {
    ctx.clearRect(0, 0, w, h)

    for (const p of petals) {
      p.sway += p.swaySpeed
      p.x += Math.sin(p.sway) * (p.swayAmp * 0.012)
      p.y += p.vy
      p.rot += p.rotSpeed
      if (p.y > h + 20) { p.y = -20; p.x = Math.random() * w }
      if (p.x < -30) p.x = w + 30
      if (p.x > w + 30) p.x = -30
      drawPetal(p)
    }

    for (let i = sparks.length - 1; i >= 0; i--) {
      const s = sparks[i]
      s.x += s.vx; s.y += s.vy; s.life -= s.decay
      const a = Math.max(s.life, 0) * 0.85
      ctx.beginPath()
      ctx.fillStyle = 'rgba(' + s.hue + ',' + a + ')'
      ctx.shadowColor = 'rgba(' + s.hue + ',' + a + ')'
      ctx.shadowBlur = 10 * dpr
      ctx.arc(s.x, s.y, s.r, 0, Math.PI * 2)
      ctx.fill()
      ctx.shadowBlur = 0
      if (s.life <= 0 || s.y < -20) sparks.splice(i, 1)
    }

    rafId = requestAnimationFrame(draw)
  }

  sparkTimer = setInterval(() => { if (Math.random() < 0.7) spawnSpark() }, 900)
  window.addEventListener('resize', resize)
  removeResize = () => window.removeEventListener('resize', resize)
  resize()
  draw()
}

onMounted(() => { init(cv.value) })
onBeforeUnmount(() => {
  cancelAnimationFrame(rafId)
  clearInterval(sparkTimer)
  if (removeResize) removeResize()
})
</script>
