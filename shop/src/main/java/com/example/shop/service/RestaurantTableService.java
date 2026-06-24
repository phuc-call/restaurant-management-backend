package com.example.shop.service;

import com.example.shop.entity.RestaurantTable;
import com.example.shop.entity.enums.TableStatus;
import com.example.shop.payloads.CartDTO;
import com.example.shop.payloads.RestaurantTableDTO;
import com.example.shop.payloads.reponse.RestaurantTableResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface RestaurantTableService {
    RestaurantTableDTO createTable(RestaurantTableDTO restaurantTableDTO, List<MultipartFile> files);
    RestaurantTableResponse getAllRestaurantTable(Integer pageNumber, Integer pageSize,
                                       String sortBy, String sorOrder, TableStatus status);
    RestaurantTableDTO uploadRestaurantTable(Long tableId,RestaurantTableDTO restaurantTableDTO,List<MultipartFile>files);
    String delete (Long tableId);
    RestaurantTableDTO ORDERING(Long tableId);
    String setStatusTableAVAILABLE(Long tableId);
    CartDTO enterTableByToken(String token);


}
