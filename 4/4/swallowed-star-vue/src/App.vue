<template>
  <Starfield />
  <div class="cosmos-deco"></div>
  <router-view />
  <Toast />
</template>

<script setup>
import Starfield from './components/Starfield.vue'
import Toast from './components/Toast.vue'
</script>
