package com.ryan.api.repository;

import com.ryan.api.entity.Comment;
import com.ryan.api.entity.Task;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CommentRepository extends JpaRepository<Comment, UUID> {

    Page<Comment> findByTask(Task task, Pageable pageable);
}
