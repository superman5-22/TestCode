# 結合テスト仕様書

**システム名：** LumberCore 木材受発注基幹システム
**対象レイヤー：** Backend（Controller層 + Service層）／Frontend（API連携 + 画面反映）
**テストフレームワーク：** Spring Boot Test / MockMvc / @WebMvcTest（Backend）、ブラウザ手動確認（Frontend）
**作成日：** 2024-04

> **結合テストの方針**
> Controller と Service の連携を検証する。Repository は @MockBean でモック化し、
> HTTP リクエスト〜レスポンスまでを MockMvc で一貫して確認する。
> Frontend については、実際のバックエンドを起動した状態で API 通信と画面反映を確認する。

---

## 1. OrderController × OrderService

| No. | テスト対象（画面/項目） | テスト観点 | 確認内容 | テスト条件 | テスト手順 | 期待値 |
|-----|----------------------|------------|----------|------------|------------|--------|
| IT-OC-001 | GET /api/orders | 正常系：全件取得 | 200 OKと受注リストが返ること | OrderServiceが2件のリストを返すようモック設定する | MockMvcで `GET /api/orders` を実行する | HTTPステータス200。レスポンスJSONの配列サイズが2であること |
| IT-OC-002 | GET /api/orders?status=PENDING | 正常系：ステータスフィルタ | クエリパラメータでステータス絞り込みが効くこと | `findByStatus(PENDING)` が1件返すようモック設定する | MockMvcで `GET /api/orders?status=PENDING` を実行する | HTTPステータス200。`findAll()` が呼ばれず `findByStatus(PENDING)` が呼ばれること |
| IT-OC-003 | GET /api/orders | 正常系：0件 | データが0件のとき空配列が返ること | `findAll()` が空リストを返すようモック設定する | MockMvcで `GET /api/orders` を実行する | HTTPステータス200。レスポンスが `[]` であること |
| IT-OC-004 | GET /api/orders/{id} | 正常系：ID存在 | 200 OKと受注オブジェクトが返ること | ID=1L の受注をモック設定する | MockMvcで `GET /api/orders/1` を実行する | HTTPステータス200。`$.id` が 1、`$.customerName` が期待値と一致すること |
| IT-OC-005 | GET /api/orders/{id} | 異常系：ID不存在 | 404 Not Foundと errorメッセージが返ること | `findById(99L)` が `NoSuchElementException` をスローするようモック設定する | MockMvcで `GET /api/orders/99` を実行する | HTTPステータス404。`$.error` に "99" が含まれること |
| IT-OC-006 | POST /api/orders | 正常系：新規登録 | 201 Createdと作成された受注が返ること | `createOrder()` がID=10L・status=PENDINGの受注を返すようモック設定する | MockMvcで `POST /api/orders` にJSON bodyを送信する | HTTPステータス201。`$.id` が 10、`$.status` が "PENDING" であること |
| IT-OC-007 | POST /api/orders | 異常系：明細なし | 400 Bad Requestが返ること | `createOrder()` が `IllegalArgumentException("受注明細は1件以上必要です")` をスローするようモック設定する | 明細なしのbodyで `POST /api/orders` を実行する | HTTPステータス400。`$.error` に "1件以上" が含まれること |
| IT-OC-008 | PATCH /api/orders/{id}/status | 正常系：ステータス更新 | 200 OKと更新後の受注が返ること | `updateStatus(1L, CONFIRMED)` がCONFIRMED受注を返すようモック設定する | MockMvcで `PATCH /api/orders/1/status` に `{"status":"CONFIRMED"}` を送信する | HTTPステータス200。`$.status` が "CONFIRMED" であること |
| IT-OC-009 | PATCH /api/orders/{id}/status | 異常系：不正なステータス遷移 | 409 Conflictが返ること | `updateStatus()` が `IllegalStateException("キャンセル済み...")` をスローするようモック設定する | `PATCH /api/orders/1/status` に `{"status":"CONFIRMED"}` を送信する | HTTPステータス409。`$.error` に "キャンセル済み" が含まれること |
| IT-OC-010 | PATCH /api/orders/{id}/status | 異常系：ID不存在 | 404 Not Foundが返ること | `updateStatus()` が `NoSuchElementException` をスローするようモック設定する | `PATCH /api/orders/99/status` を実行する | HTTPステータス404であること |
| IT-OC-011 | DELETE /api/orders/{id} | 正常系：削除成功 | 204 No Contentが返ること | `deleteOrder(1L)` が正常終了するようモック設定する | MockMvcで `DELETE /api/orders/1` を実行する | HTTPステータス204。レスポンスボディが空であること |
| IT-OC-012 | DELETE /api/orders/{id} | 異常系：PENDING以外は削除不可 | 409 Conflictが返ること | `deleteOrder(1L)` が `IllegalStateException("確認待ち状態...")` をスローするようモック設定する | `DELETE /api/orders/1` を実行する | HTTPステータス409であること |
| IT-OC-013 | DELETE /api/orders/{id} | 異常系：ID不存在 | 404 Not Foundが返ること | `deleteOrder(99L)` が `NoSuchElementException` をスローするようモック設定する | `DELETE /api/orders/99` を実行する | HTTPステータス404であること |

