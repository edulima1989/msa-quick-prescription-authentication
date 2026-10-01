package com.quickprescription.authentication.mapper;

import com.quickprescription.authentication.dto.RegisterRequest;
import com.quickprescription.authentication.dto.UsuarioRegistrado;
import com.quickprescription.authentication.model.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "userRole", ignore = true)
    User toUser(RegisterRequest request);

    @Mapping(target = "id", source = "userId")
    UsuarioRegistrado toUsuarioRegistrado(User user);
}
