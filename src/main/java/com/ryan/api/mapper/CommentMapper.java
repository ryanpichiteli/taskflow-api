package com.ryan.api.mapper;

import com.ryan.api.dto.comment.CommentResponse;
import com.ryan.api.entity.Comment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = UserMapper.class)
public interface CommentMapper {

    @Mapping(target = "taskId", source = "task.id")
    CommentResponse toResponse(Comment comment);
}