---

## 2. InvoiceController × InvoiceService

| No. | テスト対象（画面/項目） | テスト観点 | 確認内容 | テスト条件 | テスト手順 | 期待値 |
|-----|----------------------|------------|----------|------------|------------|--------|
| IT-IC-001 | GET /api/invoices | 正常系：全件取得 | 200 OKと請求書リストが返ること | 2件の請求書をモック設定する | MockMvcで `GET /api/invoices` を実行する | HTTPステータス200。配列サイズが2であること |
| IT-IC-002 | GET /api/invoices?status=UNPAID | 正常系：ステータスフィルタ | UNPAIDのみ絞り込まれること | `findByStatus(UNPAID)` が1件返すようモック設定する | `GET /api/invoices?status=UNPAID` を実行する | HTTPステータス200。`$[0].status` が "UNPAID" であること |
| IT-IC-003 | GET /api/invoices/{id} | 正常系：ID存在 | 200 OKと請求書オブジェクトが返ること | ID=1L の請求書をモック設定する | `GET /api/invoices/1` を実行する | HTTPステータス200。`$.invoiceNo` が期待値と一致すること。`$.totalWithTax` が正しい値であること |
| IT-IC-004 | GET /api/invoices/{id} | 異常系：ID不存在 | 404 Not Foundが返ること | `findById(99L)` が `NoSuchElementException` をスローするようモック設定する | `GET /api/invoices/99` を実行する | HTTPステータス404であること |
| IT-IC-005 | POST /api/invoices/issue | 正常系：請求書発行 | 201 Createdと発行された請求書が返ること | `issueInvoiceFromOrder()` がUNPAID請求書を返すようモック設定する | `POST /api/invoices/issue?orderId=1&issueDate=2024-05-01&dueDate=2024-05-31` を実行する | HTTPステータス201。`$.status` が "UNPAID"、`$.totalWithTax` が正しい値であること |
| IT-IC-006 | POST /api/invoices/issue | 異常系：未納品受注への発行 | 409 Conflictが返ること | `issueInvoiceFromOrder()` が `IllegalStateException("納品済み...")` をスローするようモック設定する | `POST /api/invoices/issue?orderId=2&issueDate=...&dueDate=...` を実行する | HTTPステータス409。`$.error` に "納品済み" が含まれること |
| IT-IC-007 | POST /api/invoices/issue | 異常系：支払期限が発行日より前 | 400 Bad Requestが返ること | `issueInvoiceFromOrder()` が `IllegalArgumentException("支払期限...")` をスローするようモック設定する | `POST /api/invoices/issue?orderId=1&issueDate=2024-05-31&dueDate=2024-05-01` を実行する | HTTPステータス400。`$.error` に "支払期限" が含まれること |
| IT-IC-008 | PATCH /api/invoices/{id}/pay | 正常系：支払済み更新 | 200 OKとPAID請求書が返ること | `markAsPaid(1L)` がPAID請求書を返すようモック設定する | `PATCH /api/invoices/1/pay` を実行する | HTTPステータス200。`$.status` が "PAID" であること |
| IT-IC-009 | PATCH /api/invoices/{id}/pay | 異常系：ID不存在 | 404 Not Foundが返ること | `markAsPaid(99L)` が `NoSuchElementException` をスローするようモック設定する | `PATCH /api/invoices/99/pay` を実行する | HTTPステータス404であること |
| IT-IC-010 | POST /api/invoices/check-overdue | 正常系：期限切れ更新 | 200 OKと更新件数が返ること | `updateOverdueInvoices()` が 3 を返すようモック設定する | `POST /api/invoices/check-overdue` を実行する | HTTPステータス200。`$.updatedCount` が 3 であること |
| IT-IC-011 | POST /api/invoices/check-overdue | 正常系：更新対象なし | 200 OKで updatedCount=0 が返ること | `updateOverdueInvoices()` が 0 を返すようモック設定する | `POST /api/invoices/check-overdue` を実行する | HTTPステータス200。`$.updatedCount` が 0 であること |

