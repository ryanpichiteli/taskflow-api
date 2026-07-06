package com.ryan.api.controller;

import com.ryan.api.dto.common.PageResponse;
import com.ryan.api.dto.invitation.InvitationResponse;
import com.ryan.api.dto.invitation.InviteMemberRequest;
import com.ryan.api.enums.InvitationStatus;
import com.ryan.api.security.UserPrincipal;
import com.ryan.api.service.ProjectInvitationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@Tag(name = "Invitations", description = "Convites para participar de projetos")
public class ProjectInvitationController {

    private final ProjectInvitationService invitationService;

    public ProjectInvitationController(ProjectInvitationService invitationService) {
        this.invitationService = invitationService;
    }

    @PostMapping("/api/projects/{projectId}/invitations")
    @Operation(summary = "Convida um usuario para o projeto pelo e-mail (somente owner ou ADMIN)")
    public ResponseEntity<InvitationResponse> invite(@PathVariable UUID projectId,
                                                      @Valid @RequestBody InviteMemberRequest request,
                                                      @AuthenticationPrincipal UserPrincipal principal) {
        InvitationResponse response = invitationService.invite(projectId, request.email(), principal.getUser());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/api/projects/{projectId}/invitations")
    @Operation(summary = "Lista convites enviados de um projeto (somente owner ou ADMIN)")
    public PageResponse<InvitationResponse> listForProject(@PathVariable UUID projectId,
                                                            @AuthenticationPrincipal UserPrincipal principal,
                                                            Pageable pageable) {
        return PageResponse.from(invitationService.listForProject(projectId, principal.getUser(), pageable));
    }

    @GetMapping("/api/invitations/me")
    @Operation(summary = "Lista os convites recebidos pelo usuario autenticado")
    public PageResponse<InvitationResponse> listForCurrentUser(@RequestParam(required = false) InvitationStatus status,
                                                                 @AuthenticationPrincipal UserPrincipal principal,
                                                                 Pageable pageable) {
        return PageResponse.from(invitationService.listForCurrentUser(status, principal.getUser(), pageable));
    }

    @PostMapping("/api/invitations/{id}/accept")
    @Operation(summary = "Aceita um convite recebido")
    public InvitationResponse accept(@PathVariable UUID id, @AuthenticationPrincipal UserPrincipal principal) {
        return invitationService.accept(id, principal.getUser());
    }

    @PostMapping("/api/invitations/{id}/decline")
    @Operation(summary = "Recusa um convite recebido")
    public InvitationResponse decline(@PathVariable UUID id, @AuthenticationPrincipal UserPrincipal principal) {
        return invitationService.decline(id, principal.getUser());
    }
}
