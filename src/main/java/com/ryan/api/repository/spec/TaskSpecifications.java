package com.ryan.api.repository.spec;

import com.ryan.api.entity.Project;
import com.ryan.api.entity.Task;
import com.ryan.api.enums.TaskPriority;
import com.ryan.api.enums.TaskStatus;
import org.springframework.data.jpa.domain.Specification;

import java.util.UUID;

public final class TaskSpecifications {

    private TaskSpecifications() {
    }

    public static Specification<Task> belongsToProject(Project project) {
        return (root, query, cb) -> cb.equal(root.get("project"), project);
    }

    public static Specification<Task> hasStatus(TaskStatus status) {
        if (status == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    public static Specification<Task> hasPriority(TaskPriority priority) {
        if (priority == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("priority"), priority);
    }

    public static Specification<Task> hasAssignee(UUID assigneeId) {
        if (assigneeId == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("assignee").get("id"), assigneeId);
    }

    public static Specification<Task> titleContains(String title) {
        if (title == null || title.isBlank()) {
            return null;
        }
        String pattern = "%" + title.toLowerCase() + "%";
        return (root, query, cb) -> cb.like(cb.lower(root.get("title")), pattern);
    }
}
