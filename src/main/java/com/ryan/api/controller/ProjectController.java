package com.ryan.api.controller;

import com.ryan.api.dto.common.PageResponse;
import com.ryan.api.dto.project.ProjectCreateRequest;
import com.ryan.api.dto.project.ProjectResponse;
import com.ryan.api.dto.project.ProjectUpdateRequest;
import com.ryan.api.enums.ProjectStatus;
import com.ryan.api.security.UserPrincipal;
import com.ryan.api.service.ProjectService;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/projects")
@Tag(name = "Projects", description = "Gerenciamento de projetos")
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @PostMapping
    @Operation(summary = "Cria um novo projeto")
    public ResponseEntity<ProjectResponse> create(@Valid @RequestBody ProjectCreateRequest request,
                                                   @AuthenticationPrincipal UserPrincipal principal) {
        ProjectResponse response = projectService.create(request, principal.getUser());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "Lista projetos do usuario autenticado, com filtros e paginacao")
    public PageResponse<ProjectResponse> list(@RequestParam(required = false) String name,
                                               @RequestParam(required = false) ProjectStatus status,
                                               @AuthenticationPrincipal UserPrincipal principal,
                                               Pageable pageable) {
        return PageResponse.from(projectService.list(name, status, principal.getUser(), pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca um projeto pelo id")
    public ProjectResponse getById(@PathVariable UUID id, @AuthenticationPrincipal UserPrincipal principal) {
        return projectService.getById(id, principal.getUser());
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza um projeto (somente owner ou ADMIN)")
    public ProjectResponse update(@PathVariable UUID id, @Valid @RequestBody ProjectUpdateRequest request,
                                   @AuthenticationPrincipal UserPrincipal principal) {
        return projectService.update(id, request, principal.getUser());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remove um projeto (somente owner ou ADMIN)")
    public ResponseEntity<Void> delete(@PathVariable UUID id, @AuthenticationPrincipal UserPrincipal principal) {
        projectService.delete(id, principal.getUser());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/members/{userId}")
    @Operation(summary = "Remove um membro do projeto (somente owner ou ADMIN)")
    public ProjectResponse removeMember(@PathVariable UUID id, @PathVariable UUID userId,
                                         @AuthenticationPrincipal UserPrincipal principal) {
        return projectService.removeMember(id, userId, principal.getUser());
    }
}
