# 単体テスト仕様書

**システム名：** LumberCore 木材受発注基幹システム
**対象レイヤー：** Backend（Service層・Model層）／Frontend（JavaScript関数）
**テストフレームワーク：** JUnit 5 / Mockito / AssertJ（Backend）、手動確認（Frontend）
**作成日：** 2024-04

> **単体テストの方針**
> 各クラス・メソッドを依存関係から切り離し、ロジック単体で検証する。
> 外部依存（Repository・他Service）は Mockito でモック化する。
> 正常系・異常系・境界値の3観点を必ず網羅する。

---

## 1. OrderService

| No. | テスト対象（画面/項目） | テスト観点 | 確認内容 | テスト条件 | テスト手順 | 期待値 |
|-----|----------------------|------------|----------|------------|------------|--------|
| UT-OS-001 | OrderService / findAll() | 正常系：データ取得 | リポジトリの全件を返すこと | OrderRepositoryに2件のダミーデータを設定する | `findAll()` を呼び出す | 戻り値のリストサイズが2であること。`verify(repo, times(1)).findAll()` が通ること |
| UT-OS-002 | OrderService / findAll() | 境界値：0件 | データが存在しないとき空リストを返すこと | OrderRepositoryが空リストを返すよう設定する | `findAll()` を呼び出す | 戻り値が空リスト（`size=0`）であること。例外が発生しないこと |
| UT-OS-003 | OrderService / findById() | 正常系：ID存在 | 指定IDの受注オブジェクトを返すこと | ID=1L の受注が存在するよう Repository をスタブ設定する | `findById(1L)` を呼び出す | 返却オブジェクトのIDが1L、orderNoが期待値と一致すること |
| UT-OS-004 | OrderService / findById() | 異常系：ID不存在 | 存在しないIDで NoSuchElementException が発生すること | `repo.findById(99L)` が `Optional.empty()` を返すよう設定する | `findById(99L)` を呼び出す | `NoSuchElementException` がスローされ、メッセージに "99" が含まれること |
| UT-OS-005 | OrderService / findById() | 境界値：ID=1 | 最小IDで正常に取得できること | ID=1L の受注をスタブ設定する | `findById(1L)` を呼び出す | 例外が発生せず、受注オブジェクトが返ること |
| UT-OS-006 | OrderService / findByStatus() | 正常系：PENDING | PENDINGステータスの受注のみ返すこと | PENDING受注1件をスタブ設定する | `findByStatus(PENDING)` を呼び出す | サイズ1のリストが返り、要素のstatusがPENDINGであること |
| UT-OS-007 | OrderService / findByStatus() | 正常系：全ステータス | すべてのステータス値でリポジトリが呼ばれること | 各ステータスで空リストを返すようスタブ設定する | `@ParameterizedTest` で全ステータスをループ呼び出し | 各ステータスで `repo.findByStatus(status)` が1回ずつ呼ばれること |
| UT-OS-008 | OrderService / findByStatus() | 異常系：null | status=nullで IllegalArgumentException が発生すること | 引数として null を渡す | `findByStatus(null)` を呼び出す | `IllegalArgumentException` がスローされ、メッセージに "null" が含まれること |
| UT-OS-009 | OrderService / findByCustomerId() | 正常系：顧客ID存在 | 指定顧客IDの受注を返すこと | customerId=1L の受注をスタブ設定する | `findByCustomerId(1L)` を呼び出す | 戻り値のリストサイズが1であること |
| UT-OS-010 | OrderService / findByCustomerId() | 異常系：ID=0 | customerId=0で IllegalArgumentException が発生すること | 引数として 0L を渡す | `findByCustomerId(0L)` を呼び出す | `IllegalArgumentException` がスローされること |
| UT-OS-011 | OrderService / findByCustomerId() | 異常系：ID負数 | 負のcustomerIdで IllegalArgumentException が発生すること | 引数として -1L を渡す | `findByCustomerId(-1L)` を呼び出す | `IllegalArgumentException` がスローされること |
| UT-OS-012 | OrderService / findByCustomerId() | 異常系：null | customerId=nullで IllegalArgumentException が発生すること | 引数として null を渡す | `findByCustomerId(null)` を呼び出す | `IllegalArgumentException` がスローされること |
| UT-OS-013 | OrderService / createOrder() | 正常系：受注登録 | 受注が保存され、PENDINGステータスで返ること | 有効な受注オブジェクト（明細1件以上）を用意し、`repo.save()` がID付きで返すようスタブ設定する | `createOrder(order)` を呼び出す | 保存された受注のstatusがPENDING、IDが採番されていること。`repo.save()` が1回呼ばれること |
| UT-OS-014 | OrderService / createOrder() | 正常系：合計金額自動計算 | 明細の数量×単価の合計が totalAmount に反映されること | 数量5・単価1000の明細と数量3・単価500の明細を持つ受注を用意する | `createOrder(order)` を呼び出す | `totalAmount` が 6,500（5×1000 + 3×500）であること |
| UT-OS-015 | OrderService / createOrder() | 異常系：order=null | order=nullで IllegalArgumentException が発生すること | 引数として null を渡す | `createOrder(null)` を呼び出す | `IllegalArgumentException` がスローされること |
| UT-OS-016 | OrderService / createOrder() | 異常系：明細0件 | 受注明細が空リストのとき IllegalArgumentException が発生すること | itemsに空リストを設定した受注を用意する | `createOrder(order)` を呼び出す | `IllegalArgumentException` がスローされ、メッセージに "1件以上" が含まれること |
| UT-OS-017 | OrderService / createOrder() | 異常系：明細null | 受注明細がnullのとき IllegalArgumentException が発生すること | itemsにnullを設定した受注を用意する | `createOrder(order)` を呼び出す | `IllegalArgumentException` がスローされること |
| UT-OS-018 | OrderService / updateStatus() | 正常系：PENDING→CONFIRMED | ステータスをCONFIRMEDに更新できること | status=PENDINGの受注を取得するようスタブ設定する | `updateStatus(1L, CONFIRMED)` を呼び出す | 返却オブジェクトのstatusがCONFIRMEDであること |
| UT-OS-019 | OrderService / updateStatus() | 正常系：CONFIRMED→DELIVERED | ステータスをDELIVEREDに更新できること | status=CONFIRMEDの受注をスタブ設定する | `updateStatus(1L, DELIVERED)` を呼び出す | 返却オブジェクトのstatusがDELIVEREDであること |
| UT-OS-020 | OrderService / updateStatus() | 異常系：CANCELLED済み変更不可 | CANCELLEDステータスの受注は変更できないこと | status=CANCELLEDの受注をスタブ設定する | `updateStatus(1L, CONFIRMED)` を呼び出す | `IllegalStateException` がスローされ、メッセージに "キャンセル済み" が含まれること |
| UT-OS-021 | OrderService / updateStatus() | 異常系：DELIVERED→CANCELLED不可 | 納品済み受注はキャンセルできないこと | status=DELIVEREDの受注をスタブ設定する | `updateStatus(1L, CANCELLED)` を呼び出す | `IllegalStateException` がスローされ、メッセージに "納品済み" が含まれること |
| UT-OS-022 | OrderService / updateStatus() | 異常系：ID不存在 | 存在しないIDで NoSuchElementException が発生すること | `repo.findById(999L)` が `Optional.empty()` を返すようスタブ設定する | `updateStatus(999L, CONFIRMED)` を呼び出す | `NoSuchElementException` がスローされること |
| UT-OS-023 | OrderService / deleteOrder() | 正常系：PENDING削除 | PENDINGステータスの受注を削除できること | status=PENDINGの受注をスタブ設定する | `deleteOrder(1L)` を呼び出す | 例外が発生せず、`repo.deleteById(1L)` が1回呼ばれること |
| UT-OS-024 | OrderService / deleteOrder() | 異常系：CONFIRMED削除不可 | CONFIRMED受注は削除できないこと | status=CONFIRMEDの受注をスタブ設定する | `deleteOrder(1L)` を呼び出す | `IllegalStateException` がスローされ、`repo.deleteById()` が呼ばれないこと |
| UT-OS-025 | OrderService / deleteOrder() | 異常系：DELIVERED削除不可 | DELIVERED受注は削除できないこと | status=DELIVEREDの受注をスタブ設定する | `deleteOrder(1L)` を呼び出す | `IllegalStateException` がスローされること |

