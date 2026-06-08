<template>
  <div class="chart-container relative w-full h-[200px] max-h-[250px]">
    <canvas ref="canvasRef"></canvas>
  </div>
</template>

<script setup>
import { ref, onMounted, watch, onUnmounted } from 'vue'
import { Chart, LineController, LineElement, PointElement, LinearScale, CategoryScale, Filler, Tooltip } from 'chart.js'

Chart.register(LineController, LineElement, PointElement, LinearScale, CategoryScale, Filler, Tooltip)

const props = defineProps({ data: { type: Array, default: () => [] } })
const canvasRef = ref(null)
let chart = null

function buildChart() {
  if (!canvasRef.value) return
  const isDark = document.documentElement.classList.contains('dark')
  const labels = props.data.map(d => d.recordDate?.slice(5) || '')
  const values = props.data.map(d => d.currentWeight)
  const gridColor = isDark ? 'rgba(148,163,184,0.08)' : 'rgba(148,163,184,0.15)'

  if (chart) chart.destroy()
  chart = new Chart(canvasRef.value, {
    type: 'line',
    data: {
      labels: labels.length ? labels : ['暂无'],
      datasets: [{
        data: values.length ? values : [0],
        borderColor: '#22c55e', borderWidth: 3, fill: true,
        backgroundColor: 'rgba(34,197,94,0.1)', tension: 0.4,
        pointBackgroundColor: '#ffffff', pointBorderColor: '#22c55e',
        pointBorderWidth: 2, pointRadius: 3, pointHoverRadius: 5,
      }]
    },
    options: {
      responsive: true, maintainAspectRatio: false,
      plugins: { legend: { display: false }, tooltip: { backgroundColor: isDark ? '#1e293b' : '#0f172a', padding: 10, displayColors: false, callbacks: { label: ctx => ctx.parsed.y + ' kg' } } },
      scales: {
        x: { grid: { display: false }, ticks: { font: { size: 11 }, color: '#94a3b8' }, border: { display: false } },
        y: { grid: { color: gridColor, drawBorder: false }, ticks: { display: false }, border: { display: false } }
      },
      interaction: { mode: 'index', intersect: false },
    }
  })
}

onMounted(() => buildChart())
watch(() => props.data, () => buildChart(), { deep: true })
onUnmounted(() => { if (chart) chart.destroy() })
</script>
