package com.ryan.api.service;

import com.ryan.api.dto.comment.CommentCreateRequest;
import com.ryan.api.dto.comment.CommentResponse;
import com.ryan.api.entity.Comment;
import com.ryan.api.entity.Task;
import com.ryan.api.entity.User;
import com.ryan.api.enums.Role;
import com.ryan.api.exception.ForbiddenOperationException;
import com.ryan.api.exception.ResourceNotFoundException;
import com.ryan.api.mapper.CommentMapper;
import com.ryan.api.repository.CommentRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class CommentService {

    private final CommentRepository commentRepository;
    private final CommentMapper commentMapper;
    private final TaskService taskService;
    private final ProjectService projectService;

    public CommentService(CommentRepository commentRepository, CommentMapper commentMapper,
                           TaskService taskService, ProjectService projectService) {
        this.commentRepository = commentRepository;
        this.commentMapper = commentMapper;
        this.taskService = taskService;
        this.projectService = projectService;
    }

    @Transactional
    public CommentResponse create(UUID taskId, CommentCreateRequest request, User currentUser) {
        Task task = taskService.getEntityById(taskId);
        projectService.requireMembership(task.getProject(), currentUser);

        Comment comment = new Comment(request.content(), task, currentUser);
        return commentMapper.toResponse(commentRepository.save(comment));
    }

    public Page<CommentResponse> list(UUID taskId, User currentUser, Pageable pageable) {
        Task task = taskService.getEntityById(taskId);
        projectService.requireMembership(task.getProject(), currentUser);

        return commentRepository.findByTask(task, pageable).map(commentMapper::toResponse);
    }

    @Transactional
    public void delete(UUID id, User currentUser) {
        Comment comment = commentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found: " + id));

        boolean isAuthor = comment.getAuthor().getId().equals(currentUser.getId());
        if (currentUser.getRole() != Role.ADMIN && !isAuthor) {
            throw new ForbiddenOperationException("Only the comment author can delete this comment");
        }

        commentRepository.delete(comment);
    }
}