---

## 2. InvoiceService

| No. | テスト対象（画面/項目） | テスト観点 | 確認内容 | テスト条件 | テスト手順 | 期待値 |
|-----|----------------------|------------|----------|------------|------------|--------|
| UT-IS-001 | InvoiceService / findAll() | 正常系：データ取得 | 全請求書リストを返すこと | 2件の請求書をスタブ設定する | `findAll()` を呼び出す | サイズ2のリストが返ること |
| UT-IS-002 | InvoiceService / findById() | 正常系：ID存在 | 指定IDの請求書を返すこと | ID=1L の請求書をスタブ設定する | `findById(1L)` を呼び出す | invoiceNoが期待値と一致すること |
| UT-IS-003 | InvoiceService / findById() | 異常系：ID不存在 | 存在しないIDで NoSuchElementException が発生すること | `repo.findById(99L)` が `Optional.empty()` を返すようスタブ設定する | `findById(99L)` を呼び出す | `NoSuchElementException` がスローされ、メッセージに "99" が含まれること |
| UT-IS-004 | InvoiceService / findByStatus() | 正常系：UNPAIDフィルタ | UNPAID請求書のみ返すこと | UNPAID請求書1件をスタブ設定する | `findByStatus(UNPAID)` を呼び出す | サイズ1のリストが返り、statusがUNPAIDであること |
| UT-IS-005 | InvoiceService / findByStatus() | 異常系：null | status=nullで IllegalArgumentException が発生すること | 引数として null を渡す | `findByStatus(null)` を呼び出す | `IllegalArgumentException` がスローされること |
| UT-IS-006 | InvoiceService / issueInvoiceFromOrder() | 正常系：請求書発行 | DELIVERED受注から請求書が発行できること | DELIVERED受注（totalAmount=10000）をスタブ設定する | `issueInvoiceFromOrder(1L, 発行日, 支払期限)` を呼び出す | amount=10000、tax=1000、totalWithTax=11000、status=UNPAIDの請求書が返ること |
| UT-IS-007 | InvoiceService / issueInvoiceFromOrder() | 正常系：請求書番号自動採番 | 請求書番号が自動生成されること | 既存1件の状態でスタブ設定する | `issueInvoiceFromOrder()` を呼び出す | invoiceNoが "INV-" で始まり、空文字でないこと |
| UT-IS-008 | InvoiceService / issueInvoiceFromOrder() | 異常系：非DELIVERED受注 | DELIVERED以外の受注は請求書発行不可 | status=CONFIRMEDの受注をスタブ設定する | `issueInvoiceFromOrder(1L, ...)` を呼び出す | `IllegalStateException` がスローされ、メッセージに "納品済み" が含まれること |
| UT-IS-009 | InvoiceService / issueInvoiceFromOrder() | 異常系：支払期限が発行日より前 | dueDate < issueDate のとき IllegalArgumentException が発生すること | issueDate=2024-05-31、dueDate=2024-05-01 で呼び出す | `issueInvoiceFromOrder(1L, 2024-05-31, 2024-05-01)` を呼び出す | `IllegalArgumentException` がスローされ、メッセージに "支払期限" が含まれること |
| UT-IS-010 | InvoiceService / issueInvoiceFromOrder() | 境界値：発行日＝支払期限 | 発行日と支払期限が同日のとき正常発行できること | issueDate＝dueDate＝2024-05-01 で設定する | `issueInvoiceFromOrder(1L, 2024-05-01, 2024-05-01)` を呼び出す | 例外が発生せず、請求書が返ること |
| UT-IS-011 | InvoiceService / markAsPaid() | 正常系：支払済み更新 | UNPAID→PAIDに更新できること | status=UNPAIDの請求書をスタブ設定する | `markAsPaid(1L)` を呼び出す | 返却オブジェクトのstatusがPAIDであること |
| UT-IS-012 | InvoiceService / markAsPaid() | 冪等性：PAID済み再実行 | 既にPAIDの請求書を再度実行しても正常終了すること | status=PAIDの請求書をスタブ設定する | `markAsPaid(1L)` を呼び出す | statusがPAIDのまま返り、`repo.save()` が呼ばれないこと |
| UT-IS-013 | InvoiceService / updateOverdueInvoices() | 正常系：期限切れあり | 期限切れUNPAID請求書がOVERDUEになること | dueDate=2024-01-01のUNPAID請求書と、未来日のUNPAID請求書を1件ずつスタブ設定する | `updateOverdueInvoices(2024-06-01)` を呼び出す | 戻り値が1（更新件数）。期限切れ請求書のstatusがOVERDUEになること |
| UT-IS-014 | InvoiceService / updateOverdueInvoices() | 境界値：当日は期限切れにならない | dueDate＝当日はOVERDUEにならないこと | dueDate=2024-05-01のUNPAID請求書を用意し、today=2024-05-01 で呼び出す | `updateOverdueInvoices(2024-05-01)` を呼び出す | 戻り値が0。statusがUNPAIDのまま変化しないこと |
| UT-IS-015 | InvoiceService / updateOverdueInvoices() | 境界値：対象0件 | UNPAID請求書が0件のとき0を返すこと | UNPAIDが空リストで返るようスタブ設定する | `updateOverdueInvoices(任意の日付)` を呼び出す | 戻り値が0であること |

