package com.msmeerp.inventory.service.impl;

import com.msmeerp.common.exception.BadRequestException;
import com.msmeerp.common.exception.ResourceNotFoundException;
import com.msmeerp.inventory.dto.WarehouseDto;
import com.msmeerp.inventory.dto.WarehouseUpsertRequest;
import com.msmeerp.inventory.entity.Warehouse;
import com.msmeerp.inventory.repository.WarehouseRepository;
import com.msmeerp.inventory.service.WarehouseService;
import com.msmeerp.tenant.context.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WarehouseServiceImpl implements WarehouseService {

    private final WarehouseRepository warehouseRepository;

    @Override
    @Transactional
    public WarehouseDto createWarehouse(WarehouseUpsertRequest request) {
        String tenantId = TenantContext.getTenantId();
        if (warehouseRepository.existsByTenantIdAndCode(tenantId, request.getCode())) {
            throw new BadRequestException("Warehouse code '" + request.getCode() + "' already exists for this tenant");
        }

        Warehouse warehouse = Warehouse.builder()
                .name(request.getName())
                .code(request.getCode())
                .location(request.getLocation())
                .active(request.getActive() == null || request.getActive())
                .build();
        warehouse.setTenantId(tenantId);

        Warehouse saved = warehouseRepository.save(warehouse);
        return mapToDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public WarehouseDto getWarehouseById(Long id) {
        return mapToDto(findById(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<WarehouseDto> getAllWarehouses() {
        String tenantId = TenantContext.getTenantId();
        return warehouseRepository.findByTenantId(tenantId).stream()
                .map(this::mapToDto)
                .toList();
    }

    @Override
    @Transactional
    public WarehouseDto updateWarehouse(Long id, WarehouseUpsertRequest request) {
        Warehouse warehouse = findById(id);

        String tenantId = TenantContext.getTenantId();
        if (!warehouse.getCode().equalsIgnoreCase(request.getCode())
                && warehouseRepository.existsByTenantIdAndCode(tenantId, request.getCode())) {
            throw new BadRequestException("Warehouse code '" + request.getCode() + "' already exists for this tenant");
        }

        warehouse.setName(request.getName());
        warehouse.setCode(request.getCode());
        warehouse.setLocation(request.getLocation());
        if (request.getActive() != null) {
            warehouse.setActive(request.getActive());
        }

        return mapToDto(warehouseRepository.save(warehouse));
    }

    @Override
    @Transactional
    public void deleteWarehouse(Long id) {
        Warehouse warehouse = findById(id);
        warehouseRepository.delete(warehouse);
    }

    private Warehouse findById(Long id) {
        Warehouse warehouse = warehouseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse", "id", id));

        String tenantId = TenantContext.getTenantId();
        if (warehouse.getTenantId() != null && !warehouse.getTenantId().equals(tenantId)) {
            throw new ResourceNotFoundException("Warehouse", "id", id);
        }
        return warehouse;
    }

    private WarehouseDto mapToDto(Warehouse warehouse) {
        return WarehouseDto.builder()
                .id(warehouse.getId())
                .name(warehouse.getName())
                .code(warehouse.getCode())
                .location(warehouse.getLocation())
                .active(warehouse.isActive())
                .build();
    }
}
