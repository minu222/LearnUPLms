package com.lms.mainpages.repository;

import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.*;
import java.util.*;

@Component
public class PostSchemaResolver {

    public static final class Resolved {
        public final String table;    // "posts"
        public final String id;       // id/post_id/...
        public final String author;   // author_id/writer_id/user_id/...
        public final String title;    // title/subject
        public final String content;  // content/body/text
        public final String category; // category/cat/type (optional)
        public final String created;  // created_at/reg_date/... (optional)

        public Resolved(String table, String id, String author, String title, String content, String category, String created) {
            this.table = table;
            this.id = id;
            this.author = author;
            this.title = title;
            this.content = content;
            this.category = category;
            this.created = created;
        }
    }

    private final DataSource dataSource;
    private volatile Resolved cached; // 1회 해석 후 캐시

    public PostSchemaResolver(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public Resolved get() {
        var c = cached;
        if (c != null) return c;
        synchronized (this) {
            if (cached == null) {
                cached = resolveNow();
                // ✅ 매핑 결과 1회 출력 (콘솔)
                System.out.println("[PostSchemaResolver] table=" + cached.table
                        + ", id=" + cached.id
                        + ", author=" + cached.author
                        + ", title=" + cached.title
                        + ", content=" + cached.content
                        + ", category=" + cached.category
                        + ", created=" + cached.created);
            }
            return cached;
        }
    }

    private Resolved resolveNow() {
        String table = "posts"; // 테이블명은 현재 'posts'로 고정 사용
        Set<String> cols = new HashSet<>();
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "SELECT column_name FROM information_schema.columns " +
                             "WHERE table_schema = database() AND table_name = ?")) {
            ps.setString(1, table);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) cols.add(rs.getString(1).toLowerCase(Locale.ROOT));
            }
        } catch (SQLException e) {
            throw new IllegalStateException("posts 테이블 메타 조회 실패", e);
        }

        String id       = pick(cols, "id","post_id","board_id","postid","boardid");
        String author   = pick(cols, "author_id","writer_id","user_id","authorid","writerid","userid");
        String title    = pick(cols, "title","subject");
        String content  = pick(cols, "content","body","text");
        String category = pickOptional(cols, "category","cat","type");
        String created  = pickOptional(cols, "created_at","createdat","reg_date","reg_dt","created","write_date");

        if (id == null || author == null || title == null || content == null) {
            throw new IllegalStateException("posts 컬럼 매핑 실패 (id/author/title/content 필수 컬럼명을 확인하세요)");
        }
        return new Resolved(table, id, author, title, content, category, created);
    }

    private static String pick(Set<String> cols, String... candidates) {
        for (String c : candidates) if (cols.contains(c)) return c;
        return null;
    }
    private static String pickOptional(Set<String> cols, String... candidates) {
        for (String c : candidates) if (cols.contains(c)) return c;
        return null;
    }

}

