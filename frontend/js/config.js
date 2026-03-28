/**
 * API設定
 * 環境に応じてAPIのベースURLを切り替える
 *
 * ローカル開発時:  window.API_BASE_URL を定義するか、デフォルト値を利用
 * Vercel本番環境: Vercel環境変数 VITE_API_BASE_URL は使えないため、
 *                 index.html の <script> タグで window.API_BASE_URL を定義する
 *                 または vercel.json の rewrites でプロキシを設定する
 */
const API_BASE_URL =
  (typeof window !== "undefined" && window.API_BASE_URL) ||
  "http://localhost:8080";

/**
 * fetchラッパー
 * @param {string} path  - APIパス（例: /api/orders）
 * @param {object} options - fetchオプション
 * @returns {Promise<any>}
 */
async function apiFetch(path, options = {}) {
  const defaultOptions = {
    headers: {
      "Content-Type": "application/json",
      Accept: "application/json",
    },
  };
  const mergedOptions = {
    ...defaultOptions,
    ...options,
    headers: { ...defaultOptions.headers, ...(options.headers || {}) },
  };

  const response = await fetch(`${API_BASE_URL}${path}`, mergedOptions);

  if (!response.ok) {
    const errorBody = await response.text();
    throw new Error(
      `API Error: ${response.status} ${response.statusText} - ${errorBody}`
    );
  }

  // 204 No Content など body が空の場合の処理
  const contentType = response.headers.get("content-type");
  if (contentType && contentType.includes("application/json")) {
    return response.json();
  }
  return null;
}
