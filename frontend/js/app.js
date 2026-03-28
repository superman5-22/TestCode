/**
 * LumberCore — アプリケーションロジック
 * ナビゲーション制御・各画面レンダリング
 */

// ============================================================
// ナビゲーション
// ============================================================
const SECTION_TITLES = {
  dashboard: 'ダッシュボード',
  orders:    '受注管理',
  invoices:  '請求管理',
  stock:     '在庫管理',
  products:  '商品マスタ',
  customers: '顧客マスタ',
};

const SECTION_RENDERERS = {
  dashboard: renderDashboard,
  orders:    renderOrders,
  invoices:  renderInvoices,
  stock:     renderStock,
  products:  renderProducts,
  customers: renderCustomers,
};

function navigate(section) {
  // アクティブ状態切替
  document.querySelectorAll('.nav-item').forEach(el => el.classList.remove('active'));
  document.querySelectorAll('.section').forEach(el => el.classList.remove('active'));

  const navEl = document.querySelector(`.nav-item[data-section="${section}"]`);
  const sectionEl = document.getElementById(`section-${section}`);

  if (navEl) navEl.classList.add('active');
  if (sectionEl) sectionEl.classList.add('active');

  // トップバーのタイトル更新
  const titleEl = document.getElementById('topbar-title');
  if (titleEl) titleEl.textContent = SECTION_TITLES[section] || section;

  // セクション初期化
  if (SECTION_RENDERERS[section]) SECTION_RENDERERS[section]();
}

// ============================================================
// フォーマットユーティリティ
// ============================================================
function formatCurrency(amount) {
  return new Intl.NumberFormat('ja-JP', { style: 'currency', currency: 'JPY' }).format(amount);
}

function formatDate(dateStr) {
  if (!dateStr) return '—';
  const d = new Date(dateStr);
  return `${d.getFullYear()}/${String(d.getMonth() + 1).padStart(2, '0')}/${String(d.getDate()).padStart(2, '0')}`;
}

/**
 * ステータスバッジ HTML を返す
 * 色は意味ごとに分離した CSS クラスで管理
 */
function statusBadge(status, label) {
  const cls = {
    DELIVERED: 'status-delivered',
    CONFIRMED: 'status-confirmed',
    PENDING:   'status-pending',
    CANCELLED: 'status-cancelled',
    PAID:      'status-paid',
    UNPAID:    'status-unpaid',
    OVERDUE:   'status-overdue',
  }[status] || '';
  return `<span class="status ${cls}">${label}</span>`;
}

/**
 * 在庫バー HTML を返す
 * 満量基準: threshold × 2
 */
function stockBar(quantity, threshold) {
  if (!threshold) return '';
  const pct    = Math.min(100, Math.round((quantity / (threshold * 2)) * 100));
  const fillCls = quantity <= threshold
    ? 'stock-bar__fill--low'
    : quantity <= threshold * 1.5
    ? 'stock-bar__fill--warn'
    : '';
  return `<div class="stock-bar-wrap">
    <div class="stock-bar"><div class="stock-bar__fill ${fillCls}" style="width:${pct}%"></div></div>
  </div>`;
}

// ============================================================
// ダッシュボード
// ============================================================
function getGreeting() {
  const h = new Date().getHours();
  if (h < 10) return 'おはようございます';
  if (h < 14) return 'お疲れさまです';
  if (h < 18) return 'お疲れさまです';
  return 'お疲れさまでした';
}

