package com.ryan.api.controller;

import com.ryan.api.dto.comment.CommentCreateRequest;
import com.ryan.api.dto.comment.CommentResponse;
import com.ryan.api.dto.common.PageResponse;
import com.ryan.api.security.UserPrincipal;
import com.ryan.api.service.CommentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@Tag(name = "Comments", description = "Comentarios em tarefas")
public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @PostMapping("/api/tasks/{taskId}/comments")
    @Operation(summary = "Adiciona um comentario a uma tarefa")
    public ResponseEntity<CommentResponse> create(@PathVariable UUID taskId,
                                                   @Valid @RequestBody CommentCreateRequest request,
                                                   @AuthenticationPrincipal UserPrincipal principal) {
        CommentResponse response = commentService.create(taskId, request, principal.getUser());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/api/tasks/{taskId}/comments")
    @Operation(summary = "Lista comentarios de uma tarefa")
    public PageResponse<CommentResponse> list(@PathVariable UUID taskId,
                                               @AuthenticationPrincipal UserPrincipal principal,
                                               Pageable pageable) {
        return PageResponse.from(commentService.list(taskId, principal.getUser(), pageable));
    }

    @DeleteMapping("/api/comments/{id}")
    @Operation(summary = "Remove um comentario (somente autor ou ADMIN)")
    public ResponseEntity<Void> delete(@PathVariable UUID id, @AuthenticationPrincipal UserPrincipal principal) {
        commentService.delete(id, principal.getUser());
        return ResponseEntity.noContent().build();
    }
}
