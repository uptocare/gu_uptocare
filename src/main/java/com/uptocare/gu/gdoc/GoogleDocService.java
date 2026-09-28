package com.uptocare.gu.gdoc;

import java.io.IOException;
import java.net.URI;
import java.net.URLDecoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;

/**
 * 抓取 Google 文件，去除 Google 的排版樣式，只保留內容結構
 * （標題、段落、粗體/斜體、清單、表格、圖片、連結），交給網站自己的 CSS 呈現。
 *
 * 支援兩種連結：
 *  - 一般分享連結 https://docs.google.com/document/d/{id}/edit...（需「知道連結的任何人可檢視」）
 *  - 發布到網路 https://docs.google.com/document/d/e/{id}/pub
 */
public final class GoogleDocService {

    private static final Duration CACHE_TTL = Duration.ofMinutes(10);
    private static final Pattern DOC_URL =
        Pattern.compile("^https://docs\\.google\\.com/document/d/(e/)?([A-Za-z0-9_-]+)");
    private static final Pattern CSS_RULE = Pattern.compile("\\.([A-Za-z0-9_-]+)\\{([^}]*)\\}");
    private static final Pattern BG_COLOR = Pattern.compile("background-color:(#[0-9a-fA-F]{6})");
    private static final Pattern GOOGLE_REDIRECT_Q = Pattern.compile("[?&]q=([^&]+)");

    private static final Set<String> BLOCK_TAGS = Set.of(
        "p", "ul", "ol", "li", "table", "thead", "tbody", "tr", "td", "th",
        "blockquote", "br", "sup", "sub");
    private static final Set<String> DROP_TAGS = Set.of(
        "script", "style", "noscript", "iframe", "object", "embed", "form", "input",
        "button", "svg", "head", "meta", "link", "title", "textarea", "select");

    private static final HttpClient HTTP = HttpClient.newBuilder()
        .followRedirects(HttpClient.Redirect.NORMAL)
        .connectTimeout(Duration.ofSeconds(5))
        .build();

    private static final Map<String, Cached> CACHE = new ConcurrentHashMap<>();

    private record Cached(String html, long fetchedAt) {}

    private GoogleDocService() {}

    /** 回傳整理過的 HTML。抓取失敗時若有舊快取就用舊的，否則丟出例外。 */
    public static String render(String docUrl) throws IOException {
        Cached c = CACHE.get(docUrl);
        long now = System.currentTimeMillis();
        if (c != null && now - c.fetchedAt < CACHE_TTL.toMillis()) return c.html;
        try {
            String html = clean(fetch(docUrl));
            CACHE.put(docUrl, new Cached(html, now));
            return html;
        } catch (IOException | InterruptedException e) {
            if (c != null) return c.html;
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            throw e instanceof IOException io ? io : new IOException(e);
        }
    }

    static String fetch(String docUrl) throws IOException, InterruptedException {
        Matcher m = DOC_URL.matcher(docUrl.strip());
        if (!m.find()) throw new IOException("不是 Google 文件連結：" + docUrl);
        String url = m.group(1) != null
            ? "https://docs.google.com/document/d/e/" + m.group(2) + "/pub"
            : "https://docs.google.com/document/d/" + m.group(2) + "/export?format=html";

        HttpResponse<String> res = HTTP.send(
            HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(10)).GET().build(),
            HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (res.uri().getHost().startsWith("accounts.")) {
            throw new IOException("文件尚未開放：請在 Google 文件「共用」設定為「知道連結的任何人 → 檢視者」");
        }
        if (res.statusCode() != 200) throw new IOException("Google 回應 HTTP " + res.statusCode());
        return res.body();
    }

    /** 把 Google 匯出的 HTML 轉成乾淨的內容 HTML。 */
    public static String clean(String googleHtml) {
        Document src = Jsoup.parse(googleHtml);

        // Google 以 CSS class 表示粗體/斜體等，先讀出每個 class 代表的格式
        Map<String, String> classStyles = new HashMap<>();
        for (Element style : src.select("style")) {
            Matcher m = CSS_RULE.matcher(style.data());
            while (m.find()) classStyles.merge(m.group(1), m.group(2), (a, b) -> a + ";" + b);
        }

        // 移除文件中的「註解」與其參照，避免把審閱意見公開
        for (Element ref : src.select("a[id^=cmnt_ref]")) {           // 內文中的 [a] 參照
            (ref.parent() != null && ref.parent().tagName().equals("sup") ? ref.parent() : ref).remove();
        }
        for (Element a : src.select("a[id^=cmnt]")) {                 // 文末的註解內容區塊
            Element block = a.parent();
            while (block != null && !block.tagName().equals("div")) block = block.parent();
            boolean isWrapper = block == null || block.id().equals("contents") || block == src.body();
            (isWrapper ? a.parent() : block).remove();
        }

        Element root = src.getElementById("contents");     // 「發布到網路」版本
        if (root == null) root = src.body();

        Document out = Document.createShell("");
        Element body = out.body();
        for (Node n : root.childNodes()) copy(n, body, classStyles);
        removeEmptyParagraphs(body);
        return body.html();
    }

