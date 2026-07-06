package com.ryan.api.repository.spec;

import com.ryan.api.entity.Project;
import com.ryan.api.enums.ProjectStatus;
import org.springframework.data.jpa.domain.Specification;

import java.util.UUID;

public final class ProjectSpecifications {

    private ProjectSpecifications() {
    }

    public static Specification<Project> nameContains(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        String pattern = "%" + name.toLowerCase() + "%";
        return (root, query, cb) -> cb.like(cb.lower(root.get("name")), pattern);
    }

    public static Specification<Project> hasStatus(ProjectStatus status) {
        if (status == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    public static Specification<Project> isMember(UUID userId) {
        return (root, query, cb) -> {
            query.distinct(true);
            return cb.or(
                    cb.equal(root.get("owner").get("id"), userId),
                    cb.equal(root.join("members").get("id"), userId));
        };
    }
}
