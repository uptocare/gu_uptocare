# gu_uptocare

醫師個人網站（自我介紹、重要訊息、經驗分享），姊妹站為 crc.uptocare.com（`D:\webapps\crc_uptocare`）。

## Stack
- Eclipse Dynamic Web Project, **Tomcat 9 + JDK 17** → 使用 `javax.servlet`（Servlet 4.0），**不要**用 `jakarta.*`。
- JSP + JSTL 1.2（`WEB-INF/lib/taglibs-standard-*.jar`），taglib URI `http://java.sun.com/jsp/jstl/core`。
- jsoup 1.7.2（`WEB-INF/lib`）解析 Google 文件；舊版 API，不支援 `select("> a")` 這類開頭 combinator、沒有 `Safelist`。
- Bootstrap 5.3 + Bootstrap Icons（jsDelivr CDN）、Noto Serif TC（Google Fonts），在 `header.jspf` / `footer.jspf` 載入。
- 無 Maven/Gradle；Eclipse 編譯到 `build/classes`。

## Layout
- `WEB-INF/posts.txt` — 文章清單（key: value 區塊，空行分隔，含空白範本）。修改後自動重新讀取。
- `com.uptocare.gu.data.PostRepository` — 讀 posts.txt；路徑由 `web.AppInit`（@WebListener）設定。
- `com.uptocare.gu.gdoc.GoogleDocService` — 抓 Google 文件（`/export?format=html` 或 `/d/e/.../pub`），
  以白名單方式重建 HTML（去 Google 樣式、還原粗斜體、解開 google.com/url 連結、移除文件註解），快取 10 分鐘。
- `WEB-INF/jspf/header.jspf`（含 `navmenu.jspf`）、`footer.jspf` — 共用版型，用 `<%@ include file=... %>` 靜態引入。
  引入前設定 request attribute `pageTitle`、`activeNav`（home/about/news/share）；header 會定義 `${ctx}`。
- 頁面：`index.jsp`, `about.jsp`, `posts.jsp?type=NEWS|SHARE`, `post.jsp?id=`。
- `web.xml` 的 jsp-config 統一設定 UTF-8，fragment 不需要 page directive。

## Conventions
- 使用者/設定檔文字一律 `<c:out>` 輸出；只有 GoogleDocService 的輸出（已過濾）直接輸出。
- 文字介面為繁體中文；頁尾保留醫療免責聲明。

## Local test
Eclipse 以 Tomcat v9.0 執行，或用 `C:\apache-tomcat-9.0.122`：`http://localhost:8080/gu_uptocare/`。
命令列編譯：`javac -encoding UTF-8 -cp "C:\apache-tomcat-9.0.122\lib\servlet-api.jar;src\main\webapp\WEB-INF\lib\jsoup-1.7.2.jar" -d build\classes <java files>`
