# Vercel デプロイガイド
## ― LumberCore フロントエンド デプロイ手順 ―

---

## 前提条件

| 必要なもの | 確認方法 |
|------------|----------|
| GitHub アカウント | https://github.com にログインできること |
| Vercel アカウント | https://vercel.com にログインできること（GitHub 連携推奨）|
| Node.js 18以上 （Vercel CLI 用） | `node -v` |
| Vercel CLI（任意） | `npm i -g vercel` → `vercel --version` |

---

## 方法A: GitHub 連携（推奨・最も簡単）

### ステップ1: リポジトリを GitHub にプッシュ

```bash
# リポジトリ初期化（既にGitが設定済みならスキップ）
cd /path/to/your/project
git init
git add .
git commit -m "初回コミット"

# GitHubにリポジトリを作成後、リモートを追加してプッシュ
git remote add origin https://github.com/<ユーザー名>/<リポジトリ名>.git
git branch -M main
git push -u origin main
```

### ステップ2: Vercel にデプロイ

1. https://vercel.com/dashboard にアクセス
2. **「Add New」→「Project」** をクリック
3. **「Import Git Repository」** で GitHub アカウントを連携
4. 対象リポジトリを選択し **「Import」** をクリック

### ステップ3: ビルド設定

| 設定項目 | 値 |
|----------|-----|
| **Framework Preset** | `Other`（Vanilla HTML/JSのため）|
| **Root Directory** | `frontend`（frontendフォルダにindex.htmlがある場合）|
| **Build Command** | 空欄（ビルド不要）|
| **Output Directory** | `.`（ルートディレクトリ）または空欄 |

> **注意**: `frontend/index.html` を配置している場合、Root Directory を `frontend` に設定する

### ステップ4: 環境変数の設定（バックエンドURL）

1. Vercel プロジェクトの **「Settings」→「Environment Variables」** を開く
2. 以下を追加:

| 変数名 | 値 | 環境 |
|--------|----|------|
| （今回は vercel.json の rewrites を使用） | - | - |

**または** `frontend/index.html` の以下コメントを外してバックエンドURLを設定:

```html
<script>window.API_BASE_URL = "https://your-backend-api.example.com";</script>
```

### ステップ5: デプロイ実行

- **「Deploy」** ボタンをクリック
- 1〜2分でデプロイ完了
- 発行された URL（例: `https://lumber-core.vercel.app`）でアクセス確認

---

## 方法B: Vercel CLI を使ったデプロイ

```bash
# 1. Vercel CLI インストール
npm install -g vercel

# 2. ログイン
vercel login

# 3. frontendディレクトリでデプロイ
cd frontend
vercel

# 初回は対話形式で設定
# ? Set up and deploy? → Y
# ? Which scope? → 自分のアカウントを選択
# ? Link to existing project? → N（新規の場合）
# ? What's your project's name? → lumber-core
# ? In which directory is your code located? → ./
# ? Want to override the settings? → N

# 4. 本番デプロイ
vercel --prod
```

---

## バックエンド (Spring Boot) との疎通確認

### ローカル環境での疎通確認

```bash
# 1. バックエンド起動
cd backend
./mvnw spring-boot:run

# 2. フロントエンドをローカルサーバーで起動（Live Server等）
# VS Code の Live Server 拡張: index.html を右クリック → "Open with Live Server"
# または Python の簡易サーバー:
cd frontend
python3 -m http.server 5500

# 3. ブラウザで http://localhost:5500 を開く
# 4. 各画面の「🔄 API取得」ボタンをクリック
# 5. ブラウザの開発者ツール（F12）→ Network タブで通信を確認
```

### 本番環境（Vercel + バックエンド）の疎通確認

#### CORS設定の確認

バックエンドの `application.properties` に Vercel のドメインを追加:

```properties
cors.allowed-origins=https://lumber-core.vercel.app,http://localhost:5500
```

#### vercel.json によるプロキシ設定（CORS回避の別手段）

```json
{
  "rewrites": [
    {
      "source": "/api/(.*)",
      "destination": "https://your-backend-api.example.com/api/$1"
    }
  ]
}
```

この設定を使う場合、フロントエンドは `/api/orders` のように**相対パス**でリクエストするだけで
自動的にバックエンドへ転送されます。`window.API_BASE_URL` を空文字に設定してください。

---

## バックエンドのホスティング先（推奨サービス）

| サービス | 特徴 | 無料枠 |
|----------|------|--------|
| **Render** | Spring Boot に対応、設定が簡単 | あり（スリープあり） |
| **Railway** | Docker不要、Gitプッシュで自動デプロイ | あり |
| **Fly.io** | Dockerコンテナデプロイ | あり |
| **AWS Elastic Beanstalk** | 本番運用向け | 12ヶ月無料枠 |

---

## トラブルシューティング

| 症状 | 原因 | 対処 |
|------|------|------|
| CORS エラー（Access-Control-Allow-Origin）| バックエンドの CORS 設定にフロントのオリジンが未登録 | `application.properties` の `cors.allowed-origins` に Vercel ドメインを追加 |
| API取得ボタンを押しても何も変わらない | バックエンドが起動していない / URL が違う | `window.API_BASE_URL` を正しく設定、バックエンドの起動を確認 |
| Vercel で 404 になる | Root Directory の設定ミス | Vercel の Project Settings → Root Directory を `frontend` に変更 |
| ビルドエラー（npm関連）| Framework Preset が間違っている | Framework Preset を `Other` に設定 |
| 環境変数が反映されない | 再デプロイが必要 | Environment Variables 変更後は Redeploy が必要 |

---

## デプロイ後チェックリスト

- [ ] Vercel の発行URLでトップページが表示される
- [ ] サイドナビゲーションで各画面に遷移できる
- [ ] ダッシュボードの KPI カードに数値が表示される
- [ ] 受注一覧・請求一覧でダミーデータが表示される
- [ ] フィルター・検索が動作する
- [ ] 詳細モーダルが開閉できる
- [ ] バックエンド起動後、「🔄 API取得」ボタンでデータ取得できる（疎通確認）
- [ ] ブラウザの開発者ツールでコンソールエラーがない
