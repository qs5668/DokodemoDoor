(function() {
  function sleep(ms) { return new Promise(function(r) { setTimeout(r, ms); }); }

  // ============================================================
  // 模块一：三步上手创建演示
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
      { label: '主世界放门 A', phase: '第 2 步：手持门之钥，右键这扇门', run: function() {
        doorA.classList.add('show');
        chat('* 你在主世界放了一扇铁门');
      }},
      { label: '钥匙右键门 A', phase: '第 3 步：现在去另一个世界（比如下界）', run: function() {
        handA.style.opacity = '1';
        doorA.classList.add('lit');
        chat('[任意门] 这扇门已选中！请在 60 秒内右键另一扇门完成配对', 'sys');
      }},
      { label: '前往下界', phase: '第 4 步：在下界放第二扇门', run: function() {
        steveA.classList.add('hidden');
        steveB.classList.remove('hidden');
        chat('* 你跑到了下界（普通走过去或者用原版下界门都行）');
      }},
      { label: '下界放门 B', phase: '第 5 步：手持门之钥，右键门 B', run: function() {
        doorB.classList.add('show');
        chat('* 你在下界放了第二扇铁门');
      }},
      { label: '钥匙右键门 B', phase: '第 6 步：见证连接建立…', run: function() {
        handB.style.opacity = '1';
        chat('[任意门] 正在建立两扇门之间的连接…', 'sys');
      }},
      { label: '配对成功', phase: '第 7 步：走回主世界，走进门 A 试试', run: function() {
        handA.style.opacity = '0';
        handB.style.opacity = '0';
        doorB.classList.add('lit');
        pLink.classList.add('on');
        chat('[任意门] 配对成功！消耗 门之钥 ×1', 'ok');
        chat('[任意门] 从现在起，走进任意一扇门，就会从另一扇门前走出', 'sys');
      }},
      { label: '走进门 A', phase: '第 8 步：从下界的门 B 走出来', run: function() {
        steveB.classList.add('hidden');
        steveA.classList.remove('hidden');
        steveA.style.left = '8%';
        void steveA.offsetWidth;
        steveA.style.left = '19%';
        chat('* 你走进门 A，紫色粒子把你包裹起来…');
      }},
      { label: '门 B 抵达', phase: '演示完成！点「重置演示」可以再看一遍', run: function() {
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
      phaseEl.textContent = '第 1 步：在主世界放一扇门（木门铁门都行）';
      renderSteps();
    }

    nextBtn.addEventListener('click', function() {
      if (locked || cur >= steps.length) return;
      locked = true;
      if (cur === 5) spawnParticles();
      steps[cur].run();
      cur++;
      phaseEl.textContent = steps[Math.min(cur, steps.length - 1)].phase;
      if (cur >= steps.length) nextBtn.disabled = true;
      renderSteps();
      setTimeout(function() { locked = false; }, 500);
    });

    resetBtn.addEventListener('click', reset);
    renderSteps();
  })();

  // ============================================================
  // 模块二：推门试试（点击传送 + 3 秒冷却）
  // ============================================================
  (function() {
    var scene = document.getElementById('tryScene');
    var bgOver = document.getElementById('bgOver');
    var bgNether = document.getElementById('bgNether');
    var flash = document.getElementById('tryFlash');
    var particles = document.getElementById('tryParticles');
    var label = document.getElementById('tryLabel');
    var status = document.getElementById('tryStatus');
    var cdWrap = document.getElementById('tryCd');
    var cdFill = document.getElementById('tryCdFill');

    var inNether = false;
    var cooling = false;
    var cdTimer = null;

    function burst() {
      particles.innerHTML = '';
      var colors = ['var(--accent)', 'var(--accent2)', '#f5d76e'];
      for (var i = 0; i < 24; i++) {
        var p = document.createElement('i');
        var angle = Math.random() * Math.PI * 2;
        var dist = 40 + Math.random() * 90;
        p.style.setProperty('--tx', (Math.cos(angle) * dist).toFixed(0) + 'px');
        p.style.setProperty('--ty', (Math.sin(angle) * dist - 30).toFixed(0) + 'px');
        p.style.animationDelay = (Math.random() * 0.15).toFixed(2) + 's';
        var c = colors[i % colors.length];
        p.style.background = c;
        p.style.boxShadow = '0 0 8px ' + c;
        particles.appendChild(p);
      }
    }

    function startCooldown() {
      cooling = true;
      scene.classList.add('cooling');
      cdWrap.classList.add('on');
      cdFill.style.transition = 'none';
      cdFill.style.width = '100%';
      void cdFill.offsetWidth;
      cdFill.style.transition = 'width 3s linear';
      cdFill.style.width = '0%';
      var until = Date.now() + 3000;
      clearInterval(cdTimer);
      cdTimer = setInterval(function() {
        var left = until - Date.now();
        if (left <= 0) {
          clearInterval(cdTimer);
          cooling = false;
          scene.classList.remove('cooling');
          cdWrap.classList.remove('on');
          status.innerHTML = '冷却结束，<b>再点一次门</b>就能回去了。';
        } else {
          status.textContent = '传送冷却中…还剩 ' + (left / 1000).toFixed(1) + ' 秒（游戏里也是 3 秒）';
        }
      }, 100);
    }

    scene.addEventListener('click', function() {
      if (cooling) {
        status.textContent = '冷却中，稍等一下再进门——这就是规则里的 3 秒冷却。';
        return;
      }
      burst();
      flash.classList.add('on');
      setTimeout(function() { flash.classList.remove('on'); }, 180);

      setTimeout(function() {
        inNether = !inNether;
        bgOver.classList.toggle('off', inNether);
        bgNether.classList.toggle('on', inNether);
        label.textContent = inNether ? '当前：下界' : '当前：主世界';
        if (inNether) {
          status.innerHTML = '传送成功！你已抵达<b>下界</b>。刚传过来不能马上回去——冷却 3 秒。';
        } else {
          status.innerHTML = '欢迎回到<b>主世界</b>！传送就是这么简单：走进去，走出来。';
        }
        startCooldown();
      }, 200);
    });
  })();

  // ============================================================
  // 模块三：FAQ 手风琴
  // ============================================================
  (function() {
    var items = document.querySelectorAll('.faq-item');
    items.forEach(function(item) {
      var q = item.querySelector('.faq-q');
      var a = item.querySelector('.faq-a');
      q.addEventListener('click', function() {
        var isOpen = item.classList.contains('open');
        items.forEach(function(other) {
          other.classList.remove('open');
          other.querySelector('.faq-a').style.maxHeight = '0px';
        });
        if (!isOpen) {
          item.classList.add('open');
          a.style.maxHeight = a.scrollHeight + 'px';
        }
      });
    });
  })();
})();
