/**
 * メインアプリケーションロジック
 * ナビゲーション制御・各画面のレンダリング
 */

// ============================================================
// ナビゲーション制御
// ============================================================
function navigate(section) {
  document.querySelectorAll(".nav-link").forEach((el) => el.classList.remove("active"));
  document.querySelectorAll(".section").forEach((el) => el.classList.remove("active"));

  const navEl = document.querySelector(`[data-section="${section}"]`);
  const sectionEl = document.getElementById(`section-${section}`);
  if (navEl) navEl.classList.add("active");
  if (sectionEl) sectionEl.classList.add("active");

  // 各セクション初期化
  const handlers = {
    dashboard: renderDashboard,
    orders: renderOrders,
    invoices: renderInvoices,
    stock: renderStock,
    products: renderProducts,
    customers: renderCustomers,
  };
  if (handlers[section]) handlers[section]();
}

// ============================================================
// フォーマットユーティリティ
// ============================================================
function formatCurrency(amount) {
  return new Intl.NumberFormat("ja-JP", { style: "currency", currency: "JPY" }).format(amount);
}

function formatDate(dateStr) {
  if (!dateStr) return "-";
  const d = new Date(dateStr);
  return `${d.getFullYear()}/${String(d.getMonth() + 1).padStart(2, "0")}/${String(d.getDate()).padStart(2, "0")}`;
}

function statusBadge(status, label) {
  const colorMap = {
    DELIVERED: "badge-green",
    CONFIRMED: "badge-blue",
    PENDING: "badge-yellow",
    CANCELLED: "badge-red",
    PAID: "badge-green",
    UNPAID: "badge-red",
    OVERDUE: "badge-red",
  };
  return `<span class="badge ${colorMap[status] || "badge-gray"}">${label}</span>`;
}

// ============================================================
// ダッシュボード
// ============================================================
function renderDashboard() {
  const totalOrders = DUMMY_ORDERS.length;
  const pendingOrders = DUMMY_ORDERS.filter((o) => o.status === "PENDING").length;
  const totalRevenue = DUMMY_ORDERS.reduce((sum, o) => sum + o.totalAmount, 0);
  const unpaidInvoices = DUMMY_INVOICES.filter((i) => i.status === "UNPAID").length;
  const lowStock = DUMMY_STOCKS.filter((s) => s.quantity <= s.threshold).length;

  document.getElementById("kpi-orders").textContent = totalOrders;
  document.getElementById("kpi-pending").textContent = pendingOrders;
  document.getElementById("kpi-revenue").textContent = formatCurrency(totalRevenue);
  document.getElementById("kpi-unpaid").textContent = unpaidInvoices;
  document.getElementById("kpi-lowstock").textContent = lowStock;

  // 直近注文テーブル
  const tbody = document.getElementById("recent-orders-body");
  tbody.innerHTML = DUMMY_ORDERS.slice(0, 5)
    .map(
      (o) => `
    <tr>
      <td>${o.orderNo}</td>
      <td>${o.customerName}</td>
      <td>${formatDate(o.orderDate)}</td>
      <td>${formatCurrency(o.totalAmount)}</td>
      <td>${statusBadge(o.status, o.statusLabel)}</td>
    </tr>`
    )
    .join("");
}

// ============================================================
// 受注一覧
// ============================================================
function renderOrders(orders = DUMMY_ORDERS) {
  const tbody = document.getElementById("orders-body");
  tbody.innerHTML = orders
    .map(
      (o) => `
    <tr>
      <td>${o.orderNo}</td>
      <td>${o.customerName}</td>
      <td>${formatDate(o.orderDate)}</td>
      <td>${formatDate(o.deliveryDate)}</td>
      <td>${formatCurrency(o.totalAmount)}</td>
      <td>${statusBadge(o.status, o.statusLabel)}</td>
      <td>
        <button class="btn btn-sm btn-primary" onclick="showOrderDetail(${o.id})">詳細</button>
      </td>
    </tr>`
    )
    .join("");
}

