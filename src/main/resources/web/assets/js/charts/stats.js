/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

/* eslint-disable no-unused-vars */

// 管理页用量统计迷你趋势图
// eslint-disable-next-line no-unused-vars
function $renderStats(data, $el) {
  const xAxis = []
  const series = []
  data.forEach((item) => {
    xAxis.push(item[0])
    series.push(item[1])
  })

  const option = {
    grid: { left: 0, right: 0, top: 4, bottom: 4 },
    animation: true,
    tooltip: {
      trigger: 'axis',
      formatter: '{b} : <b>{c}</b>',
      textStyle: {
        fontSize: 12,
        lineHeight: 1.3,
        color: '#333',
      },
      axisPointer: {
        lineStyle: { color: '#ddd' },
      },
      backgroundColor: '#fff',
      extraCssText: 'border-radius:0;box-shadow:0 0 6px 0 rgba(0, 0, 0, .1), 0 8px 10px 0 rgba(170, 182, 206, .2);',
      confine: true,
      position: 'top',
    },
    textStyle: {
      fontFamily: '"Hiragina Sans GB", San Francisco, "Helvetica Neue", Helvetica, Arial, PingFangSC-Light, "WenQuanYi Micro Hei", "Microsoft YaHei UI", "Microsoft YaHei", sans-serif',
    },
    xAxis: {
      show: false,
      type: 'category',
      data: xAxis,
    },
    yAxis: {
      show: false,
      type: 'value',
      splitLine: { show: false },
      cursor: 'default',
    },
    series: [
      {
        data: series,
        areaStyle: { opacity: 0.2 },
        itemStyle: {
          normal: {
            color: '#4285f4',
            lineStyle: { color: '#4285f4' },
          },
        },
        type: 'line',
        smooth: true,
        connectNulls: true,
      },
    ],
  }

  const c = echarts.init($el.find('span')[0])
  c.setOption(option)
}
