package edu.northeastern.uniforum.forum.dao;

import edu.northeastern.uniforum.db.Database;
import edu.northeastern.uniforum.forum.util.TimeUtil;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ReplyDAO {

    public static class ReplyDTO {
        public int replyId;
        public int postId;
        public String author;
        public String content;
        public String timeAgo;
        public int likes;
        public Integer parentReplyId;
    }

    public List<ReplyDTO> getRepliesByPostId(int postId) throws SQLException {
        List<ReplyDTO> list = new ArrayList<>();

        String sql = """
            SELECT r.reply_id, r.post_id, r.content, r.number_of_likes, r.created_time,
                   u.user_name
            FROM Replies r
            JOIN Users u ON r.user_id = u.user_id
            WHERE r.post_id = ?
            ORDER BY r.created_time ASC
            """;

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, postId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ReplyDTO dto = new ReplyDTO();
                    dto.replyId = rs.getInt("reply_id");
                    dto.postId = rs.getInt("post_id");
                    dto.content = rs.getString("content");
                    dto.likes = rs.getInt("number_of_likes");
                    dto.author = rs.getString("user_name");
                    dto.parentReplyId = null;

                    Timestamp ts = rs.getTimestamp("created_time");
                    if (ts != null) {
                        LocalDateTime created = ts.toLocalDateTime();
                        dto.timeAgo = TimeUtil.timeAgo(created);
                    } else {
                        dto.timeAgo = "unknown";
                    }

                    list.add(dto);
                }
            }
        }

        return list;
    }

    public void createReply(int postId, int userId, String content) throws SQLException {
        String sql = """
            INSERT INTO Replies (post_id, user_id, content, number_of_likes, created_time)
            VALUES (?, ?, ?, 0, CURRENT_TIMESTAMP)
            """;

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, postId);
            ps.setInt(2, userId);
            ps.setString(3, content);
            ps.executeUpdate();

            updatePostReplyCount(conn, postId);
        }
    }

    private void updatePostReplyCount(Connection conn, int postId) throws SQLException {
        String sql = """
            UPDATE Posts 
            SET number_of_replies = (
                SELECT COUNT(*) FROM Replies WHERE post_id = ?
            )
            WHERE post_id = ?
            """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, postId);
            ps.setInt(2, postId);
            ps.executeUpdate();
        }
    }
}
