(function() {
  function sleep(ms) { return new Promise(function(r) { setTimeout(r, ms); }); }

  // ============================================================
  // Demo 01 — 任意门创建全流程
  // ============================================================
  (function() {
    var doorA = document.getElementById('doorA');
    var doorB = document.getElementById('doorB');
    var steveA = document.getElementById('steveA');
    var steveB = document.getElementById('steveB');
    var handA = document.getElementById('handA');
    var handB = document.getElementById('handB');
    var pLink = document.getElementById('pLink');
    var stepsBox = document.getElementById('createSteps');
    var phaseEl = document.getElementById('createPhase');
    var chatBox = document.getElementById('createChat');
    var nextBtn = document.getElementById('createNext');
    var resetBtn = document.getElementById('createReset');

    function chat(text, cls) {
      var line = document.createElement('div');
      line.className = 'chat-line';
      var span = document.createElement('span');
      span.className = cls || 'dim';
      span.textContent = text;
      line.appendChild(span);
      chatBox.appendChild(line);
      chatBox.scrollTop = chatBox.scrollHeight;
    }

    function spawnParticles() {
      var colors = ['var(--accent)', 'var(--accent2)'];
      for (var i = 0; i < 16; i++) {
        var p = document.createElement('i');
        p.style.left = (20 + Math.random() * 8) + '%';
        p.style.top = (44 + Math.random() * 30) + '%';
        p.style.setProperty('--fx', (42 + Math.random() * 10) + '%');
        p.style.animationDelay = (Math.random() * 1.4).toFixed(2) + 's';
        p.style.background = colors[i % 2];
        p.style.boxShadow = '0 0 8px ' + colors[i % 2];
        pLink.appendChild(p);
      }
    }

    var steps = [
      { label: '放置门 A', phase: '第 2 步：手持门之钥，右键门 A', run: function() {
        doorA.classList.add('show');
        chat('* 你在主世界放置了一扇铁门');
      }},
      { label: '钥匙右键门 A', phase: '第 3 步：穿行到另一个世界（下界）', run: function() {
        handA.style.opacity = '1';
        doorA.classList.add('lit');
        chat('[任意门] 已选中这扇门，请在 60 秒内右键另一扇门完成配对', 'sys');
      }},
      { label: '前往下界', phase: '第 4 步：在下界放置第二扇门', run: function() {
        steveA.classList.add('hidden');
        steveB.classList.remove('hidden');
        chat('* 你来到了下界');
      }},
      { label: '放置门 B', phase: '第 5 步：手持门之钥，右键门 B', run: function() {
        doorB.classList.add('show');
        chat('* 你在下界放置了第二扇铁门');
      }},
      { label: '钥匙右键门 B', phase: '第 6 步：连接建立中…', run: function() {
        handB.style.opacity = '1';
        chat('[任意门] 正在建立两扇门之间的连接…', 'sys');
      }},
      { label: '配对完成', phase: '第 7 步：走回主世界，走进门 A 测试', run: function() {
        handA.style.opacity = '0';
        handB.style.opacity = '0';
        doorB.classList.add('lit');
        pLink.classList.add('on');
        chat('[任意门] 配对成功！消耗 门之钥 ×1', 'ok');
        chat('[任意门] 此后走进任意一扇门，即可从另一扇门前走出', 'sys');
      }},
      { label: '走进门 A', phase: '第 8 步：从下界的门 B 走出', run: function() {
        steveB.classList.add('hidden');
        steveA.classList.remove('hidden');
        steveA.style.left = '8%';
        void steveA.offsetWidth;
        steveA.style.left = '19%';
        chat('* 你走进门 A，紫颂粒子将你包裹…');
      }},
      { label: '门 B 走出', phase: '演示完成，点击「重置演示」可重新体验', run: function() {
        steveA.classList.add('hidden');
        steveB.classList.remove('hidden');
        steveB.style.left = '76%';
        void steveB.offsetWidth;
        steveB.style.left = '64%';
        chat('[任意门] 传送成功！欢迎来到下界', 'ok');
      }}
    ];

    var cur = 0;
    var locked = false;

    function renderSteps() {
      stepsBox.innerHTML = '';
      steps.forEach(function(s, i) {
        var chip = document.createElement('span');
        chip.className = 'step-chip' + (i < cur ? ' done' : (i === cur ? ' active' : ''));
        chip.textContent = (i + 1) + '. ' + s.label;
        stepsBox.appendChild(chip);
      });
    }

    function reset() {
      cur = 0; locked = false;
      doorA.classList.remove('show', 'lit');
      doorB.classList.remove('show', 'lit');
      steveA.classList.remove('hidden');
      steveA.style.left = '8%';
      steveB.classList.add('hidden');
      steveB.style.left = '78%';
      handA.style.opacity = '0';
      handB.style.opacity = '0';
      pLink.classList.remove('on');
      pLink.innerHTML = '';
      chatBox.innerHTML = '';
      var line = document.createElement('div');
      line.className = 'chat-line dim';
      line.textContent = '* 演示已就绪，点击「执行下一步」开始';
      chatBox.appendChild(line);
      nextBtn.disabled = false;
      phaseEl.textContent = '第 1 步：在主世界放置一扇门';
      renderSteps();
    }

    nextBtn.addEventListener('click', function() {
      if (locked || cur >= steps.length) return;
      locked = true;
      if (cur === 5) spawnParticles();
      steps[cur].run();
      cur++;
      phaseEl.textContent = steps[Math.min(cur, steps.length - 1)].phase;
      if (cur >= steps.length) {
        nextBtn.disabled = true;
      }
      renderSteps();
      setTimeout(function() { locked = false; }, 500);
    });

    resetBtn.addEventListener('click', reset);
    renderSteps();
  })();

  // ============================================================
  // Demo 02 — 传送检测链
  // ============================================================
  (function() {
    var chainBox = document.getElementById('chain');
    var tpBtn = document.getElementById('tpBtn');
    var resultEl = document.getElementById('tpResult');
    var cdWrap = document.getElementById('cdWrap');
    var cdFill = document.getElementById('cdFill');
    var cdLabel = document.getElementById('cdLabel');
    var chatBox = document.getElementById('tpChat');
    var blockerEls = document.querySelectorAll('#blockers .tg');

    var cooldownUntil = 0;
    var cdTimer = null;
    var running = false;

    function chat(text, cls) {
      var line = document.createElement('div');
      line.className = 'chat-line';
      var span = document.createElement('span');
      span.className = cls || 'dim';
      span.textContent = text;
      line.appendChild(span);
      chatBox.appendChild(line);
      chatBox.scrollTop = chatBox.scrollHeight;
    }

    function blockers() {
      var map = {};
      blockerEls.forEach(function(t) { map[t.dataset.k] = t.classList.contains('on'); });
      return map;
    }

    blockerEls.forEach(function(t) {
      t.addEventListener('click', function() { t.classList.toggle('on'); });
    });

    function node(step) { return chainBox.querySelector('.chain-node[data-step="' + step + '"]'); }

    function resetChain() {
      chainBox.querySelectorAll('.chain-node').forEach(function(n) {
        n.classList.remove('pass', 'block', 'final');
      });
    }

    function startCooldown() {
      cooldownUntil = Date.now() + 3000;
      cdWrap.classList.add('on');
      cdFill.style.transition = 'none';
      cdFill.style.width = '100%';
      void cdFill.offsetWidth;
      cdFill.style.transition = 'width 3s linear';
      cdFill.style.width = '0%';
      clearInterval(cdTimer);
      cdTimer = setInterval(function() {
        var left = cooldownUntil - Date.now();
        if (left <= 0) {
          clearInterval(cdTimer);
          cdLabel.textContent = '冷却已结束';
          setTimeout(function() { cdWrap.classList.remove('on'); }, 1500);
        } else {
          cdLabel.textContent = '传送冷却：' + (left / 1000).toFixed(1) + 's';
        }
      }, 100);
    }

    var failMsg = {
      cooldown: ['传送冷却中，请稍候再试', '在「冷却检查」被拦截'],
      perm: ['你没有使用这扇任意门的权限', '在「权限检查」被拦截'],
      broken: ['对侧的门已失效，本门暂时无法传送', '在「对侧门有效性」被拦截'],
      world: ['目标世界已禁用任意门传送', '在「目标世界可用」被拦截'],
      unsafe: ['对侧落点不安全，传送已取消', '在「落点安全校验」被拦截']
    };

    tpBtn.addEventListener('click', async function() {
      if (running) return;
      running = true;
      resetChain();
      resultEl.textContent = '检测中…';
      resultEl.style.color = 'var(--muted)';
      var st = blockers();
      var seq = ['cooldown', 'perm', 'broken', 'world', 'unsafe'];

      for (var i = 0; i < seq.length; i++) {
        var s = seq[i];
        await sleep(420);
        var hit = st[s] || (s === 'cooldown' && Date.now() < cooldownUntil);
        if (hit) {
          node(s).classList.add('block');
          chat('[任意门] ' + failMsg[s][0], 'err');
          resultEl.textContent = '✘ ' + failMsg[s][1];
          resultEl.style.color = 'var(--danger)';
          if (s === 'broken') chat('* 本门已降级为未配对状态', 'dim');
          running = false;
          return;
        }
        node(s).classList.add('pass');
      }

      await sleep(420);
      node('tp').classList.add('final');
      chat('[任意门] 传送成功！粒子绽放中…', 'ok');
      resultEl.textContent = '✔ 六环检测全部通过 · 传送成功';
      resultEl.style.color = 'var(--accent2)';
      startCooldown();
      running = false;
    });
  })();

  // ============================================================
  // Demo 04 — 任意门管理 GUI
  // ============================================================
  (function() {
    var chestGrid = document.getElementById('chestGrid');
    var gdName = document.getElementById('gdName');
    var gdState = document.getElementById('gdState');
    var gdKv = document.getElementById('gdKv');
    var gdTp = document.getElementById('gdTp');
    var gdRename = document.getElementById('gdRename');
    var gdUnlink = document.getElementById('gdUnlink');
    var gdDel = document.getElementById('gdDel');
    var chestTitle = document.querySelector('.mc-chest-title');

    var toastEl = document.createElement('div');
    toastEl.className = 'gui-toast';
    document.body.appendChild(toastEl);
    var toastTimer = null;
    function toast(text) {
      toastEl.textContent = text;
      toastEl.classList.add('on');
      clearTimeout(toastTimer);
      toastTimer = setTimeout(function() { toastEl.classList.remove('on'); }, 2200);
    }

    var pairs = [
      { name: '主城东站', owner: '米优Admin', wa: 'world (120, 64, -80)', wb: 'world_nether (15, 40, -10)', uses: 1284, linked: true },
      { name: '矿场北门', owner: 'Steve', wa: 'world (-320, 71, 450)', wb: 'world (-300, 70, 440)', uses: 356, linked: true },
      { name: '蜂岛牧场', owner: 'Alex', wa: 'skyblock (8, 90, 12)', wb: 'world (640, 68, -210)', uses: 210, linked: true },
      { name: '末影直达', owner: 'PixelTest01', wa: 'world (0, 64, 0)', wb: 'world_the_end (100, 49, 0)', uses: 98, linked: true },
      { name: '集市环线', owner: '村民甲', wa: 'world (210, 65, 310)', wb: 'world (280, 66, 305)', uses: 776, linked: true },
      { name: '深板岩矿井', owner: 'Notch粉丝', wa: 'world (-45, -18, 88)', wb: 'deepslate_mine (-60, -24, 70)', uses: 156, linked: true },
      { name: '观星台', owner: 'Luna', wa: 'skyblock (0, 120, 0)', wb: 'skyblock_obs (30, 128, -40)', uses: 43, linked: false },
      { name: '红石实验室', owner: 'CJ', wa: 'world (900, 40, 900)', wb: 'redstone_lab (0, 30, 0)', uses: 89, linked: true }
    ];
    var owners = ['Steve', 'Alex', 'PixelTest01', 'PixelPlayer2', 'Luna', 'CJ', '村民甲', '米优Admin'];
    var worlds = ['world', 'world_nether', 'world_the_end', 'skyblock', 'deepslate_mine'];
    for (var i = 0; i < 32; i++) {
      pairs.push({
        name: '门对 ' + (i + 9),
        owner: owners[i % owners.length],
        wa: worlds[i % worlds.length] + ' (' + (i * 17 + 20) + ', 64, ' + (i * 23 - 100) + ')',
        wb: worlds[(i + 2) % worlds.length] + ' (' + (i * 13 - 50) + ', 40, ' + (i * 31 + 60) + ')',
        uses: (i * 37) % 500,
        linked: i % 7 !== 3
      });
    }

    var page = 0;
    var PER = 36;
    var filterMine = false;
    var selected = -1;

    function visiblePairs() {
      return pairs.filter(function(p) { return !filterMine || p.owner === '米优Admin'; });
    }

    function addSlot(cls, text, title, onclick) {
      var slot = document.createElement('div');
      slot.className = 'chest-slot';
      if (cls) {
        var item = document.createElement('div');
        item.className = 'chest-item ' + cls;
        item.innerHTML = text;
        slot.appendChild(item);
      }
      if (title) slot.title = title;
      if (onclick) slot.addEventListener('click', onclick);
      chestGrid.appendChild(slot);
      return slot;
    }

    function updateDetail() {
      var p = selected >= 0 ? visiblePairs()[selected] : null;
      if (!p) {
        gdName.textContent = '未选中门对';
        gdState.textContent = filterMine ? '已筛选：仅显示我创建的门对 · 点击左侧门图标查看详情' : '点击左侧任意门图标查看详情';
        gdKv.innerHTML = '';
        return;
      }
      gdName.textContent = p.name;
      gdState.textContent = p.linked ? '● 已配对 · 双向互通' : '○ 未配对 · 对侧门已失效';
      gdState.style.color = p.linked ? 'var(--accent2)' : 'var(--warn)';
      gdKv.innerHTML =
        '<b>门 A：</b>' + p.wa + '<br>' +
        '<b>门 B：</b>' + p.wb + '<br>' +
        '<b>所有者：</b>' + p.owner + '<br>' +
        '<b>累计穿越：</b>' + p.uses.toLocaleString() + ' 次';
    }

    function renderGui() {
      chestGrid.innerHTML = '';
      var data = visiblePairs();
      var pages = Math.max(1, Math.ceil(data.length / PER));
      if (page >= pages) page = pages - 1;
      chestTitle.textContent = '任意门管理 · 共 ' + data.length + ' 对' + (filterMine ? ' · 我的' : '');

      // 第 1 行：功能格
      addSlot('nav', '◀', '上一页', function() { if (page > 0) { page--; selected = -1; updateDetail(); renderGui(); } });
      addSlot(null, '', '', null);
      addSlot('func', filterMine ? '★' : '☆', filterMine ? '取消筛选' : '仅显示我创建的', function() {
        filterMine = !filterMine; page = 0; selected = -1;
        updateDetail(); renderGui();
        toast(filterMine ? '已筛选：仅显示你创建的门对' : '已显示全部门对');
      });
      addSlot(null, '', '', null);
      addSlot('nav', (page + 1) + '/' + pages, '当前页码', null);
      addSlot(null, '', '', null);
      addSlot(null, '', '', null);
      addSlot('func', '✕', '关闭', function() { toast('已关闭管理界面'); });
      addSlot('nav', '▶', '下一页', function() { if (page < pages - 1) { page++; selected = -1; updateDetail(); renderGui(); } });

      // 第 2~5 行：门对图标
      var start = page * PER;
      for (var i = start; i < start + PER; i++) {
        if (i < data.length) {
          (function(idx) {
            var p = data[idx];
            var slot = addSlot('door' + (p.linked ? '' : ' unlinked'), '', p.name + ' · ' + p.wa.split(' ')[0] + (p.linked ? '' : ' · 未配对'), function() {
              selected = idx;
              chestGrid.querySelectorAll('.chest-slot').forEach(function(s) { s.classList.remove('selected'); });
              slot.classList.add('selected');
              updateDetail();
            });
            if (idx === selected) slot.classList.add('selected');
          })(i);
        } else {
          addSlot(null, '', '', null);
        }
      }

      // 第 6 行：快捷栏映射
      for (var j = 0; j < 9; j++) addSlot(null, '', '', null);
    }

    gdTp.addEventListener('click', function() {
      var p = selected >= 0 ? visiblePairs()[selected] : null;
      if (!p) { toast('请先选择一个门对'); return; }
      toast('正在传送至「' + p.name + '」…');
    });
    gdRename.addEventListener('click', function() {
      var p = selected >= 0 ? visiblePairs()[selected] : null;
      if (!p) { toast('请先选择一个门对'); return; }
      toast('请在聊天栏输入新名称（演示流程）');
    });
    gdUnlink.addEventListener('click', function() {
      var p = selected >= 0 ? visiblePairs()[selected] : null;
      if (!p) { toast('请先选择一个门对'); return; }
      p.linked = false;
      renderGui(); updateDetail();
      toast('已解除「' + p.name + '」的配对');
    });
    gdDel.addEventListener('click', function() {
      var data = visiblePairs();
      var p = selected >= 0 ? data[selected] : null;
      if (!p) { toast('请先选择一个门对'); return; }
      var realIdx = pairs.indexOf(p);
      pairs.splice(realIdx, 1);
      selected = -1;
      renderGui(); updateDetail();
      toast('已删除「' + p.name + '」的记录');
    });

    renderGui();
    updateDetail();
  })();
})();
