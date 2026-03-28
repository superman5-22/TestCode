/**
 * ダミーデータ定義
 * DB接続なしで初期表示用データを提供する
 */

const DUMMY_PRODUCTS = [
  { id: 1, code: "SPF-2x4", name: "SPF 2×4材 (6F)", unit: "本", price: 398 },
  { id: 2, code: "SPF-2x6", name: "SPF 2×6材 (6F)", unit: "本", price: 598 },
  { id: 3, code: "CEDAR-1x4", name: "杉 1×4材 (6F)", unit: "本", price: 450 },
  { id: 4, code: "PINE-BOARD", name: "パイン集成材 18×200×1820", unit: "枚", price: 1980 },
  { id: 5, code: "PLYWOOD-12", name: "構造用合板 12mm 910×1820", unit: "枚", price: 1280 },
];

const DUMMY_CUSTOMERS = [
  { id: 1, code: "C001", name: "山田建設株式会社", contact: "山田 太郎", tel: "03-1234-5678" },
  { id: 2, code: "C002", name: "鈴木工務店", contact: "鈴木 次郎", tel: "06-9876-5432" },
  { id: 3, code: "C003", name: "田中リフォーム", contact: "田中 三郎", tel: "052-111-2222" },
];

const DUMMY_ORDERS = [
  {
    id: 1,
    orderNo: "ORD-2024-001",
    orderDate: "2024-04-01",
    deliveryDate: "2024-04-10",
    customerId: 1,
    customerName: "山田建設株式会社",
    status: "DELIVERED",
    statusLabel: "納品済",
    items: [
      { productId: 1, productName: "SPF 2×4材 (6F)", quantity: 100, unitPrice: 398, subtotal: 39800 },
      { productId: 5, productName: "構造用合板 12mm", quantity: 50, unitPrice: 1280, subtotal: 64000 },
    ],
    totalAmount: 103800,
  },
  {
    id: 2,
    orderNo: "ORD-2024-002",
    orderDate: "2024-04-05",
    deliveryDate: "2024-04-15",
    customerId: 2,
    customerName: "鈴木工務店",
    status: "CONFIRMED",
    statusLabel: "受注確定",
    items: [
      { productId: 3, productName: "杉 1×4材 (6F)", quantity: 200, unitPrice: 450, subtotal: 90000 },
    ],
    totalAmount: 90000,
  },
  {
    id: 3,
    orderNo: "ORD-2024-003",
    orderDate: "2024-04-08",
    deliveryDate: "2024-04-20",
    customerId: 3,
    customerName: "田中リフォーム",
    status: "PENDING",
    statusLabel: "確認待ち",
    items: [
      { productId: 4, productName: "パイン集成材", quantity: 10, unitPrice: 1980, subtotal: 19800 },
      { productId: 2, productName: "SPF 2×6材 (6F)", quantity: 30, unitPrice: 598, subtotal: 17940 },
    ],
    totalAmount: 37740,
  },
];

const DUMMY_INVOICES = [
  {
    id: 1,
    invoiceNo: "INV-2024-001",
    issueDate: "2024-04-30",
    dueDate: "2024-05-31",
    customerId: 1,
    customerName: "山田建設株式会社",
    orderId: 1,
    orderNo: "ORD-2024-001",
    amount: 103800,
    tax: 10380,
    totalWithTax: 114180,
    status: "PAID",
    statusLabel: "支払済",
  },
  {
    id: 2,
    invoiceNo: "INV-2024-002",
    issueDate: "2024-04-30",
    dueDate: "2024-05-31",
    customerId: 2,
    customerName: "鈴木工務店",
    orderId: 2,
    orderNo: "ORD-2024-002",
    amount: 90000,
    tax: 9000,
    totalWithTax: 99000,
    status: "UNPAID",
    statusLabel: "未払い",
  },
];

const DUMMY_STOCKS = [
  { productId: 1, productName: "SPF 2×4材 (6F)", quantity: 350, unit: "本", threshold: 100 },
  { productId: 2, productName: "SPF 2×6材 (6F)", quantity: 85, unit: "本", threshold: 100 },
  { productId: 3, productName: "杉 1×4材 (6F)", quantity: 420, unit: "本", threshold: 200 },
  { productId: 4, productName: "パイン集成材 18×200×1820", quantity: 25, unit: "枚", threshold: 30 },
  { productId: 5, productName: "構造用合板 12mm 910×1820", quantity: 180, unit: "枚", threshold: 50 },
];