---

## 3. StockService

| No. | テスト対象（画面/項目） | テスト観点 | 確認内容 | テスト条件 | テスト手順 | 期待値 |
|-----|----------------------|------------|----------|------------|------------|--------|
| UT-SS-001 | StockService / findAll() | 正常系：全件取得 | 全在庫リストを返すこと | 2件の在庫をスタブ設定する | `findAll()` を呼び出す | サイズ2のリストが返ること |
| UT-SS-002 | StockService / findLowStocks() | 正常系：要補充品目取得 | 補充基準以下の在庫のみ返すこと | 要補充1件をスタブ設定する | `findLowStocks()` を呼び出す | サイズ1のリストが返ること |
| UT-SS-003 | StockService / updateStock() | 正常系：入庫（delta正） | 在庫が増加すること | quantity=100の在庫をスタブ設定する | `updateStock(1L, +50)` を呼び出す | quantity=150が返ること |
| UT-SS-004 | StockService / updateStock() | 正常系：出庫（delta負） | 在庫が減少すること | quantity=100の在庫をスタブ設定する | `updateStock(1L, -30)` を呼び出す | quantity=70が返ること |
| UT-SS-005 | StockService / updateStock() | 境界値：delta=0 | 在庫数が変化しないこと | quantity=100の在庫をスタブ設定する | `updateStock(1L, 0)` を呼び出す | quantity=100のまま返ること |
| UT-SS-006 | StockService / updateStock() | 境界値：全数出庫（在庫=0） | 在庫をすべて出庫すると0になること | quantity=100の在庫をスタブ設定する | `updateStock(1L, -100)` を呼び出す | quantity=0が返ること。例外が発生しないこと |
| UT-SS-007 | StockService / updateStock() | 異常系：在庫不足 | 出庫数が現在庫を超えると IllegalStateException が発生すること | quantity=10の在庫をスタブ設定する | `updateStock(1L, -11)` を呼び出す | `IllegalStateException` がスローされ、メッセージに "在庫数が不足" が含まれること |
| UT-SS-008 | StockService / updateStock() | 異常系：ID不存在 | 存在しない商品IDで NoSuchElementException が発生すること | `repo.findByProductId(99L)` が `Optional.empty()` を返すようスタブ設定する | `updateStock(99L, 10)` を呼び出す | `NoSuchElementException` がスローされ、メッセージに "99" が含まれること |

