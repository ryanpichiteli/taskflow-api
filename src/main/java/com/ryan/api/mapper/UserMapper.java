package com.ryan.api.mapper;

import com.ryan.api.dto.user.UserResponse;
import com.ryan.api.entity.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserResponse toResponse(User user);
}
