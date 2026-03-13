package com.example.shop.service;

import com.example.shop.entity.RestaurantTable;
import com.example.shop.entity.enums.Floor;
import com.example.shop.entity.enums.TableStatus;
import com.example.shop.payloads.*;
import com.example.shop.payloads.reponse.RestaurantTableResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

public interface RestaurantTableService {
    RestaurantTableDTO createTable(RestaurantTableDTO restaurantTableDTO, List<MultipartFile> files);
    RestaurantTableResponse getAllRestaurantTableShow(
            Integer pageNumber,
            Integer pageSize,
            String sortBy,
            String sortOrder,
            TableStatus status,
            Floor floor);
    RestaurantTableDTO uploadRestaurantTable(Long tableId,RestaurantTableDTO restaurantTableDTO,List<MultipartFile>files);
    public String hideTable(Long tableId);
    RestaurantTableDTO ORDERING(Long tableId);
    String setStatusTableAVAILABLE(Long tableId);
    CartDTO enterTableByToken(String token);
    public List<FloorTablesResponse> getTablesByAllFloors();
    public String showTable(Long tableId);
    RestaurantTableResponse getAllRestaurantTableHide(
            Integer pageNumber,
            Integer pageSize,
            String sortBy,
            String sortOrder,

            Floor floor);
    public List<RestaurantTableDTO> createMultipleTables(
            CreateMultipleTableDTO dto, Floor floor

    );
    RestaurantTableDTO deleteTable(Long tableId);
    public RestaurantTableDTO updateTableInfo(
            Long tableId,
            UpdateRestaurantTableDTO dto
    );
    RestaurantTableDTO getTableById(Long tableId);

}