---

## 3. StockController × StockService

| No. | テスト対象（画面/項目） | テスト観点 | 確認内容 | テスト条件 | テスト手順 | 期待値 |
|-----|----------------------|------------|----------|------------|------------|--------|
| IT-SC-001 | GET /api/stocks | 正常系：全件取得 | 200 OKと在庫リストが返ること | 5件の在庫をモック設定する | `GET /api/stocks` を実行する | HTTPステータス200。配列サイズが5であること |
| IT-SC-002 | GET /api/stocks/low | 正常系：要補充のみ取得 | 補充基準以下の在庫のみ返ること | `findLowStocks()` が2件返すようモック設定する | `GET /api/stocks/low` を実行する | HTTPステータス200。配列サイズが2であること |
| IT-SC-003 | PATCH /api/stocks/{productId}/adjust | 正常系：入庫 | 200 OKと更新後在庫が返ること | `updateStock(1L, 50)` が quantity=150 の在庫を返すようモック設定する | `PATCH /api/stocks/1/adjust` に `{"delta":50}` を送信する | HTTPステータス200。`$.quantity` が 150 であること |
| IT-SC-004 | PATCH /api/stocks/{productId}/adjust | 正常系：出庫 | 200 OKと在庫が減少した結果が返ること | `updateStock(1L, -30)` が quantity=70 の在庫を返すようモック設定する | `PATCH /api/stocks/1/adjust` に `{"delta":-30}` を送信する | HTTPステータス200。`$.quantity` が 70 であること |
| IT-SC-005 | PATCH /api/stocks/{productId}/adjust | 異常系：在庫不足 | 409 Conflictが返ること | `updateStock()` が `IllegalStateException("在庫数が不足...")` をスローするようモック設定する | `PATCH /api/stocks/1/adjust` に `{"delta":-999}` を送信する | HTTPステータス409。`$.error` に "在庫数が不足" が含まれること |
| IT-SC-006 | PATCH /api/stocks/{productId}/adjust | 異常系：ID不存在 | 404 Not Foundが返ること | `updateStock(99L, ...)` が `NoSuchElementException` をスローするようモック設定する | `PATCH /api/stocks/99/adjust` に `{"delta":10}` を送信する | HTTPステータス404であること |
| IT-SC-007 | PATCH /api/stocks/{productId}/adjust | 異常系：deltaキー欠落 | 400 Bad Requestが返ること | リクエストボディから "delta" キーを省略する | `PATCH /api/stocks/1/adjust` に `{}` を送信する | HTTPステータス400であること |

---

## 4. CORS設定（CorsConfig × Controller）

| No. | テスト対象（画面/項目） | テスト観点 | 確認内容 | テスト条件 | テスト手順 | 期待値 |
|-----|----------------------|------------|----------|------------|------------|--------|
| IT-CORS-001 | CorsConfig / 許可オリジン | 正常系：許可済みオリジンからのリクエスト | 許可済みオリジンからのリクエストにCORSヘッダーが付与されること | `cors.allowed-origins=http://localhost:5500` を設定する | `OPTIONS /api/orders` を `Origin: http://localhost:5500` で送信する | `Access-Control-Allow-Origin: http://localhost:5500` ヘッダーが含まれること |
| IT-CORS-002 | CorsConfig / 未許可オリジン | 異常系：未許可オリジンからのリクエスト | 未許可オリジンからのリクエストはCORSヘッダーが付与されないこと | `cors.allowed-origins=http://localhost:5500` を設定する | `OPTIONS /api/orders` を `Origin: https://evil.example.com` で送信する | `Access-Control-Allow-Origin` ヘッダーが含まれないこと |
| IT-CORS-003 | CorsConfig / 許可メソッド | 正常系：GET/POST/PUT/DELETE/OPTIONSが許可されること | プリフライトリクエストで許可メソッドが返ること | 許可済みオリジンからOPTIONSリクエストを送信する | `OPTIONS /api/orders` を送信し、`Access-Control-Request-Method: POST` を付与する | `Access-Control-Allow-Methods` に POST が含まれること |

