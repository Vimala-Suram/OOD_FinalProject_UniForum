-- SQL Query to create Post_Votes table for tracking individual user votes
-- This table ensures one user can only vote once per post

CREATE TABLE IF NOT EXISTS Post_Votes (
    vote_id INTEGER PRIMARY KEY AUTOINCREMENT,
    post_id INTEGER NOT NULL,
    user_id INTEGER NOT NULL,
    vote_type INTEGER NOT NULL,  -- 1 for upvote, -1 for downvote
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(post_id, user_id),  -- Ensures one vote per user per post
    FOREIGN KEY (post_id) REFERENCES Posts(post_id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES Users(user_id) ON DELETE CASCADE
);

-- Create index for faster lookups
CREATE INDEX IF NOT EXISTS idx_post_votes_post_user ON Post_Votes(post_id, user_id);

-- Verify the table was created
-- SELECT name FROM sqlite_master WHERE type='table' AND name='Post_Votes';

