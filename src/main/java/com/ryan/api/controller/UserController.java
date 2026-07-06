package com.ryan.api.controller;

import com.ryan.api.dto.common.PageResponse;
import com.ryan.api.dto.user.UserResponse;
import com.ryan.api.security.UserPrincipal;
import com.ryan.api.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@Tag(name = "Users", description = "Consulta de usuarios")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    @Operation(summary = "Retorna os dados do usuario autenticado")
    public UserResponse me(@AuthenticationPrincipal UserPrincipal principal) {
        return userService.getById(principal.getId());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca um usuario pelo id")
    public UserResponse getById(@PathVariable UUID id) {
        return userService.getById(id);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Lista todos os usuarios (somente ADMIN)")
    public PageResponse<UserResponse> list(Pageable pageable) {
        return PageResponse.from(userService.list(pageable));
    }
}
