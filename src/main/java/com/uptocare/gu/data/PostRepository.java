package com.uptocare.gu.data;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.uptocare.gu.model.Post;
import com.uptocare.gu.model.Post.Type;

/**
 * 文章清單，讀自 WEB-INF/posts.txt（格式見該檔開頭說明）。
 * 檔案有修改時會自動重新讀取，不需重啟 Tomcat。
 */
public final class PostRepository {

    private static Path file;
    private static long loadedModified = -1;
    private static List<Post> posts = List.of();

    private PostRepository() {}

    public static synchronized void init(Path postsFile) {
        file = postsFile;
        loadedModified = -1;
    }

    private static synchronized List<Post> all() {
        if (file == null) return posts;
        try {
            long modified = Files.getLastModifiedTime(file).toMillis();
            if (modified != loadedModified) {
                posts = parse(Files.readAllLines(file, StandardCharsets.UTF_8));
                loadedModified = modified;
            }
        } catch (IOException e) {
            System.err.println("[PostRepository] 無法讀取 " + file + ": " + e);
        }
        return posts;
    }

    /** 每篇文章是一段「key: value」，段落之間用空行分隔；# 開頭為註解。 */
    static List<Post> parse(List<String> lines) {
        List<Post> result = new ArrayList<>();
        Map<String, String> block = new HashMap<>();
        for (String raw : lines) {
            String line = raw.strip();
            if (line.startsWith("#")) continue;
            if (line.isEmpty()) {
                addPost(block, result);
                block.clear();
                continue;
            }
            int colon = line.indexOf(':');
            if (colon > 0) {
                block.put(line.substring(0, colon).strip().toLowerCase(), line.substring(colon + 1).strip());
            }
        }
        addPost(block, result);
        return List.copyOf(result);
    }

    private static void addPost(Map<String, String> b, List<Post> out) {
        if (b.isEmpty()) return;
        try {
            String doc = b.getOrDefault("doc", "");
            out.add(new Post(
                Integer.parseInt(b.get("id")),
                Type.valueOf(b.getOrDefault("type", "SHARE").toUpperCase()),
                b.getOrDefault("title", "(未命名)"),
                b.getOrDefault("summary", ""),
                doc.isEmpty() ? null : doc,
                LocalDate.parse(b.get("date")),
                Boolean.parseBoolean(b.getOrDefault("pinned", "false"))));
        } catch (RuntimeException e) {
            System.err.println("[PostRepository] 略過格式錯誤的文章 " + b + ": " + e);
        }
    }

    public static List<Post> findByType(Type type) {
        return all().stream()
            .filter(p -> p.getType() == type)
            .sorted(Comparator.comparing(Post::isPinned).reversed()
                .thenComparing(Post::getDate, Comparator.reverseOrder()))
            .collect(Collectors.toList());
    }

    public static List<Post> latest(Type type, int limit) {
        return findByType(type).stream().limit(limit).collect(Collectors.toList());
    }

    public static Post findById(int id) {
        return all().stream().filter(p -> p.getId() == id).findFirst().orElse(null);
    }
}
