package com.msmeerp.inventory.service;

import com.msmeerp.inventory.dto.WarehouseDto;
import com.msmeerp.inventory.dto.WarehouseUpsertRequest;

import java.util.List;

public interface WarehouseService {
    WarehouseDto createWarehouse(WarehouseUpsertRequest request);
    WarehouseDto getWarehouseById(Long id);
    List<WarehouseDto> getAllWarehouses();
    WarehouseDto updateWarehouse(Long id, WarehouseUpsertRequest request);
    void deleteWarehouse(Long id);
}
