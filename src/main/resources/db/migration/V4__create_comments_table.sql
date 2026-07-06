CREATE TABLE comments (
    id UUID PRIMARY KEY,
    content VARCHAR(2000) NOT NULL,
    task_id UUID NOT NULL REFERENCES tasks (id) ON DELETE CASCADE,
    author_id UUID NOT NULL REFERENCES users (id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX idx_comments_task ON comments (task_id);
