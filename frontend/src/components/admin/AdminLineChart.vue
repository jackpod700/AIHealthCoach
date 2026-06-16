<script setup>
import { computed } from "vue";

const props = defineProps({
  title: {
    type: String,
    required: true,
  },
  points: {
    type: Array,
    default: () => [],
  },
  series: {
    type: Array,
    default: () => [],
  },
  valueSuffix: {
    type: String,
    default: "",
  },
});

const chartWidth = 640;
const chartHeight = 220;
const paddingTop = 18;
const paddingRight = 20;
const paddingBottom = 28;
const paddingLeft = 62;

const hasEnoughData = computed(() => props.points.length >= 2 && props.series.length > 0);

const values = computed(() => {
  return props.series.flatMap((serie) =>
    props.points
      .map((point) => Number(point[serie.key]))
      .filter((value) => Number.isFinite(value))
  );
});

const maxValue = computed(() => Math.max(1, ...values.value));
const minValue = computed(() => Math.min(0, ...values.value));

const yTicks = computed(() => {
  const max = maxValue.value;
  const min = minValue.value;
  return [max, min + (max - min) * 0.75, (max + min) / 2, min + (max - min) * 0.25, min];
});

function x(index) {
  if (props.points.length <= 1) {
    return paddingLeft;
  }
  return paddingLeft + (index / (props.points.length - 1)) * (chartWidth - paddingLeft - paddingRight);
}

function y(value) {
  const range = maxValue.value - minValue.value || 1;
  return chartHeight - paddingBottom - ((value - minValue.value) / range) * (chartHeight - paddingTop - paddingBottom);
}

function pathFor(serie) {
  return props.points
    .map((point, index) => {
      const value = Number(point[serie.key]) || 0;
      return `${index === 0 ? "M" : "L"} ${x(index).toFixed(1)} ${y(value).toFixed(1)}`;
    })
    .join(" ");
}

function latestValue(serie) {
  const latest = props.points.at(-1);
  if (!latest) {
    return "-";
  }
  const value = latest[serie.key];
  if (value === null || value === undefined) {
    return "-";
  }
  return `${Number(value).toLocaleString("ko-KR", { maximumFractionDigits: 1 })}${props.valueSuffix}`;
}

function formatTick(value) {
  return `${Number(value).toLocaleString("ko-KR", { maximumFractionDigits: 1 })}${props.valueSuffix}`;
}
</script>

<template>
  <article class="admin-chart-card">
    <header>
      <div>
        <p class="section-eyebrow">HISTORY</p>
        <h2>{{ title }}</h2>
      </div>
      <div class="admin-chart-legend">
        <span v-for="serie in series" :key="serie.key">
          <i :style="{ backgroundColor: serie.color }"></i>
          {{ serie.label }} {{ latestValue(serie) }}
        </span>
      </div>
    </header>

    <div v-if="!hasEnoughData" class="admin-chart-empty">
      아직 그래프를 그릴 데이터가 부족해요.
    </div>

    <svg v-else class="admin-chart-svg" :viewBox="`0 0 ${chartWidth} ${chartHeight}`" role="img">
      <g class="admin-chart-guides">
        <line
          v-for="tick in yTicks"
          :key="tick"
          :x1="paddingLeft"
          :x2="chartWidth - paddingRight"
          :y1="y(tick)"
          :y2="y(tick)"
        />
      </g>
      <g class="admin-chart-axis">
        <text
          v-for="tick in yTicks"
          :key="`label-${tick}`"
          class="admin-chart-axis-label"
          :x="paddingLeft - 10"
          :y="y(tick) + 4"
          text-anchor="end"
        >
          {{ formatTick(tick) }}
        </text>
      </g>
      <path
        v-for="serie in series"
        :key="serie.key"
        class="admin-chart-line"
        :d="pathFor(serie)"
        :stroke="serie.color"
      />
    </svg>
  </article>
</template>
