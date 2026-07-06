package com.ryan.api.service;

import com.ryan.api.dto.user.UserResponse;
import com.ryan.api.entity.User;
import com.ryan.api.exception.ResourceNotFoundException;
import com.ryan.api.mapper.UserMapper;
import com.ryan.api.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    public UserService(UserRepository userRepository, UserMapper userMapper) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
    }

    public User getEntityById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
    }

    public UserResponse getById(UUID id) {
        return userMapper.toResponse(getEntityById(id));
    }

    public Page<UserResponse> list(Pageable pageable) {
        return userRepository.findAll(pageable).map(userMapper::toResponse);
    }
}
