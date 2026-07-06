package com.ryan.api.mapper;

import com.ryan.api.dto.task.TaskResponse;
import com.ryan.api.entity.Task;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = UserMapper.class)
public interface TaskMapper {

    @Mapping(target = "projectId", source = "project.id")
    TaskResponse toResponse(Task task);
}
