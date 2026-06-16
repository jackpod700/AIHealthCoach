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
  valueKey: {
    type: String,
    required: true,
  },
  color: {
    type: String,
    default: "#d9805a",
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

const hasEnoughData = computed(() => props.points.length >= 2);
const maxValue = computed(() => {
  return Math.max(1, ...props.points.map((point) => Number(point[props.valueKey]) || 0));
});

const yTicks = computed(() => [maxValue.value, maxValue.value / 2, 0]);

function barWidth() {
  const usableWidth = chartWidth - paddingLeft - paddingRight;
  return Math.max(3, usableWidth / props.points.length - 2);
}

function x(index) {
  const usableWidth = chartWidth - paddingLeft - paddingRight;
  return paddingLeft + (index / props.points.length) * usableWidth;
}

function height(value) {
  return ((Number(value) || 0) / maxValue.value) * (chartHeight - paddingTop - paddingBottom);
}

function y(value) {
  return chartHeight - paddingBottom - height(value);
}

function latestValue() {
  const latest = props.points.at(-1);
  if (!latest) {
    return "-";
  }
  return `${Number(latest[props.valueKey] || 0).toLocaleString("ko-KR", { maximumFractionDigits: 1 })}${props.valueSuffix}`;
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
        <span>
          <i :style="{ backgroundColor: color }"></i>
          현재 {{ latestValue() }}
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
      <rect
        v-for="(point, index) in points"
        :key="`${point.timestamp}-${index}`"
        class="admin-chart-bar"
        :x="x(index)"
        :y="y(point[valueKey])"
        :width="barWidth()"
        :height="height(point[valueKey])"
        :fill="color"
        rx="3"
      />
    </svg>
  </article>
</template>
