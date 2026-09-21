package com.hms.accesscontrol.service.impl;

import com.hms.accesscontrol.dto.PermissionDto;
import com.hms.accesscontrol.entity.Permission;
import com.hms.accesscontrol.repository.PermissionRepository;
import com.hms.accesscontrol.service.PermissionService;
import com.hms.common.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PermissionServiceImpl implements PermissionService {

    private final PermissionRepository permissionRepository;

    @Override
    @Transactional
    public PermissionDto createPermission(PermissionDto permissionDto) {
        if (permissionRepository.findByName(permissionDto.getName()).isPresent()) {
            throw new BadRequestException("Permission already exists: " + permissionDto.getName());
        }

        Permission permission = Permission.builder()
                .name(permissionDto.getName())
                .module(permissionDto.getModule())
                .description(permissionDto.getDescription())
                .build();

        Permission saved = permissionRepository.save(permission);
        return mapToDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PermissionDto> getAllPermissions() {
        return permissionRepository.findAll().stream()
                .map(this::mapToDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PermissionDto> getPermissionsByModule(String module) {
        return permissionRepository.findByModule(module).stream()
                .map(this::mapToDto)
                .toList();
    }

    private PermissionDto mapToDto(Permission p) {
        return PermissionDto.builder()
                .id(p.getId())
                .name(p.getName())
                .module(p.getModule())
                .description(p.getDescription())
                .build();
    }
}
