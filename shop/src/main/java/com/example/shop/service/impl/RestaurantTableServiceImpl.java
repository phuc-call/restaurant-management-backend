package com.example.shop.service.impl;

import com.example.shop.entity.Cart;
import com.example.shop.entity.Image;
import com.example.shop.entity.RestaurantTable;
import com.example.shop.entity.TableType;
import com.example.shop.entity.enums.ECartStatus;
import com.example.shop.entity.enums.TableStatus;
import com.example.shop.exception.APIException;
import com.example.shop.payloads.CartDTO;
import com.example.shop.payloads.CartItemDTO;
import com.example.shop.payloads.RestaurantTableDTO;
import com.example.shop.payloads.reponse.RestaurantTableResponse;
import com.example.shop.repository.CartRepo;
import com.example.shop.repository.ImageRepo;
import com.example.shop.repository.RestaurantRepo;
import com.example.shop.repository.TableTypeRepo;
import com.example.shop.service.RestaurantTableService;
import com.example.shop.hellper.TextNormalizer;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


@Service
@Transactional
public class RestaurantTableServiceImpl implements RestaurantTableService {
    @Autowired
    RestaurantRepo restaurantRepo;
    @Autowired
    FileStorageService fileStorageService;
    @Autowired
    ImageRepo imageRepo;
    @Autowired
    ModelMapper modelMapper;
    @Autowired
    TableTypeRepo tableTypeRepo;
    @Autowired
    CartRepo cartRepo;
    @Autowired
    QRService qrService;


    @Override
    public RestaurantTableDTO createTable(RestaurantTableDTO dto, List<MultipartFile> files) {
        TableType tableType =tableTypeRepo.findById(dto.getTableTypeId()).orElseThrow(()->
                new APIException("Type table not exist!!"));
        //  Check name table and number, typeTable
        if (restaurantRepo.findByTableName(dto.getTableName()).isPresent()) {
            throw new APIException("Table name already exists!");
        }
        if (restaurantRepo.findByNumberTable(dto.getNumberTable()).isPresent()) {
            throw new APIException("Table number already exists!");
        }

        // 3. Mapping DTO -> Entity
        RestaurantTable table = new RestaurantTable();
        table.setTableName(dto.getTableName());
        table.setAccessToken(UUID.randomUUID().toString());
        table.setNumberTable(dto.getNumberTable());
        table.setTableType(tableType);
        table.setSeatCount(dto.getSeatCount());
        RestaurantTable saved = restaurantRepo.save(table);
        //QR
        String baseUrl = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();
//        String baseUrl = "http://10.0.15.237:8080"; // IP LAN
        String qrContent = "http://localhost:3000/menu?token=" + saved.getAccessToken();

        saved.setQrUrl(qrContent);

        byte[] qrBytes = qrService.generateQRCode(qrContent, 300, 300);
        saved.setQrImage(qrBytes);
        restaurantRepo.save(saved);

        //  Upload images
        if (files != null && !files.isEmpty()) {
            List<Image> images = new ArrayList<>();
            for (MultipartFile file : files) {
                String url = fileStorageService.save(file);
                Image image = new Image();
                image.setImageUrl(url);
                image.setAltText(saved.getTableName());
                image.setRestaurantTable(saved);
                images.add(image);
            }
            imageRepo.saveAll(images);
        }
        Cart cart=new Cart();
        cart.setId(table.getTableId());
        cart.setStatus(ECartStatus.CLOSED);
        cart.setRestaurantTable(saved);//save keyMain of table
        cart.setTotalPrice(BigDecimal.ZERO);
        cartRepo.save(cart);
         return modelMapper.map(saved, RestaurantTableDTO.class);
    }


    @Override
    public RestaurantTableResponse getAllRestaurantTable(
            Integer pageNumber,
            Integer pageSize,
            String sortBy,
            String sortOrder,
            TableStatus status) {

        Sort sort = sortOrder.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(pageNumber, pageSize, sort);

        Page<RestaurantTable> page = restaurantRepo.filter(status, pageable);

        List<RestaurantTableDTO> tableDTOs = page.getContent()
                .stream()
                .map(table -> modelMapper.map(table, RestaurantTableDTO.class))
                .toList();

        RestaurantTableResponse response = new RestaurantTableResponse();
        response.setContents(tableDTOs);
        response.setPageNumber(page.getNumber());
        response.setPageSize(page.getSize());
        response.setTotalPage(page.getTotalPages());
        response.setElementTotalPage(page.getTotalElements());

        return response;
    }