---

## 5. Frontend × Backend API 連携

> **前提：** バックエンド（Spring Boot）を `localhost:8080` で起動済みの状態で実施する。
> フロントエンドは `localhost:5500`（Live Server等）で起動する。

| No. | テスト対象（画面/項目） | テスト観点 | 確認内容 | テスト条件 | テスト手順 | 期待値 |
|-----|----------------------|------------|----------|------------|------------|--------|
| IT-FE-001 | 受注管理画面 / 「↺ 更新」ボタン | API通信：受注一覧取得 | ボタン押下でAPIからデータを取得し、テーブルに反映されること | バックエンドが起動していること。`GET /api/orders` が受注リストを返すこと | 受注管理画面を開き「↺ 更新」ボタンをクリックする | テーブルがAPIレスポンスのデータで再描画されること。ブラウザDevToolsのNetworkタブで `GET /api/orders` に200が返ること |
| IT-FE-002 | 請求管理画面 / 「↺ 更新」ボタン | API通信：請求書一覧取得 | ボタン押下でAPIから請求書データを取得できること | バックエンドが起動していること。`GET /api/invoices` が請求書リストを返すこと | 請求管理画面を開き「↺ 更新」ボタンをクリックする | テーブルがAPIレスポンスのデータで再描画されること |
| IT-FE-003 | 在庫管理画面 / 「↺ 更新」ボタン | API通信：在庫一覧取得 | ボタン押下でAPIから在庫データを取得できること | バックエンドが起動していること。`GET /api/stocks` が在庫リストを返すこと | 在庫管理画面を開き「↺ 更新」ボタンをクリックする | テーブルがAPIレスポンスのデータで再描画されること |
| IT-FE-004 | 全画面 / 「↺ 更新」ボタン | 異常系：バックエンド停止時 | API通信失敗時にエラーメッセージを表示し、ダミーデータにフォールバックすること | バックエンドを停止した状態（または存在しないURLを設定）にする | 「↺ 更新」ボタンをクリックする | エラーメッセージが一時表示された後、ダミーデータでテーブルが再描画されること。ページがクラッシュしないこと |
| IT-FE-005 | config.js / API_BASE_URL 切り替え | 環境変数切り替え | `window.API_BASE_URL` の値がAPI通信に反映されること | `index.html` に `<script>window.API_BASE_URL = "http://localhost:8080";</script>` を追加する | ブラウザのDevToolsのNetworkタブで通信先URLを確認する | API通信のURLが `http://localhost:8080/api/orders` になっていること |
| IT-FE-006 | 受注管理画面 / 詳細モーダル | 画面内連携：モーダル表示 | テーブルの「詳細」クリックで正しい受注データがモーダルに表示されること | ダミーデータで画面が描画されていること | 受注一覧の1行目の「詳細」ボタンをクリックする | モーダルが開き、対象受注の受注番号・顧客名・金額・明細が正しく表示されること |
| IT-FE-007 | 請求管理画面 / 詳細モーダル | 画面内連携：金額計算表示 | モーダル内の小計・税額・税込合計が正しく表示されること | ダミーデータの請求書（amount=103800, tax=10380, totalWithTax=114180）を使用する | 請求一覧の「詳細」ボタンをクリックする | モーダル内に「¥103,800」「¥10,380」「¥114,180」が正しく表示されること |
| IT-FE-008 | ナビゲーション / 確認待ちバッジ | 状態連動：バッジ表示 | PENDING受注が存在するとき、ナビゲーションに件数バッジが表示されること | DUMMY_ORDERSにPENDINGが1件以上含まれる状態でページを開く | ダッシュボードを表示する（初期表示） | サイドバーの「受注管理」ナビ項目にオレンジ色のバッジと件数が表示されること |