function renderDashboard() {
  const d   = new Date();
  const day = ['日','月','火','水','木','金','土'][d.getDay()];
  const dateStr = `${d.getMonth() + 1}月${d.getDate()}日（${day}）`;

  const dateEl = document.getElementById('dash-date');
  const greetEl = document.getElementById('dash-greeting');
  if (dateEl) dateEl.textContent = dateStr;
  if (greetEl) greetEl.textContent = getGreeting();

  // KPI 集計
  const totalOrders  = DUMMY_ORDERS.length;
  const pendingOrders = DUMMY_ORDERS.filter(o => o.status === 'PENDING').length;
  const totalRevenue = DUMMY_ORDERS.reduce((sum, o) => sum + o.totalAmount, 0);
  const unpaidInvoices = DUMMY_INVOICES.filter(i => i.status === 'UNPAID').length;
  const overdueInvoices = DUMMY_INVOICES.filter(i => i.status === 'OVERDUE').length;
  const lowStock     = DUMMY_STOCKS.filter(s => s.quantity <= s.threshold).length;

  // KPI 値セット
  document.getElementById('kpi-orders').textContent    = `${totalOrders}件`;
  document.getElementById('kpi-pending').textContent   = `${pendingOrders}件`;
  document.getElementById('kpi-revenue').textContent   = formatCurrency(totalRevenue);
  document.getElementById('kpi-unpaid').textContent    = `${unpaidInvoices}件`;
  document.getElementById('kpi-lowstock').textContent  = `${lowStock}品目`;

  // 確認待ち受注カードの状態
  const pendingCard = document.getElementById('kpi-card-pending');
  const pendingHint = document.getElementById('kpi-pending-hint');
  const pendingVal  = document.getElementById('kpi-pending');
  if (pendingOrders > 0) {
    pendingCard.classList.add('kpi-card--urgent');
    pendingVal.classList.add('kpi-value--urgent');
    pendingHint.textContent = '確認・承認が必要です';
    pendingHint.classList.add('kpi-hint--urgent');
  } else {
    pendingHint.textContent = '未確認の受注はありません';
  }

  // 在庫不足カードの状態
  const stockCard = document.getElementById('kpi-card-stock');
  const stockHint = document.getElementById('kpi-stock-hint');
  if (lowStock > 0) {
    stockCard.classList.add('kpi-card--warn');
    stockHint.textContent = '補充を検討してください';
    stockHint.classList.add('kpi-hint--urgent');
  }

  // 未払い・期限超過ヒント
  const unpaidHint = document.getElementById('kpi-unpaid-hint');
  if (overdueInvoices > 0) {
    unpaidHint.textContent = `うち期限超過 ${overdueInvoices}件`;
    unpaidHint.classList.add('kpi-hint--urgent');
  } else if (unpaidInvoices > 0) {
    unpaidHint.textContent = '期限超過はありません';
  }

  // ナビバッジ更新
  updateNavBadge(pendingOrders);

  // 直近受注テーブル
  const tbody = document.getElementById('recent-orders-body');
  if (!tbody) return;

  if (DUMMY_ORDERS.length === 0) {
    tbody.innerHTML = `<tr><td colspan="5" class="state-cell">まだ受注データがありません</td></tr>`;
    return;
  }

  tbody.innerHTML = DUMMY_ORDERS.slice(0, 5).map(o => `
    <tr>
      <td><span class="cell-code">${o.orderNo}</span></td>
      <td>${o.customerName}</td>
      <td>${formatDate(o.orderDate)}</td>
      <td class="cell-amount">${formatCurrency(o.totalAmount)}</td>
      <td>${statusBadge(o.status, o.statusLabel)}</td>
    </tr>
  `).join('');
}

// ナビゲーションバッジ（確認待ち件数）
function updateNavBadge(pendingCount) {
  const badge = document.getElementById('nav-pending-badge');
  if (!badge) return;
  if (pendingCount > 0) {
    badge.textContent = pendingCount;
    badge.style.display = 'inline-flex';
  } else {
    badge.style.display = 'none';
  }
}

// ============================================================
// 受注管理
// ============================================================
function renderOrders(orders = DUMMY_ORDERS) {
  const tbody = document.getElementById('orders-body');
  if (!tbody) return;

  if (orders.length === 0) {
    tbody.innerHTML = `<tr><td colspan="7" class="state-cell">条件に一致する受注がありません</td></tr>`;
    return;
  }

  tbody.innerHTML = orders.map(o => `
    <tr>
      <td><span class="cell-code">${o.orderNo}</span></td>
      <td>${o.customerName}</td>
      <td>${formatDate(o.orderDate)}</td>
      <td>${formatDate(o.deliveryDate)}</td>
      <td class="cell-amount">${formatCurrency(o.totalAmount)}</td>
      <td>${statusBadge(o.status, o.statusLabel)}</td>
      <td><button class="t-link" onclick="showOrderDetail(${o.id})">詳細</button></td>
    </tr>
  `).join('');
}

