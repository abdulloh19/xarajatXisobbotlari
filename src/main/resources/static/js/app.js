/**
 * HISOBCHI & TO-DO SUPER APP - CLIENT APPLICATION LOGIC
 * Features:
 *  - Full State Management (Tasks, Transactions, Debts, Categories, Net Profit)
 *  - Uzbek Smart NLP Simulation (Fixes the hour-as-amount bug; handles dates, times, amounts)
 *  - 2-Step Task-to-Expense Conversion Flow with Modal
 *  - Interactive Analytics Charts (SVG/Canvas Dynamic Bars)
 *  - Voice Assistant Simulator with Presets
 *  - Telegram WebApp Native Integration (Haptics, Color scheme)
 */

// =============================================================================
// 1. STATE INITIALIZATION
// =============================================================================

const state = {
  activeTab: 'tab-dashboard',
  filterTodo: 'all',
  filterDebt: 'all',
  isWorkDay: true,
  dailyNetProfit: 185000,
  cashBalance: 115000,
  cardBalance: 70000,
  todayIncome: 320000,
  todayExpense: 135000,

  categories: [
    { id: 'cat-1', name: 'Tushlik', icon: '🍔', color: '#F59E0B', spentToday: 35000, monthlyTotal: 620000 },
    { id: 'cat-2', name: 'Transport', icon: '🚕', color: '#06B6D4', spentToday: 20000, monthlyTotal: 340000 },
    { id: 'cat-3', name: 'Internet & Aloqa', icon: '🌐', color: '#6366F1', spentToday: 0, monthlyTotal: 150000 },
    { id: 'cat-4', name: 'Bozorlik', icon: '🛒', color: '#EC4899', spentToday: 80000, monthlyTotal: 1250000 },
    { id: 'cat-5', name: 'Kommunal', icon: '💡', color: '#10B981', spentToday: 0, monthlyTotal: 480000 },
    { id: 'cat-6', name: 'Kiyim-kechak', icon: '👕', color: '#8B5CF6', spentToday: 0, monthlyTotal: 320000 },
    { id: 'cat-7', name: 'Boshqa', icon: '📌', color: '#64748B', spentToday: 0, monthlyTotal: 190000 }
  ],

  tasks: [
    {
      id: 1,
      title: 'Internet uchun to‘lov',
      dueDate: getTodayStr(),
      dueTime: '18:00',
      plannedAmount: 150000,
      isExpenseCandidate: true,
      category: 'Internet & Aloqa',
      completed: false,
      isOverdue: false,
      projectId: null
    },
    {
      id: 2,
      title: 'Konditsioner ustasiga pul berish',
      dueDate: getTodayStr(),
      dueTime: '16:30',
      plannedAmount: 80000,
      isExpenseCandidate: true,
      category: 'Kommunal',
      completed: false,
      isOverdue: false,
      projectId: 'proj-2'
    },
    {
      id: 3,
      title: 'Soat 12-da miting bor (Mijoz bilan)',
      dueDate: getTodayStr(),
      dueTime: '12:00',
      plannedAmount: null, // Critical: Demonstrates NO accidental 12 so'm expense!
      isExpenseCandidate: false,
      category: 'Ish',
      completed: false,
      isOverdue: false,
      projectId: 'proj-1'
    },
    {
      id: 4,
      title: 'Ingliz tili dars topshiriqlarini bajarish',
      dueDate: getRelativeDateStr(-1),
      dueTime: '09:00',
      plannedAmount: null, // Critical: Demonstrates NO accidental 9 so'm expense!
      isExpenseCandidate: false,
      category: 'Ta’lim',
      completed: false,
      isOverdue: true,
      projectId: null
    },
    {
      id: 5,
      title: 'Server va domen to‘lovini uzaytirish',
      dueDate: getRelativeDateStr(3),
      dueTime: '11:00',
      plannedAmount: 220000,
      isExpenseCandidate: true,
      category: 'Internet & Aloqa',
      completed: false,
      isOverdue: false,
      projectId: 'proj-1'
    },
    {
      id: 6,
      title: 'Tushlik qilish (Osh markazi)',
      dueDate: getTodayStr(),
      dueTime: '13:00',
      plannedAmount: 35000,
      isExpenseCandidate: true,
      category: 'Tushlik',
      completed: true,
      isOverdue: false,
      projectId: null
    }
  ],

  transactions: [
    {
      id: 101,
      type: 'expense',
      title: 'Tushlik (Osh markazi)',
      category: 'Tushlik',
      amount: 35000,
      method: 'CASH',
      time: '13:15',
      date: 'Bugun',
      icon: '🍔'
    },
    {
      id: 102,
      type: 'expense',
      title: 'Yandex Go taksi',
      category: 'Transport',
      amount: 20000,
      method: 'CARD',
      time: '10:45',
      date: 'Bugun',
      icon: '🚕'
    },
    {
      id: 103,
      type: 'income',
      title: 'Freelance buyurtma (Telegram bot)',
      category: 'Foyda',
      amount: 250000,
      method: 'CARD',
      time: '09:30',
      date: 'Bugun',
      icon: '💰'
    },
    {
      id: 104,
      type: 'expense',
      title: 'Bozorlik (meva va sabzavotlar)',
      category: 'Bozorlik',
      amount: 80000,
      method: 'CASH',
      time: '08:20',
      date: 'Bugun',
      icon: '🛒'
    },
    {
      id: 105,
      type: 'income',
      title: 'Mijozdan to‘lov (Konsultatsiya)',
      category: 'Foyda',
      amount: 70000,
      method: 'CASH',
      time: 'Kecha',
      date: 'Kecha',
      icon: '💵'
    }
  ],

  debts: [
    {
      id: 201,
      personName: 'Jasur aka',
      amount: 750000,
      type: 'BORROWED', // Men olganman
      dueDate: '20-oktabr, 2026',
      note: 'Yangi noutbuk olish uchun olingan qarz',
      settled: false
    },
    {
      id: 202,
      personName: 'Akmal',
      amount: 500000,
      type: 'BORROWED', // Men olganman
      dueDate: '15-oktabr, 2026',
      note: 'Yo‘l xarajatlari uchun',
      settled: false
    },
    {
      id: 203,
      personName: 'Otabek',
      amount: 800000,
      type: 'LENT', // Menga qaytarishadi
      dueDate: '25-oktabr, 2026',
      note: 'Bot dasturlash xizmati bo‘yicha qarz',
      settled: false
    }
  ],

  projects: [
    {
      id: 'proj-1',
      title: 'Telegram Bot Versiya 2.0',
      color: '#6366F1',
      totalTasks: 4,
      completedTasks: 2
    },
    {
      id: 'proj-2',
      title: 'Uy ta’miri & Xaridlar',
      color: '#10B981',
      totalTasks: 3,
      completedTasks: 1
    }
  ],

  // Holds task undergoing 2-step completion
  pendingTaskForExpense: null
};

