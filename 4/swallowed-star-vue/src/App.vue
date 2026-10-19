<template>
  <PetalRain />
  <div class="blossom-deco"></div>
  <router-view />
  <Toast />
</template>

<script setup>
import PetalRain from './components/PetalRain.vue'
import Toast from './components/Toast.vue'
</script>