function showOrderDetail(orderId) {
  const order = DUMMY_ORDERS.find(o => o.id === orderId);
  if (!order) return;

  const itemsHtml = (order.items || []).map(item => `
    <tr>
      <td>${item.productName}</td>
      <td class="text-r">${item.quantity}</td>
      <td class="text-r">${formatCurrency(item.unitPrice)}</td>
      <td class="text-r fw-600">${formatCurrency(item.subtotal)}</td>
    </tr>
  `).join('');

  document.getElementById('modal-title').textContent = `受注詳細 — ${order.orderNo}`;
  document.getElementById('modal-body').innerHTML = `
    <div class="detail-grid">
      <div><p class="detail-item__label">受注番号</p><p class="detail-item__value cell-code">${order.orderNo}</p></div>
      <div><p class="detail-item__label">顧客名</p><p class="detail-item__value">${order.customerName}</p></div>
      <div><p class="detail-item__label">受注日</p><p class="detail-item__value">${formatDate(order.orderDate)}</p></div>
      <div><p class="detail-item__label">納品予定日</p><p class="detail-item__value">${formatDate(order.deliveryDate)}</p></div>
      <div><p class="detail-item__label">ステータス</p><p class="detail-item__value">${statusBadge(order.status, order.statusLabel)}</p></div>
      <div><p class="detail-item__label">合計金額</p><p class="detail-item__value fw-600">${formatCurrency(order.totalAmount)}</p></div>
    </div>
    <p class="modal__sub-title">受注明細</p>
    <div class="table-wrap">
      <table class="data-table">
        <thead>
          <tr><th>商品名</th><th class="col-r">数量</th><th class="col-r">単価</th><th class="col-r">小計</th></tr>
        </thead>
        <tbody>${itemsHtml || '<tr><td colspan="4" class="state-cell">明細データなし</td></tr>'}</tbody>
      </table>
    </div>
  `;
  openModal();
}

function filterOrders() {
  const kw     = (document.getElementById('order-search')?.value || '').toLowerCase();
  const status = document.getElementById('order-status-filter')?.value || '';
  const filtered = DUMMY_ORDERS.filter(o => {
    const matchKw = !kw || o.orderNo.toLowerCase().includes(kw) || o.customerName.toLowerCase().includes(kw);
    const matchSt = !status || o.status === status;
    return matchKw && matchSt;
  });
  renderOrders(filtered);
}

async function fetchOrdersFromAPI() {
  const tbody = document.getElementById('orders-body');
  if (tbody) tbody.innerHTML = `<tr><td colspan="7" class="state-cell">サーバーに問い合わせ中...</td></tr>`;
  try {
    const data = await apiFetch('/api/orders');
    renderOrders(data);
  } catch (e) {
    if (tbody) tbody.innerHTML = `<tr><td colspan="7" class="state-cell state-cell--error">通信エラーが発生しました。ダミーデータを表示しています。</td></tr>`;
    setTimeout(() => renderOrders(DUMMY_ORDERS), 800);
  }
}

// ============================================================
// 請求管理
// ============================================================
function renderInvoices(invoices = DUMMY_INVOICES) {
  const tbody = document.getElementById('invoices-body');
  if (!tbody) return;

  if (invoices.length === 0) {
    tbody.innerHTML = `<tr><td colspan="8" class="state-cell">条件に一致する請求書がありません</td></tr>`;
    return;
  }

  tbody.innerHTML = invoices.map(inv => `
    <tr${inv.status === 'OVERDUE' ? ' class="row-warn"' : ''}>
      <td><span class="cell-code">${inv.invoiceNo}</span></td>
      <td>${inv.customerName}</td>
      <td><span class="cell-code color-muted">${inv.orderNo}</span></td>
      <td>${formatDate(inv.issueDate)}</td>
      <td${inv.status === 'OVERDUE' ? ' class="color-danger"' : ''}>${formatDate(inv.dueDate)}</td>
      <td class="cell-amount">${formatCurrency(inv.totalWithTax)}</td>
      <td>${statusBadge(inv.status, inv.statusLabel)}</td>
      <td><button class="t-link" onclick="showInvoiceDetail(${inv.id})">詳細</button></td>
    </tr>
  `).join('');
}