// =============================================================================
// 2. HELPER UTILITIES
// =============================================================================

function getTodayStr() {
  const d = new Date();
  return d.toISOString().split('T')[0];
}

function getRelativeDateStr(daysDiff) {
  const d = new Date();
  d.setDate(d.getDate() + daysDiff);
  return d.toISOString().split('T')[0];
}

function formatMoney(amount) {
  if (amount == null || isNaN(amount)) return '0 so‘m';
  return Number(amount).toLocaleString('uz-UZ').replace(/,/g, ' ') + ' so‘m';
}

function triggerHaptic() {
  if (window.Telegram && window.Telegram.WebApp && window.Telegram.WebApp.HapticFeedback) {
    try {
      window.Telegram.WebApp.HapticFeedback.impactOccurred('light');
    } catch (e) {}
  } else if (navigator.vibrate) {
    try {
      navigator.vibrate(20);
    } catch (e) {}
  }
}

function showToast(message, type = 'info', icon = '💡') {
  const container = document.getElementById('toast-container');
  if (!container) return;

  const toast = document.createElement('div');
  toast.className = `toast toast-${type}`;
  toast.innerHTML = `
    <span class="toast-icon">${icon}</span>
    <span class="toast-msg">${message}</span>
  `;

  container.appendChild(toast);
  triggerHaptic();

  setTimeout(() => {
    toast.style.opacity = '0';
    toast.style.transform = 'translateY(-10px)';
    setTimeout(() => {
      if (toast.parentNode) toast.parentNode.removeChild(toast);
    }, 300);
  }, 3200);
}

// =============================================================================
// 3. SMART NLP PARSER SIMULATOR (MIRRORS Java TodoNlpService)
// =============================================================================
// Handles Uzbek natural language input:
// - Detects time expressions (soat 12-da, soat 9 da, 18:30)
// - Fixes bug: hour numbers are NOT extracted as financial amounts
// - Detects currency only with indicators (ming, mln, so'm, k, to'lash)
// =============================================================================

