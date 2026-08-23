(function() {
  var style = getComputedStyle(document.documentElement);
  var accent = style.getPropertyValue('--accent').trim();
  var accent2 = style.getPropertyValue('--accent2').trim();
  var ink = style.getPropertyValue('--ink').trim();
  var muted = style.getPropertyValue('--muted').trim();
  var rule = style.getPropertyValue('--rule').trim();
  var bg2 = style.getPropertyValue('--bg2-solid').trim();

  // ---------- Mermaid 深色主题 ----------
  if (window.mermaid) {
    mermaid.initialize({
      startOnLoad: true,
      theme: 'dark',
      securityLevel: 'loose',
      themeVariables: {
        primaryColor: '#131a2e',
        primaryTextColor: '#e8eaf6',
        primaryBorderColor: '#4a5580',
        lineColor: '#9aa3c7',
        secondaryColor: '#1a2340',
        tertiaryColor: '#0d1326',
        fontSize: '13px',
        fontFamily: 'Segoe UI, Microsoft YaHei, sans-serif'
      }
    });
  }

  // ---------- 图 2-1：竞品六维雷达 ----------
  var radarEl = document.getElementById('chart-radar');
  if (radarEl && window.echarts) {
    var radar = echarts.init(radarEl, null, { renderer: 'svg' });
    radar.setOption({
      animation: false,
      color: [accent, accent2, muted, '#6ea8fe'],
      tooltip: { appendToBody: true },
      legend: {
        bottom: 0,
        textStyle: { color: muted, fontSize: 12 },
        itemWidth: 14, itemHeight: 8
      },
      radar: {
        indicator: [
          { name: '玩家自助创建', max: 5 },
          { name: '沉浸反馈', max: 5 },
          { name: '上手简易', max: 5 },
          { name: '跨世界传送', max: 5 },
          { name: '轻依赖', max: 5 },
          { name: '性能设计', max: 5 }
        ],
        center: ['50%', '48%'],
        radius: '62%',
        axisName: { color: ink, fontSize: 12.5 },
        splitLine: { lineStyle: { color: rule } },
        splitArea: { areaStyle: { color: ['rgba(255,255,255,0.02)', 'rgba(255,255,255,0.04)'] } },
        axisLine: { lineStyle: { color: rule } }
      },
      series: [{
        type: 'radar',
        symbolSize: 4,
        lineStyle: { width: 2 },
        areaStyle: { opacity: 0.12 },
        data: [
          { value: [1, 2, 2, 5, 2, 4], name: 'Multiverse-Portals' },
          { value: [2, 3, 3, 5, 3, 4], name: 'MyWorlds' },
          { value: [3, 3, 3, 5, 4, 4], name: 'AdvancedPortals' },
          { value: [5, 5, 5, 5, 5, 4], name: '任意门 DDoor（本方案）', lineStyle: { width: 3 }, areaStyle: { opacity: 0.2 } }
        ]
      }]
    });
    window.addEventListener('resize', function() { radar.resize(); });
  }

  // ---------- 图 5-2：开发工时估算 ----------
  var effortEl = document.getElementById('chart-effort');
  if (effortEl && window.echarts) {
    var effort = echarts.init(effortEl, null, { renderer: 'svg' });
    var cats = ['测试与文档', '防护与边界处理', '命令权限与配置', '管理 GUI', '钥匙配方与经济', '粒子与音效表现', '门识别与配对系统', '传送引擎与检测链'];
    var vals = [3, 3, 2, 3, 2, 2, 3, 4];
    effort.setOption({
      animation: false,
      tooltip: {
        appendToBody: true,
        formatter: function(p) { return p.name + '：' + p.value + ' 人日'; }
      },
      grid: { left: 8, right: 48, top: 10, bottom: 10, containLabel: true },
      xAxis: {
        type: 'value',
        max: 5,
        axisLabel: { color: muted, fontSize: 11, formatter: '{value}d' },
        splitLine: { lineStyle: { color: rule } }
      },
      yAxis: {
        type: 'category',
        data: cats,
        axisLabel: { color: ink, fontSize: 12.5 },
        axisLine: { lineStyle: { color: rule } },
        axisTick: { show: false }
      },
      series: [{
        type: 'bar',
        data: vals,
        barWidth: '55%',
        itemStyle: {
          borderRadius: [0, 4, 4, 0],
          color: {
            type: 'linear', x: 0, y: 0, x2: 1, y2: 0,
            colorStops: [
              { offset: 0, color: '#6ea8fe' },
              { offset: 1, color: accent }
            ]
          }
        },
        label: {
          show: true,
          position: 'right',
          color: accent2,
          fontSize: 12,
          fontWeight: 600,
          formatter: '{c} 人日'
        }
      }]
    });
    window.addEventListener('resize', function() { effort.resize(); });
  }
})();