---

## 4. Order モデル（ビジネスロジック）

| No. | テスト対象（画面/項目） | テスト観点 | 確認内容 | テスト条件 | テスト手順 | 期待値 |
|-----|----------------------|------------|----------|------------|------------|--------|
| UT-OM-001 | Order / recalculateTotal() | 正常系：複数明細 | 数量×単価の合計が totalAmount に設定されること | 明細3件（10×398, 5×1000, 2×200）を持つOrderを用意する | `order.recalculateTotal()` を呼び出す | totalAmount = 9,380（3,980+5,000+400）であること |
| UT-OM-002 | Order / recalculateTotal() | 正常系：小計更新 | 各 OrderItem の subtotal も更新されること | 数量5・単価400の明細1件を持つOrderを用意する | `order.recalculateTotal()` を呼び出す | 明細の subtotal が 2,000 になること |
| UT-OM-003 | Order / recalculateTotal() | 境界値：明細0件 | 明細が空リストのとき totalAmount=0 になること | `items=emptyList()` のOrderを用意する | `order.recalculateTotal()` を呼び出す | totalAmount=0。例外が発生しないこと |
| UT-OM-004 | Order / recalculateTotal() | 境界値：明細null | 明細がnullのとき totalAmount=0 になること | `items=null` のOrderを用意する | `order.recalculateTotal()` を呼び出す | totalAmount=0。`NullPointerException` が発生しないこと |
| UT-OM-005 | Order / recalculateTotal() | 境界値：数量1・単価1 | 最小値で正しく計算されること | 数量1・単価1の明細1件を用意する | `order.recalculateTotal()` を呼び出す | totalAmount=1 であること |

---

## 5. Invoice モデル（ビジネスロジック）