function showInvoiceDetail(invoiceId) {
  const inv = DUMMY_INVOICES.find(i => i.id === invoiceId);
  if (!inv) return;

  document.getElementById('modal-title').textContent = `請求書 — ${inv.invoiceNo}`;
  document.getElementById('modal-body').innerHTML = `
    <div class="detail-grid">
      <div><p class="detail-item__label">請求書番号</p><p class="detail-item__value cell-code">${inv.invoiceNo}</p></div>
      <div><p class="detail-item__label">顧客名</p><p class="detail-item__value">${inv.customerName}</p></div>
      <div><p class="detail-item__label">受注番号</p><p class="detail-item__value cell-code color-muted">${inv.orderNo}</p></div>
      <div><p class="detail-item__label">発行日</p><p class="detail-item__value">${formatDate(inv.issueDate)}</p></div>
      <div><p class="detail-item__label">支払期限</p><p class="detail-item__value">${formatDate(inv.dueDate)}</p></div>
      <div><p class="detail-item__label">支払状態</p><p class="detail-item__value">${statusBadge(inv.status, inv.statusLabel)}</p></div>
    </div>
    <div class="amount-summary">
      <div class="amount-row">
        <span>小計（税抜）</span>
        <span class="amount-row__val">${formatCurrency(inv.amount)}</span>
      </div>
      <div class="amount-row">
        <span>消費税（10%）</span>
        <span class="amount-row__val">${formatCurrency(inv.tax)}</span>
      </div>
      <div class="amount-row amount-row--total">
        <span>請求合計（税込）</span>
        <span class="amount-row__val">${formatCurrency(inv.totalWithTax)}</span>
      </div>
    </div>
  `;
  openModal();
}

function filterInvoices() {
  const kw     = (document.getElementById('invoice-search')?.value || '').toLowerCase();
  const status = document.getElementById('invoice-status-filter')?.value || '';
  const filtered = DUMMY_INVOICES.filter(inv => {
    const matchKw = !kw || inv.invoiceNo.toLowerCase().includes(kw) || inv.customerName.toLowerCase().includes(kw);
    const matchSt = !status || inv.status === status;
    return matchKw && matchSt;
  });
  renderInvoices(filtered);
}

async function fetchInvoicesFromAPI() {
  const tbody = document.getElementById('invoices-body');
  if (tbody) tbody.innerHTML = `<tr><td colspan="8" class="state-cell">サーバーに問い合わせ中...</td></tr>`;
  try {
    const data = await apiFetch('/api/invoices');
    renderInvoices(data);
  } catch (e) {
    if (tbody) tbody.innerHTML = `<tr><td colspan="8" class="state-cell state-cell--error">通信エラー — ダミーデータに切り替えます</td></tr>`;
    setTimeout(() => renderInvoices(DUMMY_INVOICES), 800);
  }
}

// ============================================================
// 在庫管理
// ============================================================
function renderStock(stocks = DUMMY_STOCKS) {
  const tbody = document.getElementById('stock-body');
  if (!tbody) return;

  if (stocks.length === 0) {
    tbody.innerHTML = `<tr><td colspan="5" class="state-cell">在庫データがありません</td></tr>`;
    return;
  }

  tbody.innerHTML = stocks.map(s => {
    const isLow = s.quantity <= s.threshold;
    return `
      <tr${isLow ? ' class="row-warn"' : ''}>
        <td>${s.productName}</td>
        <td class="text-r text-mono${isLow ? ' color-danger' : ''}">${s.quantity.toLocaleString()} ${s.unit}</td>
        <td class="text-r text-mono color-muted">${s.threshold.toLocaleString()} ${s.unit}</td>
        <td>${stockBar(s.quantity, s.threshold)}</td>
        <td>${isLow
          ? statusBadge('UNPAID', '要補充')
          : statusBadge('DELIVERED', '正常')}</td>
      </tr>
    `;
  }).join('');
}