function parseNlpInput(rawText) {
  if (!rawText || !rawText.trim()) return null;
  const text = rawText.trim();
  const lower = text.toLowerCase();

  let dueDate = getTodayStr();
  let dueTime = null;
  let plannedAmount = null;
  let isExpenseCandidate = false;
  let cleanTitle = text;

  // 1. Check relative date
  if (lower.includes('ertaga') || lower.includes('ertega')) {
    dueDate = getRelativeDateStr(1);
    cleanTitle = cleanTitle.replace(/ertaga|ertega/gi, '');
  } else if (lower.includes('indin') || lower.includes('indinga')) {
    dueDate = getRelativeDateStr(2);
    cleanTitle = cleanTitle.replace(/indin(ga)?/gi, '');
  } else if (lower.includes('kecha')) {
    dueDate = getRelativeDateStr(-1);
    cleanTitle = cleanTitle.replace(/kecha/gi, '');
  } else if (lower.includes('bugun')) {
    dueDate = getTodayStr();
    cleanTitle = cleanTitle.replace(/bugun/gi, '');
  }

  // 2. Check time pattern e.g. "soat 12-da", "soat 9 da", "soat 18:00 da", "14:30 da"
  const hourPattern = /(?:soat\s*)?(\d{1,2})(?:[:.](\d{2}))?\s*(?:-?da|-gacha|\s*da)?/i;
  const timeExplicitPattern = /\b([01]?\d|2[0-3])[:.]([0-5]\d)\b/;
  const soatWordPattern = /\bsoat\s*(\d{1,2})(?:[:.](\d{2}))?\s*(?:-?da|-gacha)?\b/gi;

  let timeMatch = null;
  if ((timeMatch = timeExplicitPattern.exec(cleanTitle)) !== null) {
    const hh = timeMatch[1].padStart(2, '0');
    const mm = timeMatch[2].padStart(2, '0');
    dueTime = `${hh}:${mm}`;
    cleanTitle = cleanTitle.replace(timeMatch[0], '');
  } else if ((timeMatch = soatWordPattern.exec(cleanTitle)) !== null) {
    const hh = timeMatch[1].padStart(2, '0');
    const mm = timeMatch[2] ? timeMatch[2].padStart(2, '0') : '00';
    dueTime = `${hh}:${mm}`;
    cleanTitle = cleanTitle.replace(timeMatch[0], '');
  }

  // 3. Extract planned amount (STRICT: Requires financial context!)
  // E.g.: "150 ming", "150k", "150000 so'm", "150000 som", "150 ming to'lash"
  const amountPattern = /\b(\d+(?:[.,]\d+)?)\s*(ming|mln|million|k|so['`‘]m|som)\b/i;
  const directLargeNumberPattern = /\b(\d{4,9})\b/; // 4+ digits e.g. 50000

  let amountMatch = amountPattern.exec(cleanTitle);
  if (amountMatch) {
    let num = parseFloat(amountMatch[1].replace(',', '.'));
    const unit = amountMatch[2].toLowerCase();
    if (unit === 'ming' || unit === 'k') {
      num = num * 1000;
    } else if (unit === 'mln' || unit === 'million') {
      num = num * 1000000;
    }
    plannedAmount = Math.round(num);
    isExpenseCandidate = true;
    cleanTitle = cleanTitle.replace(amountMatch[0], '');
  } else {
    // Check large number only if financial intent exists
    const hasFinancialWord = /to['`‘]?lash|to['`‘]?lov|xarajat|berish|narxi|olish|qarz/i.test(lower);
    if (hasFinancialWord) {
      let largeMatch = directLargeNumberPattern.exec(cleanTitle);
      if (largeMatch) {
        plannedAmount = parseInt(largeMatch[1], 10);
        isExpenseCandidate = true;
        cleanTitle = cleanTitle.replace(largeMatch[0], '');
      }
    }
  }

  // 4. Clean up remaining title
  cleanTitle = cleanTitle
    .replace(/\s+/g, ' ')
    .replace(/^[\s,.-]+|[\s,.-]+$/g, '')
    .trim();

  if (!cleanTitle) {
    cleanTitle = rawText.trim();
  }

  return {
    title: cleanTitle,
    dueDate,
    dueTime,
    plannedAmount,
    isExpenseCandidate
  };
}

// =============================================================================
// 4. RENDERERS
// =============================================================================

function renderAll() {
  renderDashboard();
  renderTasks();
  renderTransactions();
  renderDebts();
  renderAnalytics();
  renderCategories();
  renderProjects();
}

function renderDashboard() {
  // Update Net profit balance
  const valNetProfit = document.getElementById('val-net-profit');
  const valTodayIncome = document.getElementById('val-today-income');
  const valTodayExpense = document.getElementById('val-today-expense');
  const valDebtBalance = document.getElementById('val-debt-balance');

  if (valNetProfit) {
    const prefix = state.dailyNetProfit >= 0 ? '+' : '';
    valNetProfit.textContent = `${prefix}${formatMoney(state.dailyNetProfit)}`;
  }
  if (valTodayIncome) {
    valTodayIncome.textContent = `+${formatMoney(state.todayIncome)}`;
  }
  if (valTodayExpense) {
    valTodayExpense.textContent = `-${formatMoney(state.todayExpense)}`;
  }
  if (valDebtBalance) {
    // Net debt calculation
    const borrowed = state.debts.filter(d => d.type === 'BORROWED' && !d.settled).reduce((s, d) => s + d.amount, 0);
    const lent = state.debts.filter(d => d.type === 'LENT' && !d.settled).reduce((s, d) => s + d.amount, 0);
    const diff = lent - borrowed;
    valDebtBalance.textContent = (diff >= 0 ? '+' : '') + formatMoney(diff);
  }

  // Dashboard Upcoming Tasks Preview (first 3 active tasks)
  const taskPreviewContainer = document.getElementById('dashboard-tasks-preview');
  if (taskPreviewContainer) {
    const activeTasks = state.tasks.filter(t => !t.completed).slice(0, 3);
    if (activeTasks.length === 0) {
      taskPreviewContainer.innerHTML = `
        <div class="empty-state-card">
          <span>🎉</span> Barcha vazifalar bajarilgan!
        </div>
      `;
    } else {
      taskPreviewContainer.innerHTML = activeTasks.map(task => createTaskCardHtml(task, true)).join('');
    }
  }

  // Dashboard Recent Transactions Preview (first 3)
  const txPreviewContainer = document.getElementById('dashboard-tx-preview');
  if (txPreviewContainer) {
    const recentTx = state.transactions.slice(0, 3);
    txPreviewContainer.innerHTML = recentTx.map(tx => createTxCardHtml(tx)).join('');
  }
}

function createTaskCardHtml(task, isCompact = false) {
  const isCompleted = task.completed;
  const isOverdue = !isCompleted && task.isOverdue;
  const hasPlannedAmount = task.plannedAmount != null;

  return `
    <div class="task-item-card ${isCompleted ? 'completed' : ''} ${isOverdue ? 'overdue' : ''}" data-task-id="${task.id}">
      <div class="task-checkbox ${isCompleted ? 'checked' : ''}" onclick="onTaskToggleClick(${task.id})">
        ${isCompleted ? '✓' : ''}
      </div>
      <div class="task-body">
        <div class="task-title-line">
          <span class="task-title ${isCompleted ? 'task-title-done' : ''}">${escapeHtml(task.title)}</span>
          ${isOverdue ? '<span class="task-badge badge-rose">Kechikkan</span>' : ''}
          ${task.dueTime ? `<span class="task-badge badge-blue">⏰ ${task.dueTime}</span>` : ''}
        </div>
        <div class="task-meta-line">
          ${task.dueDate ? `<span>🗓 ${task.dueDate}</span>` : '<span>Sanasiz</span>'}
          ${task.category ? `<span>• ${task.category}</span>` : ''}
          ${hasPlannedAmount ? `
            <span class="planned-amount-chip">
              💰 Reja: ${formatMoney(task.plannedAmount)}
            </span>
          ` : ''}
        </div>
      </div>
      <button class="task-action-btn" onclick="deleteTask(${task.id})" title="O‘chirish">🗑️</button>
    </div>
  `;
}

function createTxCardHtml(tx) {
  const isExpense = tx.type === 'expense';
  return `
    <div class="tx-card-item">
      <div class="tx-icon-bubble ${isExpense ? 'expense-bubble' : 'income-bubble'}">
        ${tx.icon || (isExpense ? '💸' : '💰')}
      </div>
      <div class="tx-info-block">
        <span class="tx-title">${escapeHtml(tx.title)}</span>
        <span class="tx-subtitle">${tx.category} • ${tx.method === 'CARD' ? '💳 Karta' : '💵 Naqd'} • ${tx.time}</span>
      </div>
      <div class="tx-amount ${isExpense ? 'tx-amount-expense' : 'tx-amount-income'}">
        ${isExpense ? '-' : '+'}${formatMoney(tx.amount)}
      </div>
    </div>
  `;
}

function renderTasks() {
  const container = document.getElementById('todo-tasks-container');
  if (!container) return;

  let filtered = [...state.tasks];
  const today = getTodayStr();

  if (state.filterTodo === 'today') {
    filtered = filtered.filter(t => t.dueDate === today && !t.completed);
  } else if (state.filterTodo === 'week') {
    filtered = filtered.filter(t => !t.completed);
  } else if (state.filterTodo === 'overdue') {
    filtered = filtered.filter(t => t.isOverdue && !t.completed);
  } else if (state.filterTodo === 'nodate') {
    filtered = filtered.filter(t => !t.dueDate && !t.completed);
  } else if (state.filterTodo === 'completed') {
    filtered = filtered.filter(t => t.completed);
  } else {
    // 'all'
    filtered = filtered.filter(t => !t.completed);
  }

  // Update counts on filter chips
  updateTodoFilterBadges();

  if (filtered.length === 0) {
    container.innerHTML = `
      <div class="empty-state-card">
        <div style="font-size: 32px; margin-bottom: 6px;">✨</div>
        <div style="font-weight: 600;">Hozircha hech qanday vazifa yo‘q</div>
        <div style="font-size: 12px; color: var(--text-dim); margin-top: 4px;">
          Tepadagi AI maydoniga "soat 12-da miting" kabi yozing
        </div>
      </div>
    `;
    return;
  }

  container.innerHTML = filtered.map(t => createTaskCardHtml(t)).join('');
}

function updateTodoFilterBadges() {
  const allCount = state.tasks.filter(t => !t.completed).length;
  const todayCount = state.tasks.filter(t => t.dueDate === getTodayStr() && !t.completed).length;
  const overdueCount = state.tasks.filter(t => t.isOverdue && !t.completed).length;
  const completedCount = state.tasks.filter(t => t.completed).length;

  document.querySelectorAll('.filter-chips-row .chip-btn').forEach(btn => {
    const f = btn.getAttribute('data-filter');
    if (f === 'all') btn.textContent = `Barchasi (${allCount})`;
    if (f === 'today') btn.textContent = `Bugun (${todayCount})`;
    if (f === 'overdue') btn.textContent = `Kechikkan (${overdueCount})`;
    if (f === 'completed') btn.textContent = `Bajarilgan (${completedCount})`;
  });
}

function renderTransactions() {
  const container = document.getElementById('finance-tx-list');
  const countBadge = document.getElementById('tx-count-badge');
  if (container) {
    container.innerHTML = state.transactions.map(tx => createTxCardHtml(tx)).join('');
  }
  if (countBadge) {
    countBadge.textContent = `${state.transactions.length} ta operatsiya`;
  }
}

function renderDebts() {
  const container = document.getElementById('debts-container');
  if (!container) return;

  let filtered = [...state.debts];
  if (state.filterDebt === 'borrowed') {
    filtered = filtered.filter(d => d.type === 'BORROWED');
  } else if (state.filterDebt === 'lent') {
    filtered = filtered.filter(d => d.type === 'LENT');
  }

  if (filtered.length === 0) {
    container.innerHTML = `<div class="empty-state-card">Qarzlar ro‘yxati bo‘sh</div>`;
    return;
  }

  container.innerHTML = filtered.map(debt => {
    const isBorrowed = debt.type === 'BORROWED';
    return `
      <div class="debt-card-item ${debt.settled ? 'settled' : ''}">
        <div style="display: flex; justify-content: space-between; align-items: flex-start;">
          <div>
            <div style="display: flex; align-items: center; gap: 8px;">
              <span style="font-weight: 700; font-size: 16px;">${escapeHtml(debt.personName)}</span>
              <span class="task-badge ${isBorrowed ? 'badge-rose' : 'badge-emerald'}">
                ${isBorrowed ? 'Men to‘layman' : 'Menga berishadi'}
              </span>
            </div>
            <div style="font-size: 12px; color: var(--text-dim); margin-top: 4px;">
              Muddati: <b>${debt.dueDate}</b>
            </div>
            ${debt.note ? `<div style="font-size: 12px; color: var(--text-muted); margin-top: 4px;">${escapeHtml(debt.note)}</div>` : ''}
          </div>
          <div style="text-align: right;">
            <div style="font-size: 17px; font-weight: 800; color: ${isBorrowed ? 'var(--rose)' : 'var(--emerald)'}; font-family: 'Outfit';">
              ${formatMoney(debt.amount)}
            </div>
            <button class="small-action-btn" style="margin-top: 8px;" onclick="settleDebt(${debt.id})">
              ${debt.settled ? 'Qaytarilgan ✓' : 'Yopish'}
            </button>
          </div>
        </div>
      </div>
    `;
  }).join('');
}

function renderCategories() {
  const grid = document.getElementById('categories-grid');
  if (!grid) return;

  grid.innerHTML = state.categories.map(cat => `
    <div class="category-grid-tile" style="border-left: 3px solid ${cat.color};">
      <div style="display: flex; justify-content: space-between; align-items: center;">
        <span style="font-size: 20px;">${cat.icon}</span>
        <span style="font-size: 11px; color: var(--text-dim);">Bu oy</span>
      </div>
      <div style="margin-top: 6px; font-weight: 700; font-size: 13px;">${cat.name}</div>
      <div style="font-size: 12px; font-weight: 800; color: var(--text-main); font-family: 'Outfit'; margin-top: 2px;">
        ${formatMoney(cat.monthlyTotal)}
      </div>
    </div>
  `).join('');
}

function renderProjects() {
  const container = document.getElementById('projects-container');
  if (!container) return;

  container.innerHTML = state.projects.map(p => {
    const pct = Math.round((p.completedTasks / p.totalTasks) * 100);
    return `
      <div class="project-card">
        <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px;">
          <div style="display: flex; align-items: center; gap: 8px;">
            <span style="width: 10px; height: 10px; border-radius: 50%; background: ${p.color};"></span>
            <span style="font-weight: 700; font-size: 14px;">${escapeHtml(p.title)}</span>
          </div>
          <span style="font-size: 12px; color: var(--text-muted); font-weight: 600;">${p.completedTasks}/${p.totalTasks}</span>
        </div>
        <div class="progress-track">
          <div class="progress-fill" style="width: ${pct}%; background: ${p.color};"></div>
        </div>
      </div>
    `;
  }).join('');
}

function renderAnalytics() {
  const chartContainer = document.getElementById('weekly-chart');
  if (!chartContainer) return;

  const days = [
    { label: 'Dush', inc: 350, exp: 120 },
    { label: 'Sesh', inc: 420, exp: 210 },
    { label: 'Chor', inc: 280, exp: 190 },
    { label: 'Pay', inc: 510, exp: 320 },
    { label: 'Juma', inc: 400, exp: 150 },
    { label: 'Shan', inc: 650, exp: 290 },
    { label: 'Yak', inc: 320, exp: 135 }
  ];

  const maxVal = 700;

  chartContainer.innerHTML = days.map(d => {
    const incH = Math.round((d.inc / maxVal) * 100);
    const expH = Math.round((d.exp / maxVal) * 100);

    return `
      <div class="chart-bar-group">
        <div class="chart-bars-duo">
          <div class="bar bar-income" style="height: ${incH}%;" title="Daromad: ${d.inc}k"></div>
          <div class="bar bar-expense" style="height: ${expH}%;" title="Xarajat: ${d.exp}k"></div>
        </div>
        <span class="chart-bar-label">${d.label}</span>
      </div>
    `;
  }).join('');
}

// =============================================================================
// 5. INTERACTIVE FLOWS & TASK-TO-EXPENSE CONFIRMATION
// =============================================================================

window.onTaskToggleClick = function(taskId) {
  const task = state.tasks.find(t => t.id === taskId);
  if (!task) return;

  triggerHaptic();

  // If task is already completed, simply toggle it back
  if (task.completed) {
    task.completed = false;
    showToast(`"${task.title}" qayta ochildi`, 'info', '🔄');
    renderAll();
    return;
  }

  // If task has a planned amount and is marked as expense candidate -> Trigger 2-Step Confirmation!
  if (task.plannedAmount && task.plannedAmount > 0) {
    openExpenseConfirmationModal(task);
  } else {
    // Normal completion without monetary expense
    task.completed = true;
    showToast(`"${task.title}" muvaffaqiyatli bajarildi!`, 'success', '✅');
    renderAll();
  }
};

function openExpenseConfirmationModal(task) {
  state.pendingTaskForExpense = task;
  const modal = document.getElementById('modal-expense-confirm');
  const desc = document.getElementById('modal-expense-confirm-desc');
  const writeBtn = document.getElementById('btn-modal-write-expense');

  if (desc) {
    desc.innerHTML = `
      <b>"${escapeHtml(task.title)}"</b> vazifasi bajarildi.<br>
      Rejadagi <b>${formatMoney(task.plannedAmount)}</b> haqiqiy xarajat sifatida daftarga yozilsinmi?
    `;
  }
  if (writeBtn) {
    writeBtn.textContent = `✅ Ha, xarajatga yozish (${formatMoney(task.plannedAmount)})`;
  }

  if (modal) {
    modal.classList.add('active');
  }
}

function closeExpenseConfirmationModal() {
  const modal = document.getElementById('modal-expense-confirm');
  if (modal) modal.classList.remove('active');
  state.pendingTaskForExpense = null;
}

window.deleteTask = function(taskId) {
  state.tasks = state.tasks.filter(t => t.id !== taskId);
  showToast('Vazifa o‘chirildi', 'info', '🗑️');
  renderAll();
};

window.settleDebt = function(debtId) {
  const debt = state.debts.find(d => d.id === debtId);
  if (debt) {
    debt.settled = !debt.settled;
    triggerHaptic();
    showToast(`${debt.personName} bilan hisob-kitob yangilandi!`, 'success', '🤝');
    renderAll();
  }
};

// =============================================================================
// 6. MODAL & EVENT LISTENERS SETUP
// =============================================================================

document.addEventListener('DOMContentLoaded', () => {

  // A. Tab Navigation
  const tabButtons = document.querySelectorAll('.nav-tab-btn');
  tabButtons.forEach(btn => {
    btn.addEventListener('click', () => {
      const targetId = btn.getAttribute('data-target');
      switchTab(targetId);
    });
  });

  // Header and Dashboard Quick links to tabs
  document.getElementById('link-view-all-tasks')?.addEventListener('click', () => switchTab('tab-todo'));
  document.getElementById('link-view-all-tx')?.addEventListener('click', () => switchTab('tab-finance'));

  // B. Quick Action Tiles on Dashboard
  document.getElementById('quick-add-expense')?.addEventListener('click', () => openTransactionModal('expense'));
  document.getElementById('quick-add-income')?.addEventListener('click', () => openTransactionModal('income'));
  document.getElementById('quick-add-task')?.addEventListener('click', () => {
    switchTab('tab-todo');
    document.getElementById('input-smart-todo')?.focus();
  });
  document.getElementById('quick-add-debt')?.addEventListener('click', () => {
    switchTab('tab-debts');
  });

  // C. Smart NLP Task Submission
  const smartTodoInput = document.getElementById('input-smart-todo');
  const smartTodoBtn = document.getElementById('btn-smart-todo-submit');

  function handleSmartTodoSubmit() {
    if (!smartTodoInput) return;
    const text = smartTodoInput.value.trim();
    if (!text) {
      showToast('Iltimos, vazifa matnini kiriting', 'error', '⚠️');
      return;
    }

    const parsed = parseNlpInput(text);
    if (!parsed) return;

    const newTask = {
      id: Date.now(),
      title: parsed.title,
      dueDate: parsed.dueDate,
      dueTime: parsed.dueTime,
      plannedAmount: parsed.plannedAmount,
      isExpenseCandidate: parsed.isExpenseCandidate,
      category: parsed.isExpenseCandidate ? 'Xarajat' : 'Umumiy',
      completed: false,
      isOverdue: false,
      projectId: null
    };

    state.tasks.unshift(newTask);
    smartTodoInput.value = '';

    let feedbackMsg = `Vazifa saqlandi: "${newTask.title}"`;
    if (newTask.dueTime) feedbackMsg += ` [⏰ ${newTask.dueTime}]`;
    if (newTask.plannedAmount) feedbackMsg += ` [💰 Reja: ${formatMoney(newTask.plannedAmount)}]`;

    showToast(feedbackMsg, 'success', '🧠');
    renderAll();
  }

  smartTodoBtn?.addEventListener('click', handleSmartTodoSubmit);
  smartTodoInput?.addEventListener('keydown', (e) => {
    if (e.key === 'Enter') handleSmartTodoSubmit();
  });

  // D. Filter Chips on Todo Tab
  document.querySelectorAll('.filter-chips-row .chip-btn').forEach(chip => {
    chip.addEventListener('click', () => {
      document.querySelectorAll('.filter-chips-row .chip-btn').forEach(c => c.classList.remove('active'));
      chip.classList.add('active');
      state.filterTodo = chip.getAttribute('data-filter') || 'all';
      renderTasks();
    });
  });

  // E. Filter Chips on Debts Tab
  document.querySelectorAll('#tab-debts .chip-btn').forEach(chip => {
    chip.addEventListener('click', () => {
      document.querySelectorAll('#tab-debts .chip-btn').forEach(c => c.classList.remove('active'));
      chip.classList.add('active');
      state.filterDebt = chip.getAttribute('data-debt-type') || 'all';
      renderDebts();
    });
  });

  // F. 2-Step Expense Confirmation Modal Buttons
  // Option 1: Write to Expense
  document.getElementById('btn-modal-write-expense')?.addEventListener('click', () => {
    const task = state.pendingTaskForExpense;
    if (!task) return;

    task.completed = true;

    // Create corresponding transaction
    const newTx = {
      id: Date.now(),
      type: 'expense',
      title: task.title,
      category: task.category || 'Rejali Xarajat',
      amount: task.plannedAmount,
      method: 'CARD',
      time: new Date().toLocaleTimeString('uz-UZ', { hour: '2-digit', minute: '2-digit' }),
      date: 'Bugun',
      icon: '💸'
    };

    state.transactions.unshift(newTx);
    state.todayExpense += task.plannedAmount;
    state.dailyNetProfit = state.todayIncome - state.todayExpense;

    closeExpenseConfirmationModal();
    showToast(`Xarajat yozildi: -${formatMoney(task.plannedAmount)}`, 'success', '💸');
    renderAll();
  });

  // Option 2: Edit Expense Amount Before Recording
  document.getElementById('btn-modal-edit-expense-amount')?.addEventListener('click', () => {
    const task = state.pendingTaskForExpense;
    if (!task) return;

    const newAmountStr = prompt('Haqiqatda qancha summa sarfladingiz? (so‘mda):', task.plannedAmount);
    if (newAmountStr !== null) {
      const parsedAmount = parseInt(newAmountStr.replace(/\D/g, ''), 10);
      if (!isNaN(parsedAmount) && parsedAmount > 0) {
        task.completed = true;
        task.plannedAmount = parsedAmount;

        const newTx = {
          id: Date.now(),
          type: 'expense',
          title: task.title,
          category: task.category || 'Xarajat',
          amount: parsedAmount,
          method: 'CARD',
          time: new Date().toLocaleTimeString('uz-UZ', { hour: '2-digit', minute: '2-digit' }),
          date: 'Bugun',
          icon: '💸'
        };

        state.transactions.unshift(newTx);
        state.todayExpense += parsedAmount;
        state.dailyNetProfit = state.todayIncome - state.todayExpense;

        closeExpenseConfirmationModal();
        showToast(`Tuzatilgan xarajat yozildi: -${formatMoney(parsedAmount)}`, 'success', '💸');
        renderAll();
      }
    }
  });

  // Option 3: Complete task ONLY (no expense recorded)
  document.getElementById('btn-modal-close-task-only')?.addEventListener('click', () => {
    const task = state.pendingTaskForExpense;
    if (task) {
      task.completed = true;
      closeExpenseConfirmationModal();
      showToast(`Vazifa yopildi (pul hisoblanmadi)`, 'info', '✔️');
      renderAll();
    }
  });

  // G. Voice Simulator Modal
  const voiceModal = document.getElementById('modal-voice');
  const btnFloatingVoice = document.getElementById('btn-floating-voice');
  const btnHeaderVoice = document.getElementById('btn-header-voice');
  const btnCloseVoice = document.getElementById('btn-close-voice-modal');

  function openVoiceModal() {
    voiceModal?.classList.add('active');
    triggerHaptic();
  }
  function closeVoiceModal() {
    voiceModal?.classList.remove('active');
  }

  btnFloatingVoice?.addEventListener('click', openVoiceModal);
  btnHeaderVoice?.addEventListener('click', openVoiceModal);
  btnCloseVoice?.addEventListener('click', closeVoiceModal);

  // Voice Preset clicks
  document.querySelectorAll('.voice-preset-btn').forEach(btn => {
    btn.addEventListener('click', () => {
      const phrase = btn.getAttribute('data-text');
      closeVoiceModal();

      showToast(`Ovoz qabul qilindi: "${phrase}"`, 'info', '🎙️');

      setTimeout(() => {
        const parsed = parseNlpInput(phrase);
        if (parsed) {
          // If phrase is pure expense like "15 ming obedga"
          if (phrase.includes('obedga') || phrase.includes('tushlik')) {
            const newTx = {
              id: Date.now(),
              type: 'expense',
              title: 'Obed / Tushlik',
              category: 'Tushlik',
              amount: parsed.plannedAmount || 15000,
              method: 'CASH',
              time: 'Hozir',
              date: 'Bugun',
              icon: '🍔'
            };
            state.transactions.unshift(newTx);
            state.todayExpense += newTx.amount;
            state.dailyNetProfit = state.todayIncome - state.todayExpense;
            showToast(`Ovoz orqali xarajat qo‘shildi: -${formatMoney(newTx.amount)}`, 'success', '🍔');
          } else {
            // It's a task
            const newTask = {
              id: Date.now(),
              title: parsed.title,
              dueDate: parsed.dueDate,
              dueTime: parsed.dueTime,
              plannedAmount: parsed.plannedAmount,
              isExpenseCandidate: parsed.isExpenseCandidate,
              category: parsed.isExpenseCandidate ? 'To‘lov' : 'Reja',
              completed: false,
              isOverdue: false,
              projectId: null
            };
            state.tasks.unshift(newTask);

            let msg = `Ovozli vazifa: "${newTask.title}"`;
            if (newTask.dueTime) msg += ` (⏰ ${newTask.dueTime})`;
            if (newTask.plannedAmount) {
              msg += ` (💰 Reja: ${formatMoney(newTask.plannedAmount)})`;
            } else {
              msg += ` (Summasiz reja ✅)`;
            }
            showToast(msg, 'success', '✅');
          }
          renderAll();
        }
      }, 700);
    });
  });

  // H. Transaction Add Modal
  const txModal = document.getElementById('modal-transaction');
  const btnCancelTx = document.getElementById('btn-cancel-transaction');
  const btnSaveTx = document.getElementById('btn-save-transaction');

  let currentTxMode = 'expense';

  function openTransactionModal(mode = 'expense') {
    currentTxMode = mode;
    const title = document.getElementById('modal-tx-title');
    if (title) title.textContent = mode === 'expense' ? '💸 Yangi Xarajat' : '💰 Yangi Daromad';
    document.getElementById('input-tx-amount').value = '';
    document.getElementById('input-tx-desc').value = '';
    txModal?.classList.add('active');
  }

  btnCancelTx?.addEventListener('click', () => {
    txModal?.classList.remove('active');
  });

  btnSaveTx?.addEventListener('click', () => {
    const amountVal = parseInt(document.getElementById('input-tx-amount').value, 10);
    const category = document.getElementById('select-tx-category').value;
    const desc = document.getElementById('input-tx-desc').value.trim();

    if (isNaN(amountVal) || amountVal <= 0) {
      showToast('Iltimos, summani to‘g‘ri kiriting', 'error', '⚠️');
      return;
    }

    const newTx = {
      id: Date.now(),
      type: currentTxMode,
      title: desc || (currentTxMode === 'expense' ? category : 'Qo‘shimcha daromad'),
      category: category,
      amount: amountVal,
      method: 'CASH',
      time: new Date().toLocaleTimeString('uz-UZ', { hour: '2-digit', minute: '2-digit' }),
      date: 'Bugun',
      icon: currentTxMode === 'expense' ? '💸' : '💰'
    };

    state.transactions.unshift(newTx);
    if (currentTxMode === 'expense') {
      state.todayExpense += amountVal;
    } else {
      state.todayIncome += amountVal;
    }
    state.dailyNetProfit = state.todayIncome - state.todayExpense;

    txModal?.classList.remove('active');
    showToast(`${currentTxMode === 'expense' ? 'Xarajat' : 'Daromad'} saqlandi: ${formatMoney(amountVal)}`, 'success', '💾');
    renderAll();
  });

  // I. Toggle Workday
  document.getElementById('btn-toggle-workday')?.addEventListener('click', () => {
    state.isWorkDay = !state.isWorkDay;
    const badge = document.getElementById('btn-toggle-workday');
    if (badge) {
      badge.textContent = state.isWorkDay ? '💼 Ish kuni' : '🏖 Dam olish';
      badge.style.background = state.isWorkDay ? 'rgba(16, 185, 129, 0.2)' : 'rgba(245, 158, 11, 0.2)';
    }
    showToast(`Kun rejimi: ${state.isWorkDay ? 'Ish kuni' : 'Dam olish kuni'}`, 'info', '🗓');
  });

  // J. Telegram WebApp Integration Init
  if (window.Telegram && window.Telegram.WebApp) {
    try {
      window.Telegram.WebApp.ready();
      window.Telegram.WebApp.expand();
      if (window.Telegram.WebApp.MainButton) {
        window.Telegram.WebApp.MainButton.hide();
      }
    } catch (e) {
      console.log('Telegram WebApp error', e);
    }
  }

  // Initial render
  renderAll();
});

// Helper for tab switching
function switchTab(targetId) {
  document.querySelectorAll('.app-tab-section').forEach(sec => {
    sec.style.display = 'none';
    sec.classList.remove('active');
  });

  const activeSec = document.getElementById(targetId);
  if (activeSec) {
    activeSec.style.display = 'block';
    activeSec.classList.add('active');
  }

  document.querySelectorAll('.nav-tab-btn').forEach(btn => {
    if (btn.getAttribute('data-target') === targetId) {
      btn.classList.add('active');
    } else {
      btn.classList.remove('active');
    }
  });

  state.activeTab = targetId;
  triggerHaptic();
  window.scrollTo({ top: 0, behavior: 'smooth' });
}

function escapeHtml(str) {
  if (!str) return '';
  return String(str)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#039;');
}
