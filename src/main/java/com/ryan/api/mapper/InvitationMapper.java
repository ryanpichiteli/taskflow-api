package com.ryan.api.mapper;

import com.ryan.api.dto.invitation.InvitationResponse;
import com.ryan.api.entity.ProjectInvitation;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = UserMapper.class)
public interface InvitationMapper {

    @Mapping(target = "projectId", source = "project.id")
    @Mapping(target = "projectName", source = "project.name")
    InvitationResponse toResponse(ProjectInvitation invitation);
}