async function fetchStockFromAPI() {
  const tbody = document.getElementById('stock-body');
  if (tbody) tbody.innerHTML = `<tr><td colspan="5" class="state-cell">在庫情報を取得中...</td></tr>`;
  try {
    const data = await apiFetch('/api/stocks');
    renderStock(data);
  } catch (e) {
    if (tbody) tbody.innerHTML = `<tr><td colspan="5" class="state-cell state-cell--error">通信エラー — ダミーデータに切り替えます</td></tr>`;
    setTimeout(() => renderStock(DUMMY_STOCKS), 800);
  }
}

// ============================================================
// 商品マスタ
// ============================================================
function renderProducts(products = DUMMY_PRODUCTS) {
  const tbody = document.getElementById('products-body');
  if (!tbody) return;

  if (products.length === 0) {
    tbody.innerHTML = `<tr><td colspan="4" class="state-cell">商品データがありません</td></tr>`;
    return;
  }

  tbody.innerHTML = products.map(p => `
    <tr>
      <td><span class="cell-code">${p.code}</span></td>
      <td>${p.name}</td>
      <td class="color-muted">${p.unit}</td>
      <td class="cell-amount">${formatCurrency(p.price)}</td>
    </tr>
  `).join('');
}

async function fetchProductsFromAPI() {
  const tbody = document.getElementById('products-body');
  if (tbody) tbody.innerHTML = `<tr><td colspan="4" class="state-cell">取得中...</td></tr>`;
  try {
    const data = await apiFetch('/api/products');
    renderProducts(data);
  } catch (e) {
    setTimeout(() => renderProducts(DUMMY_PRODUCTS), 500);
  }
}

// ============================================================
// 顧客マスタ
// ============================================================
function renderCustomers(customers = DUMMY_CUSTOMERS) {
  const tbody = document.getElementById('customers-body');
  if (!tbody) return;

  if (customers.length === 0) {
    tbody.innerHTML = `<tr><td colspan="4" class="state-cell">顧客データがありません</td></tr>`;
    return;
  }

  tbody.innerHTML = customers.map(c => `
    <tr>
      <td><span class="cell-code">${c.code}</span></td>
      <td>${c.name}</td>
      <td>${c.contact || '—'}</td>
      <td class="color-muted">${c.tel || '—'}</td>
    </tr>
  `).join('');
}

async function fetchCustomersFromAPI() {
  const tbody = document.getElementById('customers-body');
  if (tbody) tbody.innerHTML = `<tr><td colspan="4" class="state-cell">取得中...</td></tr>`;
  try {
    const data = await apiFetch('/api/customers');
    renderCustomers(data);
  } catch (e) {
    setTimeout(() => renderCustomers(DUMMY_CUSTOMERS), 500);
  }
}

// ============================================================
// モーダル
// ============================================================
function openModal() {
  const overlay = document.getElementById('modal-overlay');
  overlay.classList.add('is-open');
  // フォーカス管理
  const closeBtn = overlay.querySelector('.modal__close');
  if (closeBtn) closeBtn.focus();
}

function closeModal() {
  document.getElementById('modal-overlay').classList.remove('is-open');
}

// ============================================================
// 初期化
// ============================================================
document.addEventListener('DOMContentLoaded', () => {
  // ナビゲーションイベント
  document.querySelectorAll('.nav-item').forEach(el => {
    el.addEventListener('click', e => {
      e.preventDefault();
      navigate(el.dataset.section);
    });
    // キーボード対応
    el.addEventListener('keydown', e => {
      if (e.key === 'Enter' || e.key === ' ') {
        e.preventDefault();
        navigate(el.dataset.section);
      }
    });
  });

  // モーダル背景クリックで閉じる
  document.getElementById('modal-overlay').addEventListener('click', e => {
    if (e.target === e.currentTarget) closeModal();
  });

  // ESCキーでモーダルを閉じる
  document.addEventListener('keydown', e => {
    if (e.key === 'Escape') closeModal();
  });

  // 初期ページ
  navigate('dashboard');
});