    @Override
    public RestaurantTableDTO uploadRestaurantTable(Long tableId, RestaurantTableDTO restaurantTableDTO,List<MultipartFile>files) {
        RestaurantTable restaurantTable= restaurantRepo.findById(tableId).orElseThrow(()->
                new APIException("No found table to update!"));
        TableType tableType=tableTypeRepo.findById(restaurantTableDTO.getTableTypeId()).orElseThrow(()->
                new APIException("Not found type table!!"));
        restaurantTable.setTableName(TextNormalizer.normalizeName(restaurantTableDTO.getTableName()));
        restaurantTable.setNumberTable(restaurantTable.getNumberTable());
        restaurantTable.setTableType(tableType);
        restaurantTable.setSeatCount(restaurantTable.getSeatCount());

        if (files!=null&&!files.isEmpty()) {
            List<Image> newImages = new ArrayList<>();
            for (MultipartFile file : files) {
                String url = fileStorageService.save(file);  //  save file
                Image image = new Image();
                image.setImageUrl(url); //  Save change
                image.setRestaurantTable(restaurantTable);
                image.setAltText(restaurantTableDTO.getTableName());
                newImages.add(image);
            }
            restaurantTable.setImages(newImages);
        }
        RestaurantTable restaurantTableSave= restaurantRepo.save(restaurantTable);
        return modelMapper.map(restaurantTableSave,RestaurantTableDTO.class);
    }

    @Override
    public String delete(Long tableId) {
        RestaurantTable restaurantTable= restaurantRepo.findById(tableId).orElseThrow(()->
                new APIException("This table not exist!!"));
        restaurantRepo.deleteById(restaurantTable.getTableId());
        return "Delete "+restaurantTable.getTableName()+" success!!";
    }

    //Option 2// customer order QR
    @Override
    public RestaurantTableDTO ORDERING(Long tableId){
        RestaurantTable restaurantTable= restaurantRepo.findById(tableId).orElseThrow(()->
                new APIException("Table not found"));
        Cart cart=cartRepo.findByRestaurantTable_tableId(tableId).orElseThrow(()->
                new APIException("Cart not found for this table"));
        if (restaurantTable.getStatus() == TableStatus.AVAILABLE) {
            //change status
            restaurantTable.setStatus(TableStatus.ORDERING);
            restaurantRepo.save(restaurantTable);
            cart.setStatus(ECartStatus.ACTIVE);
            cartRepo.save(cart);
        }
        return modelMapper.map(restaurantTable,RestaurantTableDTO.class);
    }
    @Override
    @Transactional
    public CartDTO enterTableByToken(String token) {

        // 1. Tìm table theo token
        RestaurantTable table = restaurantRepo.findByAccessToken(token)
                .orElseThrow(() -> new APIException("Invalid QR Code"));

        Long tableId = table.getTableId();

        // 2. Tìm cart theo tableId
        Cart cart = cartRepo.findByRestaurantTable_tableId(tableId)
                .orElseThrow(() -> new APIException("Cart not found"));

        // 3. GỘP LOGIC ORDERING
        if (table.getStatus() == TableStatus.AVAILABLE) {
            table.setStatus(TableStatus.ORDERING);
            restaurantRepo.save(table);

            cart.setStatus(ECartStatus.ACTIVE);
            cartRepo.save(cart);
        }

        // 4. Build CartDTO trả về
        CartDTO dto = new CartDTO();
        dto.setId(cart.getId());
        dto.setTableId(tableId);
        dto.setRestaurantTable(table.getTableName());
        dto.setStatus(cart.getStatus().name());
        dto.setTotalPrice(cart.getTotalPrice());

        dto.setCartItems(
                cart.getCartItems().stream().map(item -> {
                    CartItemDTO ci = new CartItemDTO();
                    ci.setId(item.getId());
                    ci.setMenuItemId(item.getMenuItem().getId());
                    ci.setMenuItemName(item.getMenuItem().getName());
                    ci.setUnitPrice(item.getUnitPrice());
                    ci.setQuantity(item.getQuantity());
                    return ci;
                }).toList()
        );

        return dto;
    }

    @Override
    public String setStatusTableAVAILABLE(Long tableId) {
        RestaurantTable restaurantTable = restaurantRepo.findById(tableId).orElseThrow(() ->
                new APIException("This table not exist!!"));
            restaurantTable.setStatus(TableStatus.AVAILABLE);
        return "Change status success.";
    }
}
