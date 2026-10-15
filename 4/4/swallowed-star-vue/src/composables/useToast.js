import { reactive } from 'vue'

// 全局单例 toast 状态
const state = reactive({ show: false, msg: '', type: '' })
let timer = null

export function useToast() {
  function show(msg, type = '') {
    state.msg = msg
    state.type = type
    state.show = true
    clearTimeout(timer)
    timer = setTimeout(() => { state.show = false }, 2600)
  }
  return { state, show }
}
