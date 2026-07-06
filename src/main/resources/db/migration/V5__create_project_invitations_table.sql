CREATE TABLE project_invitations (
    id UUID PRIMARY KEY,
    project_id UUID NOT NULL REFERENCES projects (id) ON DELETE CASCADE,
    invited_user_id UUID NOT NULL REFERENCES users (id),
    invited_by UUID NOT NULL REFERENCES users (id),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    responded_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_invitations_invited_user ON project_invitations (invited_user_id, status);
CREATE UNIQUE INDEX uq_invitations_pending_project_user
    ON project_invitations (project_id, invited_user_id)
    WHERE status = 'PENDING';
