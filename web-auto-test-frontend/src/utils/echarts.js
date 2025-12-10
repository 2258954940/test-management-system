import * as echarts from "echarts";

// 初始化图表并绑定 resize 事件
export function initChart(domRef, option) {
  const el = domRef?.value || domRef;
  if (!el) return null;
  const chart = echarts.init(el);
  chart.setOption(option);
  const resizeHandler = () => {
    chart.resize();
  };
  window.addEventListener("resize", resizeHandler);
  chart.__resizeHandler = resizeHandler;
  return chart;
}

// 销毁图表并解绑事件
export function destroyChart(chartInstance) {
  if (!chartInstance) return;
  if (chartInstance.__resizeHandler) {
    window.removeEventListener("resize", chartInstance.__resizeHandler);
  }
  chartInstance.dispose();
}

// 折线图默认配置（用于近 7 天趋势）
export function getLineChartOption(xData = [], yData = []) {
  return {
    tooltip: { trigger: "axis" },
    xAxis: { type: "category", data: xData },
    yAxis: { type: "value" },
    series: [
      {
        data: yData,
        type: "line",
        smooth: true,
        areaStyle: {},
      },
    ],
  };
}

// 饼图默认配置（执行结果占比）
export function getPieChartOption(data = []) {
  return {
    tooltip: { trigger: "item" },
    legend: { bottom: 0 },
    series: [
      {
        name: "占比",
        type: "pie",
        radius: ["40%", "70%"],
        avoidLabelOverlap: false,
        label: { show: false },
        emphasis: {
          label: { show: true, fontSize: 14, fontWeight: "bold" },
        },
        data,
      },
    ],
  };
}

export default {
  initChart,
  destroyChart,
  getLineChartOption,
  getPieChartOption,
};
