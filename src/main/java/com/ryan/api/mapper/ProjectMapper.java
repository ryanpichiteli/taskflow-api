package com.ryan.api.mapper;

import com.ryan.api.dto.project.ProjectResponse;
import com.ryan.api.entity.Project;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = UserMapper.class)
public interface ProjectMapper {

    ProjectResponse toResponse(Project project);
}
