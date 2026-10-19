<template>
  <div class="toast" :class="{ show: state.show, ok: state.type === 'ok', err: state.type === 'err' }">
    {{ state.msg }}
  </div>
</template>

<script setup>
import { useToast } from '../composables/useToast.js'
const { state } = useToast()
</script>