function showOrderDetail(orderId) {
  const order = DUMMY_ORDERS.find((o) => o.id === orderId);
  if (!order) return;

  const itemsHtml = order.items
    .map(
      (item) => `
    <tr>
      <td>${item.productName}</td>
      <td>${item.quantity}</td>
      <td>${formatCurrency(item.unitPrice)}</td>
      <td>${formatCurrency(item.subtotal)}</td>
    </tr>`
    )
    .join("");

  document.getElementById("modal-title").textContent = `受注詳細: ${order.orderNo}`;
  document.getElementById("modal-body").innerHTML = `
    <div class="detail-grid">
      <div><strong>受注番号</strong><p>${order.orderNo}</p></div>
      <div><strong>顧客名</strong><p>${order.customerName}</p></div>
      <div><strong>受注日</strong><p>${formatDate(order.orderDate)}</p></div>
      <div><strong>納品予定日</strong><p>${formatDate(order.deliveryDate)}</p></div>
      <div><strong>ステータス</strong><p>${statusBadge(order.status, order.statusLabel)}</p></div>
      <div><strong>合計金額</strong><p>${formatCurrency(order.totalAmount)}</p></div>
    </div>
    <h4>受注明細</h4>
    <table class="table">
      <thead><tr><th>商品名</th><th>数量</th><th>単価</th><th>小計</th></tr></thead>
      <tbody>${itemsHtml}</tbody>
    </table>`;
  document.getElementById("modal-overlay").style.display = "flex";
}

// APIからデータ取得（バックエンド連携時に使用）
async function fetchOrdersFromAPI() {
  try {
    showLoading("orders-body", 7);
    const data = await apiFetch("/api/orders");
    renderOrders(data);
  } catch (e) {
    showError("orders-body", 7, e.message);
    // フォールバック: ダミーデータで描画
    renderOrders(DUMMY_ORDERS);
  }
}

// ============================================================
// 請求書一覧
// ============================================================
function renderInvoices(invoices = DUMMY_INVOICES) {
  const tbody = document.getElementById("invoices-body");
  tbody.innerHTML = invoices
    .map(
      (inv) => `
    <tr>
      <td>${inv.invoiceNo}</td>
      <td>${inv.customerName}</td>
      <td>${inv.orderNo}</td>
      <td>${formatDate(inv.issueDate)}</td>
      <td>${formatDate(inv.dueDate)}</td>
      <td>${formatCurrency(inv.totalWithTax)}</td>
      <td>${statusBadge(inv.status, inv.statusLabel)}</td>
      <td>
        <button class="btn btn-sm btn-primary" onclick="showInvoiceDetail(${inv.id})">詳細</button>
      </td>
    </tr>`
    )
    .join("");
}

function showInvoiceDetail(invoiceId) {
  const inv = DUMMY_INVOICES.find((i) => i.id === invoiceId);
  if (!inv) return;

  document.getElementById("modal-title").textContent = `請求書詳細: ${inv.invoiceNo}`;
  document.getElementById("modal-body").innerHTML = `
    <div class="detail-grid">
      <div><strong>請求書番号</strong><p>${inv.invoiceNo}</p></div>
      <div><strong>顧客名</strong><p>${inv.customerName}</p></div>
      <div><strong>対応受注番号</strong><p>${inv.orderNo}</p></div>
      <div><strong>発行日</strong><p>${formatDate(inv.issueDate)}</p></div>
      <div><strong>支払期限</strong><p>${formatDate(inv.dueDate)}</p></div>
      <div><strong>ステータス</strong><p>${statusBadge(inv.status, inv.statusLabel)}</p></div>
    </div>
    <div class="amount-summary">
      <table class="table">
        <tr><td>小計（税抜）</td><td>${formatCurrency(inv.amount)}</td></tr>
        <tr><td>消費税（10%）</td><td>${formatCurrency(inv.tax)}</td></tr>
        <tr class="total-row"><td><strong>合計（税込）</strong></td><td><strong>${formatCurrency(inv.totalWithTax)}</strong></td></tr>
      </table>
    </div>`;
  document.getElementById("modal-overlay").style.display = "flex";
}

async function fetchInvoicesFromAPI() {
  try {
    showLoading("invoices-body", 8);
    const data = await apiFetch("/api/invoices");
    renderInvoices(data);
  } catch (e) {
    showError("invoices-body", 8, e.message);
    renderInvoices(DUMMY_INVOICES);
  }
}