    private static void copy(Node node, Element dst, Map<String, String> classStyles) {
        if (node instanceof TextNode t) {
            dst.appendText(t.text());
            return;
        }
        if (!(node instanceof Element el)) return;

        String tag = el.tagName().toLowerCase();
        if (DROP_TAGS.contains(tag)) return;

        Element target;
        switch (tag) {
            case "h1", "h2", "h3", "h4", "h5", "h6" -> {
                // 網頁的 h1 是文章標題，所以文件內的標題往下降一級
                int level = Math.min(6, tag.charAt(1) - '0' + 1);
                target = dst.appendElement("h" + level);
                if (!el.id().isEmpty()) target.attr("id", el.id());
            }
            case "p" -> {
                if (el.hasClass("title")) return;                    // 已有網頁標題
                target = dst.appendElement("p");
                if (el.hasClass("subtitle")) target.addClass("lead");
            }
            case "a" -> {
                String href = cleanHref(el.attr("href"));
                target = dst.appendElement("a");
                if (href != null) {
                    target.attr("href", href);
                    if (href.startsWith("http")) target.attr("target", "_blank").attr("rel", "noopener");
                }
                if (!el.id().isEmpty()) target.attr("id", el.id());
            }
            case "img" -> {
                String srcUrl = el.attr("src");
                if (!srcUrl.startsWith("https://")) return;
                dst.appendElement("img")
                    .attr("src", srcUrl)
                    .attr("alt", el.attr("alt"))
                    .attr("loading", "lazy")
                    .addClass("img-fluid");
                return;
            }
            case "hr" -> {
                if (!el.attr("style").contains("display:none")) dst.appendElement("hr");
                return;
            }
            case "table" -> {
                Element wrap = dst.appendElement("div").addClass("table-responsive");
                target = wrap.appendElement("table").addClass("table table-bordered");
            }
            case "td", "th" -> {
                target = dst.appendElement(tag);
                copyAttr(el, target, "colspan");
                copyAttr(el, target, "rowspan");
            }
            case "span" -> target = wrapFormatting(el, dst, classStyles);
            default -> target = BLOCK_TAGS.contains(tag) ? dst.appendElement(tag) : dst;  // 其他標籤只保留內容
        }
        for (Node child : el.childNodes()) copy(child, target, classStyles);
    }

    /** 依 span 的 class 還原粗體/斜體/底線/刪除線/螢光筆。 */
    private static Element wrapFormatting(Element span, Element dst, Map<String, String> classStyles) {
        Set<String> fmt = new HashSet<>();
        for (String cls : span.classNames()) {
            String css = classStyles.getOrDefault(cls, "").replace(" ", "");
            if (css.contains("font-weight:700") || css.contains("font-weight:bold")) fmt.add("strong");
            if (css.contains("font-style:italic")) fmt.add("em");
            if (css.contains("text-decoration:underline")) fmt.add("u");
            if (css.contains("line-through")) fmt.add("s");
            Matcher bg = BG_COLOR.matcher(css);
            if (bg.find() && !bg.group(1).equalsIgnoreCase("#ffffff")) fmt.add("mark");
        }
        // 連結本身就有底線，Google 會在連結外（或內）的 span 再標一次
        boolean isLink = span.children().stream().anyMatch(c -> c.tagName().equals("a"))
            || (span.parent() != null && span.parent().tagName().equals("a"));
        if (isLink) fmt.remove("u");
        Element target = dst;
        for (String t : new String[] {"mark", "strong", "em", "u", "s"}) {
            if (fmt.contains(t)) target = target.appendElement(t);
        }
        return target;
    }

    /** Google 會把外部連結包成 google.com/url?q=...，這裡解開；只允許安全的網址。 */
    private static String cleanHref(String href) {
        if (href == null || href.isEmpty()) return null;
        if (href.startsWith("https://www.google.com/url?")) {
            Matcher q = GOOGLE_REDIRECT_Q.matcher(href);
            if (q.find()) href = URLDecoder.decode(q.group(1), StandardCharsets.UTF_8);
        }
        String lower = href.toLowerCase();
        if (lower.startsWith("http://") || lower.startsWith("https://")
                || lower.startsWith("mailto:") || href.startsWith("#")) {
            return href;
        }
        return null;
    }

    private static void copyAttr(Element from, Element to, String attr) {
        if (from.hasAttr(attr)) to.attr(attr, from.attr(attr));
    }

    /** Google 文件常用空白段落排版，網頁上改由 CSS 控制間距。 */
    private static void removeEmptyParagraphs(Element root) {
        for (Element p : root.select("p")) {
            if (p.text().replace(' ', ' ').trim().isEmpty() && p.select("img").isEmpty()) {
                p.remove();
            }
        }
    }
}
