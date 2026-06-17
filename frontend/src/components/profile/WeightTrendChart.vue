<script setup>
import {
  CategoryScale,
  Chart as ChartJS,
  Filler,
  Legend,
  LineElement,
  LinearScale,
  PointElement,
  Tooltip,
} from "chart.js";
import { computed } from "vue";
import { Line } from "vue-chartjs";

ChartJS.register(CategoryScale, LinearScale, PointElement, LineElement, Filler, Tooltip, Legend);

const props = defineProps({
  records: {
    type: Array,
    default: () => [],
  },
  targetWeightKg: {
    type: [Number, String],
    default: null,
  },
});

const chartData = computed(() => {
  const sortedRecords = [...props.records].sort((a, b) => a.recordDate.localeCompare(b.recordDate));
  const targetWeight = Number(props.targetWeightKg);
  const hasTargetWeight = Number.isFinite(targetWeight);

  return {
    labels: sortedRecords.map((record) => formatShortDate(record.recordDate)),
    datasets: [
      {
        label: "몸무게",
        data: sortedRecords.map((record) => Number(record.weightKg)),
        borderColor: "#2f4a3c",
        backgroundColor: "rgba(47, 74, 60, 0.12)",
        pointBackgroundColor: "#2f4a3c",
        pointBorderColor: "#fff",
        pointBorderWidth: 2,
        pointRadius: 4,
        pointHoverRadius: 6,
        borderWidth: 3,
        fill: true,
        tension: 0.35,
      },
      ...(hasTargetWeight
        ? [
            {
              label: "목표 몸무게",
              data: sortedRecords.map(() => targetWeight),
              borderColor: "#d9805a",
              borderDash: [6, 6],
              pointRadius: 0,
              borderWidth: 2,
              fill: false,
              tension: 0,
            },
          ]
        : []),
    ],
  };
});

const chartOptions = computed(() => ({
  responsive: true,
  maintainAspectRatio: false,
  interaction: {
    intersect: false,
    mode: "index",
  },
  plugins: {
    legend: {
      position: "bottom",
      labels: {
        boxWidth: 10,
        boxHeight: 10,
        color: "#6f7c8d",
        font: {
          size: 11,
          weight: 700,
        },
      },
    },
    tooltip: {
      callbacks: {
        label(context) {
          return `${context.dataset.label}: ${Number(context.parsed.y).toFixed(1)}kg`;
        },
      },
    },
  },
  scales: {
    x: {
      grid: {
        display: false,
      },
      ticks: {
        color: "#8b96a4",
        font: {
          size: 11,
          weight: 700,
        },
      },
    },
    y: {
      beginAtZero: false,
      grid: {
        color: "rgba(234, 223, 206, 0.72)",
      },
      ticks: {
        color: "#8b96a4",
        callback(value) {
          return `${value}kg`;
        },
        font: {
          size: 11,
          weight: 700,
        },
      },
    },
  },
}));

function formatShortDate(dateKey) {
  const [, month, day] = dateKey.split("-");
  return `${Number(month)}/${Number(day)}`;
}
</script>

<template>
  <div class="weight-chart-frame">
    <Line v-if="records.length" :data="chartData" :options="chartOptions" />
    <div v-else class="weight-chart-empty">
      <strong>아직 기록이 없습니다.</strong>
      <span>첫 몸무게를 입력하면 추세 그래프가 나타납니다.</span>
    </div>
  </div>
</template>
