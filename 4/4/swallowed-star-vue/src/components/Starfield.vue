<template>
  <canvas id="starfield" ref="cv"></canvas>
</template>

<script setup>
import { onMounted, onBeforeUnmount, ref } from 'vue'

const cv = ref(null)
let rafId = 0
let meteorTimer = 0
let removeResize = null

function init(canvas) {
  const ctx = canvas.getContext('2d')
  let w, h, dpr, stars = [], meteors = []
  const rnd = (a, b) => a + Math.random() * (b - a)
  const rndInt = (a, b) => Math.floor(rnd(a, b + 1))

  function initStars() {
    const count = Math.round((window.innerWidth * window.innerHeight) / 7000)
    stars = []
    for (let i = 0; i < count; i++) {
      stars.push({
        x: Math.random() * w,
        y: Math.random() * h,
        r: (Math.random() * 1.3 + 0.3) * dpr,
        base: Math.random() * 0.5 + 0.3,
        tw: Math.random() * Math.PI * 2,
        tws: Math.random() * 0.02 + 0.005,
        vy: (Math.random() * 0.15 + 0.03) * dpr,
        hue: Math.random() < 0.2 ? 'rgba(255,212,121,'
          : (Math.random() < 0.4 ? 'rgba(102,224,255,' : 'rgba(234,240,255,')
      })
    }
  }

  function resize() {
    dpr = Math.min(window.devicePixelRatio || 1, 2)
    w = canvas.width = window.innerWidth * dpr
    h = canvas.height = window.innerHeight * dpr
    canvas.style.width = window.innerWidth + 'px'
    canvas.style.height = window.innerHeight + 'px'
    initStars()
  }

  function spawnMeteor() {
    const fromTop = Math.random() < 0.5
    meteors.push({
      x: Math.random() * w * 0.8 + w * 0.1,
      y: fromTop ? -40 * dpr : Math.random() * h * 0.4,
      len: (Math.random() * 120 + 80) * dpr,
      vx: -(Math.random() * 3 + 3) * dpr,
      vy: (Math.random() * 2 + 2) * dpr,
      life: 1,
      hue: Math.random() < 0.5 ? '255,212,121' : '102,224,255'
    })
  }

  function draw() {
    ctx.clearRect(0, 0, w, h)
    for (const s of stars) {
      s.tw += s.tws
      const a = s.base + Math.sin(s.tw) * 0.3
      s.y += s.vy
      if (s.y > h) { s.y = 0; s.x = Math.random() * w }
      ctx.beginPath()
      ctx.fillStyle = s.hue + Math.max(a, 0.05) + ')'
      ctx.arc(s.x, s.y, s.r, 0, Math.PI * 2)
      ctx.fill()
    }
    for (let i = meteors.length - 1; i >= 0; i--) {
      const m = meteors[i]
      m.x += m.vx; m.y += m.vy; m.life -= 0.012
      const sp = Math.hypot(m.vx, m.vy)
      const tailX = m.x - m.vx / sp * m.len
      const tailY = m.y - m.vy / sp * m.len
      const grad = ctx.createLinearGradient(m.x, m.y, tailX, tailY)
      grad.addColorStop(0, 'rgba(' + m.hue + ',' + m.life + ')')
      grad.addColorStop(1, 'rgba(' + m.hue + ',0)')
      ctx.strokeStyle = grad
      ctx.lineWidth = 2 * dpr
      ctx.beginPath()
      ctx.moveTo(m.x, m.y)
      ctx.lineTo(tailX, tailY)
      ctx.stroke()
      if (m.life <= 0 || m.x < -50 || m.y > h + 50) meteors.splice(i, 1)
    }
    rafId = requestAnimationFrame(draw)
  }

  meteorTimer = setInterval(() => { if (Math.random() < 0.6) spawnMeteor() }, 2200)
  window.addEventListener('resize', resize)
  removeResize = () => window.removeEventListener('resize', resize)
  resize()
  draw()
}

onMounted(() => { init(cv.value) })
onBeforeUnmount(() => {
  cancelAnimationFrame(rafId)
  clearInterval(meteorTimer)
  if (removeResize) removeResize()
})
</script>