| No. | テスト対象（画面/項目） | テスト観点 | 確認内容 | テスト条件 | テスト手順 | 期待値 |
|-----|----------------------|------------|----------|------------|------------|--------|
| UT-IM-001 | Invoice / calculateTax() | 正常系：10,000円 | 消費税と税込金額が正確に計算されること | amount=10,000 の Invoice を用意する | `invoice.calculateTax()` を呼び出す | tax=1,000、totalWithTax=11,000 であること |
| UT-IM-002 | Invoice / calculateTax() | 正常系：100,000円 | 大きな金額でも正確に計算されること | amount=100,000 の Invoice を用意する | `invoice.calculateTax()` を呼び出す | tax=10,000、totalWithTax=110,000 であること |
| UT-IM-003 | Invoice / calculateTax() | 境界値：0円 | 0円の場合に税額0になること | amount=0 の Invoice を用意する | `invoice.calculateTax()` を呼び出す | tax=0、totalWithTax=0 であること |
| UT-IM-004 | Invoice / calculateTax() | 境界値：1円 | 1円（端数発生）のとき切り捨てされること | amount=1 の Invoice を用意する（1×0.1=0.1→切り捨て） | `invoice.calculateTax()` を呼び出す | tax=0、totalWithTax=1 であること |
| UT-IM-005 | Invoice / calculateTax() | 境界値：端数切り捨て確認 | 端数が切り捨てされること | amount=15 の Invoice を用意する（15×0.1=1.5→切り捨て1） | `invoice.calculateTax()` を呼び出す | tax=1、totalWithTax=16 であること |
| UT-IM-006 | Invoice / calculateTax() | 境界値：99,999円 | 大きな奇数金額で切り捨てされること | amount=99,999 の Invoice を用意する（99,999×0.1=9,999.9→切り捨て9,999） | `invoice.calculateTax()` を呼び出す | tax=9,999、totalWithTax=109,998 であること |

---

## 6. Stock モデル（ビジネスロジック）

| No. | テスト対象（画面/項目） | テスト観点 | 確認内容 | テスト条件 | テスト手順 | 期待値 |
|-----|----------------------|------------|----------|------------|------------|--------|
| UT-SM-001 | Stock / isLow() | 正常系：在庫＞閾値 | 在庫が閾値を超えているとき false を返すこと | quantity=51、threshold=50 のStockを用意する | `stock.isLow()` を呼び出す | false が返ること |
| UT-SM-002 | Stock / isLow() | 境界値：在庫＝閾値 | 在庫が閾値と等しいとき true を返すこと | quantity=50、threshold=50 のStockを用意する | `stock.isLow()` を呼び出す | true が返ること（等値は補充が必要） |
| UT-SM-003 | Stock / isLow() | 境界値：在庫＜閾値 | 在庫が閾値を下回るとき true を返すこと | quantity=49、threshold=50 のStockを用意する | `stock.isLow()` を呼び出す | true が返ること |

---

## 7. OrderRepository（インメモリ実装）

| No. | テスト対象（画面/項目） | テスト観点 | 確認内容 | テスト条件 | テスト手順 | 期待値 |
|-----|----------------------|------------|----------|------------|------------|--------|
| UT-OR-001 | OrderRepository / findAll() | 正常系：初期データ | 初期化直後に3件のデータが存在すること | `new OrderRepository()` でインスタンス生成する | `findAll()` を呼び出す | サイズ3のリストが返ること |
| UT-OR-002 | OrderRepository / findById() | 正常系：存在するID | Optional に値が入ること | 初期データのID=1L を使用する | `findById(1L)` を呼び出す | `Optional.isPresent()` が true。orderNoが "ORD-2024-001" であること |
| UT-OR-003 | OrderRepository / findById() | 異常系：存在しないID | Optional.empty() が返ること | 初期データにないID=999L を使用する | `findById(999L)` を呼び出す | `Optional.isEmpty()` が true であること |
| UT-OR-004 | OrderRepository / save() | 正常系：新規登録 | IDが自動採番されること | IDなしのOrderオブジェクトを用意する | `save(order)` を呼び出す | 返却オブジェクトのIDが null でなく正数であること |
| UT-OR-005 | OrderRepository / save() | 正常系：上書き | 既存IDで保存すると内容が更新されること | ID=1L のOrderのstatusをCONFIRMEDに変更して用意する | `save(order)` を呼び出す → `findById(1L)` で確認 | `findById(1L)` のstatusがCONFIRMEDであること |
| UT-OR-006 | OrderRepository / deleteById() | 正常系：削除成功 | 削除するとデータが消えること | ID=1L を対象にする | `deleteById(1L)` を呼び出す → `findById(1L)` で確認 | `deleteById()` が true を返し、`findById(1L)` が `Optional.empty()` になること |
| UT-OR-007 | OrderRepository / deleteById() | 異常系：存在しないID | 存在しないIDを削除すると false を返すこと | 初期データにないID=999L を使用する | `deleteById(999L)` を呼び出す | false が返ること |