// ============================================================
// 在庫管理
// ============================================================
function renderStock(stocks = DUMMY_STOCKS) {
  const tbody = document.getElementById("stock-body");
  tbody.innerHTML = stocks
    .map((s) => {
      const isLow = s.quantity <= s.threshold;
      return `
    <tr class="${isLow ? "row-warning" : ""}">
      <td>${s.productName}</td>
      <td class="${isLow ? "text-danger" : ""}">${s.quantity.toLocaleString()} ${s.unit}</td>
      <td>${s.threshold.toLocaleString()} ${s.unit}</td>
      <td>${isLow ? '<span class="badge badge-red">要補充</span>' : '<span class="badge badge-green">正常</span>'}</td>
    </tr>`;
    })
    .join("");
}

async function fetchStockFromAPI() {
  try {
    showLoading("stock-body", 4);
    const data = await apiFetch("/api/stocks");
    renderStock(data);
  } catch (e) {
    showError("stock-body", 4, e.message);
    renderStock(DUMMY_STOCKS);
  }
}

// ============================================================
// 商品マスタ
// ============================================================
function renderProducts(products = DUMMY_PRODUCTS) {
  const tbody = document.getElementById("products-body");
  tbody.innerHTML = products
    .map(
      (p) => `
    <tr>
      <td>${p.code}</td>
      <td>${p.name}</td>
      <td>${p.unit}</td>
      <td>${formatCurrency(p.price)}</td>
    </tr>`
    )
    .join("");
}

async function fetchProductsFromAPI() {
  try {
    showLoading("products-body", 4);
    const data = await apiFetch("/api/products");
    renderProducts(data);
  } catch (e) {
    showError("products-body", 4, e.message);
    renderProducts(DUMMY_PRODUCTS);
  }
}

// ============================================================
// 顧客マスタ
// ============================================================
function renderCustomers(customers = DUMMY_CUSTOMERS) {
  const tbody = document.getElementById("customers-body");
  tbody.innerHTML = customers
    .map(
      (c) => `
    <tr>
      <td>${c.code}</td>
      <td>${c.name}</td>
      <td>${c.contact}</td>
      <td>${c.tel}</td>
    </tr>`
    )
    .join("");
}

async function fetchCustomersFromAPI() {
  try {
    showLoading("customers-body", 4);
    const data = await apiFetch("/api/customers");
    renderCustomers(data);
  } catch (e) {
    showError("customers-body", 4, e.message);
    renderCustomers(DUMMY_CUSTOMERS);
  }
}

// ============================================================
// ユーティリティ
// ============================================================
function showLoading(tbodyId, cols) {
  document.getElementById(tbodyId).innerHTML = `
    <tr><td colspan="${cols}" class="text-center">読み込み中...</td></tr>`;
}

function showError(tbodyId, cols, message) {
  document.getElementById(tbodyId).innerHTML = `
    <tr><td colspan="${cols}" class="text-center text-danger">エラー: ${message}</td></tr>`;
}

function closeModal() {
  document.getElementById("modal-overlay").style.display = "none";
}

// ============================================================
// 検索フィルター
// ============================================================
function filterOrders() {
  const keyword = document.getElementById("order-search").value.toLowerCase();
  const statusFilter = document.getElementById("order-status-filter").value;
  const filtered = DUMMY_ORDERS.filter((o) => {
    const matchKeyword =
      o.orderNo.toLowerCase().includes(keyword) ||
      o.customerName.toLowerCase().includes(keyword);
    const matchStatus = !statusFilter || o.status === statusFilter;
    return matchKeyword && matchStatus;
  });
  renderOrders(filtered);
}

function filterInvoices() {
  const keyword = document.getElementById("invoice-search").value.toLowerCase();
  const statusFilter = document.getElementById("invoice-status-filter").value;
  const filtered = DUMMY_INVOICES.filter((inv) => {
    const matchKeyword =
      inv.invoiceNo.toLowerCase().includes(keyword) ||
      inv.customerName.toLowerCase().includes(keyword);
    const matchStatus = !statusFilter || inv.status === statusFilter;
    return matchKeyword && matchStatus;
  });
  renderInvoices(filtered);
}

// ============================================================
// 初期化
// ============================================================
document.addEventListener("DOMContentLoaded", () => {
  // ナビゲーションイベント
  document.querySelectorAll(".nav-link").forEach((el) => {
    el.addEventListener("click", (e) => {
      e.preventDefault();
      navigate(el.dataset.section);
    });
  });

  // モーダル閉じる
  document.getElementById("modal-overlay").addEventListener("click", (e) => {
    if (e.target === e.currentTarget) closeModal();
  });

  // 初期ページ
  navigate("dashboard");
});
