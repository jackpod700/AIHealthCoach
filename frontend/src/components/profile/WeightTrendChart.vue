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
  selectedRecordDate: {
    type: String,
    default: "",
  },
});

const emit = defineEmits(["selectRecord"]);

const sortedRecords = computed(() =>
  [...props.records].sort((a, b) => a.recordDate.localeCompare(b.recordDate)),
);

const chartData = computed(() => {
  const targetWeight = Number(props.targetWeightKg);
  const hasTargetWeight = Number.isFinite(targetWeight);

  return {
    labels: sortedRecords.value.map((record) => formatShortDate(record.recordDate)),
    datasets: [
      {
        label: "몸무게",
        data: sortedRecords.value.map((record) => Number(record.weightKg)),
        borderColor: "#2f6b4d",
        backgroundColor: "rgba(90, 158, 120, 0.18)",
        pointBackgroundColor: sortedRecords.value.map((record) =>
          record.recordDate === props.selectedRecordDate ? "#e8814a" : "#4e8a66",
        ),
        pointBorderColor: "#fff",
        pointBorderWidth: 2,
        pointRadius: sortedRecords.value.map((record) =>
          record.recordDate === props.selectedRecordDate ? 6 : 5,
        ),
        pointHoverRadius: 7,
        pointHitRadius: 12,
        borderWidth: 3,
        fill: true,
        tension: 0.28,
      },
      ...(hasTargetWeight
        ? [
            {
              label: "목표 몸무게",
              data: sortedRecords.value.map(() => targetWeight),
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

const yAxisBounds = computed(() => {
  const targetWeight = Number(props.targetWeightKg);
  const values = sortedRecords.value
    .map((record) => Number(record.weightKg))
    .filter(Number.isFinite);

  if (Number.isFinite(targetWeight)) {
    values.push(targetWeight);
  }

  if (!values.length) {
    return {};
  }

  const min = Math.min(...values);
  const max = Math.max(...values);
  const range = Math.max(max - min, 1);
  const padding = Math.max(range * 0.22, 2);

  return {
    suggestedMin: Math.max(Math.floor((min - padding) * 2) / 2, 0),
    suggestedMax: Math.ceil((max + padding) * 2) / 2,
  };
});

const chartOptions = computed(() => ({
  responsive: true,
  maintainAspectRatio: false,
  layout: {
    padding: {
      top: 8,
      right: 12,
      bottom: 14,
      left: 8,
    },
  },
  onClick(_event, elements) {
    const point = elements.find((element) => element.datasetIndex === 0);

    if (!point) {
      return;
    }

    const record = sortedRecords.value[point.index];

    if (record) {
      emit("selectRecord", record, {
        x: point.element.x,
        y: point.element.y,
      });
    }
  },
  onHover(event, elements) {
    if (event.native?.target) {
      event.native.target.style.cursor = elements.length ? "pointer" : "default";
    }
  },
  interaction: {
    intersect: false,
    mode: "index",
  },
  plugins: {
    legend: {
      display: false,
    },
    tooltip: {
      displayColors: false,
      backgroundColor: "#2a2e2b",
      padding: 12,
      titleFont: {
        size: 12,
        weight: 800,
      },
      bodyFont: {
        size: 12,
        weight: 700,
      },
      callbacks: {
        title(items) {
          return items[0]?.label || "";
        },
        label(context) {
          return `${context.dataset.label}: ${Number(context.parsed.y).toFixed(1)}kg`;
        },
      },
    },
  },
  scales: {
    x: {
      border: {
        display: false,
      },
      grid: {
        display: false,
      },
      ticks: {
        color: "#a7aca4",
        padding: 8,
        font: {
          size: 12,
          weight: 600,
        },
      },
    },
    y: {
      beginAtZero: false,
      ...yAxisBounds.value,
      border: {
        display: false,
      },
      grid: {
        color: "#eff1ed",
        borderDash: [4, 4],
        drawTicks: false,
      },
      ticks: {
        color: "#a7aca4",
        padding: 10,
        callback(value) {
          return `${value}kg`;
        },
        font: {
          size: 12,
          weight: 600,
        },
      },
    },
  },
}));

function formatShortDate(dateKey) {
  const [, month, day] = dateKey.split("-");
  return `${Number(month)}/${day}`;
}
</script>

<template>
  <div class="weight-chart-frame">
    <div v-if="records.length" class="weight-chart-canvas">
      <Line
        :data="chartData"
        :options="chartOptions"
      />
    </div>
    <div v-else class="weight-chart-empty">
      <strong>아직 기록이 없습니다.</strong>
      <span>첫 몸무게를 입력하면 추세 그래프가 나타납니다.</span>
    </div>
    <div v-if="records.length" class="weight-chart-legend" aria-hidden="true">
      <span>
        <i class="weight-line-sample"></i>
        기록된 체중
      </span>
      <span v-if="targetWeightKg">
        <i class="target-line-sample"></i>
        목표 체중
      </span>
    </div>
  </div>
</template>