---

## 8. Frontend JavaScript関数

| No. | テスト対象（画面/項目） | テスト観点 | 確認内容 | テスト条件 | テスト手順 | 期待値 |
|-----|----------------------|------------|----------|------------|------------|--------|
| UT-FE-001 | app.js / formatCurrency() | 正常系：整数 | 円単位の通貨形式で表示されること | amount = 103800 | ブラウザコンソールで `formatCurrency(103800)` を実行する | `"¥103,800"` と表示されること |
| UT-FE-002 | app.js / formatCurrency() | 境界値：0円 | 0円が正しくフォーマットされること | amount = 0 | `formatCurrency(0)` を実行する | `"¥0"` と表示されること |
| UT-FE-003 | app.js / formatDate() | 正常系：有効日付 | YYYY/MM/DD形式で表示されること | dateStr = "2024-04-01" | `formatDate("2024-04-01")` を実行する | `"2024/04/01"` と表示されること |
| UT-FE-004 | app.js / formatDate() | 境界値：null | nullのとき "—" を返すこと | dateStr = null | `formatDate(null)` を実行する | `"—"` と表示されること。例外が発生しないこと |
| UT-FE-005 | app.js / formatDate() | 境界値：空文字 | 空文字のとき "—" を返すこと | dateStr = "" | `formatDate("")` を実行する | `"—"` と表示されること |
| UT-FE-006 | app.js / statusBadge() | 正常系：各ステータス | 各ステータスに対応するCSSクラスのspanが生成されること | status="DELIVERED", label="納品済" | `statusBadge("DELIVERED","納品済")` を実行する | `class="status status-delivered"` を持つ `<span>` が返ること |
| UT-FE-007 | app.js / statusBadge() | 異常系：未定義ステータス | 未定義ステータスでもHTMLが返ること（クラスなし） | status="UNKNOWN", label="不明" | `statusBadge("UNKNOWN","不明")` を実行する | `<span class="status ">不明</span>` が返り、例外が発生しないこと |
| UT-FE-008 | app.js / stockBar() | 正常系：在庫十分（緑） | 在庫が閾値の2倍以上のとき緑バーが返ること | quantity=200、threshold=100（100%以上） | `stockBar(200, 100)` を実行する | クラスなし（緑）の fill 要素が含まれること |
| UT-FE-009 | app.js / stockBar() | 正常系：要補充（赤） | 在庫が閾値以下のとき赤バーが返ること | quantity=80、threshold=100（閾値以下） | `stockBar(80, 100)` を実行する | `stock-bar__fill--low` クラスが含まれること |
| UT-FE-010 | app.js / stockBar() | 境界値：threshold=0 | threshold=0のとき空文字を返すこと | quantity=50、threshold=0 | `stockBar(50, 0)` を実行する | 空文字 `""` が返ること。ゼロ除算エラーが発生しないこと |
| UT-FE-011 | app.js / filterOrders() | 正常系：受注番号検索 | 受注番号を含むデータのみ表示されること | 検索フィールドに "ORD-2024-001" を入力する | `order-search` に文字入力後 `filterOrders()` を呼び出す | テーブルに ORD-2024-001 のみ表示されること |
| UT-FE-012 | app.js / filterOrders() | 正常系：ステータスフィルタ | 選択ステータスに一致するデータのみ表示されること | ステータスフィルタで "PENDING" を選択する | `order-status-filter` を変更後 `filterOrders()` を呼び出す | PENDING以外の受注が表示されないこと |
| UT-FE-013 | app.js / filterOrders() | 正常系：組み合わせフィルタ | キーワードとステータスを同時に適用できること | 検索欄に顧客名の一部、ステータスも選択する | 両条件を設定後 `filterOrders()` を呼び出す | 両条件に合致するデータのみ表示されること |
| UT-FE-014 | app.js / filterOrders() | 境界値：0件ヒット | 一致するデータがないとき空状態メッセージを表示すること | 存在しないキーワード（例："XXXXXXXXX"）を入力する | `filterOrders()` を呼び出す | テーブルに "条件に一致する受注がありません" が表示されること |
