package com.example.shop.service.impl;

import com.example.shop.entity.*;
import com.example.shop.entity.enums.BookingStatus;
import com.example.shop.entity.enums.ECartStatus;
import com.example.shop.entity.enums.Floor;
import com.example.shop.entity.enums.TableStatus;
import com.example.shop.exception.APIException;
import com.example.shop.payloads.*;
import com.example.shop.payloads.reponse.RestaurantTableResponse;
import com.example.shop.repository.*;
import com.example.shop.service.RestaurantTableService;
import com.example.shop.hellper.TextNormalizer;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;


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
    @Autowired
    TableBookingRepo tableBookingRepo;

    @Autowired
    SimpMessagingTemplate messagingTemplate;

    @Override
    public RestaurantTableDTO createTable(RestaurantTableDTO dto, List<MultipartFile> files) {
        TableType tableType = tableTypeRepo.findById(dto.getTableTypeId()).orElseThrow(() ->
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
        table.setFloor(dto.getFloops());
        table.setTableType(tableType);
        table.setSeatCount(dto.getSeatCount());
        RestaurantTable saved = restaurantRepo.save(table);
        //QR
        String baseUrl = ServletUriComponentsBuilder
                .fromCurrentContextPath()
                .build()
                .toUriString();

        String qrContent = baseUrl + "/menu?token=" + saved.getAccessToken();


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
        Cart cart = new Cart();
        cart.setId(table.getTableId());
        cart.setStatus(ECartStatus.CLOSED);
        cart.setRestaurantTable(saved);//save keyMain of table
        cart.setTotalPrice(BigDecimal.ZERO);
        cartRepo.save(cart);
        return modelMapper.map(saved, RestaurantTableDTO.class);
    }


    @Override
    public RestaurantTableResponse getAllRestaurantTableShow(
            Integer pageNumber,
            Integer pageSize,
            String sortBy,
            String sortOrder,
            TableStatus status,
            Floor floor) {

        Sort sort = sortOrder.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(pageNumber, pageSize, sort);

        Page<RestaurantTable> page = restaurantRepo.filter(status, floor, pageable);

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
    public RestaurantTableDTO uploadRestaurantTable(Long tableId, RestaurantTableDTO restaurantTableDTO, List<MultipartFile> files) {
        RestaurantTable restaurantTable = restaurantRepo.findById(tableId).orElseThrow(() ->
                new APIException("No found table to update!"));
        TableType tableType = tableTypeRepo.findById(restaurantTableDTO.getTableTypeId()).orElseThrow(() ->
                new APIException("Not found type table!!"));
        restaurantTable.setTableName(TextNormalizer.normalizeName(restaurantTableDTO.getTableName()));
        restaurantTable.setNumberTable(restaurantTable.getNumberTable());
        restaurantTable.setFloor(restaurantTable.getFloor());
        restaurantTable.setTableType(tableType);
        restaurantTable.setSeatCount(restaurantTable.getSeatCount());

        if (files != null && !files.isEmpty()) {
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
        RestaurantTable restaurantTableSave = restaurantRepo.save(restaurantTable);
        return modelMapper.map(restaurantTableSave, RestaurantTableDTO.class);
    }

    @Override
    public String hideTable(Long tableId) {

        RestaurantTable table = restaurantRepo.findById(tableId)
                .orElseThrow(() -> new APIException("Table not found"));

        // 1. Bàn phải trống
        if (table.getStatus() != TableStatus.AVAILABLE) {
            throw new APIException("Chỉ được ẩn bàn đang trống");
        }

        // 2. Không có booking đang hoạt động (defensive)
        boolean hasActiveBooking =
                tableBookingRepo.existsByTable_TableIdAndStatusIn(
                        tableId,
                        List.of(
                                BookingStatus.PENDING,
                                BookingStatus.CONFIRMED,
                                BookingStatus.CHECKED_IN
                        )
                );

        if (hasActiveBooking) {
            throw new APIException("Bàn đang có lịch đặt, không thể ẩn");
        }

        // 3. Ẩn bàn
        table.setStatus(TableStatus.INACTIVE);
        restaurantRepo.save(table);

        // 4. REALTIME
        messagingTemplate.convertAndSend(
                "/topic/booking",
                Map.of(
                        "event", "TABLE_HIDDEN",
                        "data", Map.of(
                                "tableId", table.getTableId(),
                                "tableName", table.getTableName(),
                                "status", table.getStatus().name()
                        )
                )
        );

        return "Hide table success";
    }


    @Override
    public String showTable(Long tableId) {

        RestaurantTable table = restaurantRepo.findById(tableId)
                .orElseThrow(() -> new APIException("Table not found"));

        // Chỉ mở lại bàn đang ẩn
        if (table.getStatus() != TableStatus.INACTIVE) {
            throw new APIException("Chỉ có thể mở lại bàn đang bị ẩn");
        }

        // Defensive check (có thể giữ hoặc bỏ)
        boolean hasActiveBooking =
                tableBookingRepo.existsByTable_TableIdAndStatusIn(
                        tableId,
                        List.of(
                                BookingStatus.PENDING,
                                BookingStatus.CONFIRMED,
                                BookingStatus.CHECKED_IN
                        )
                );

        if (hasActiveBooking) {
            throw new APIException("Bàn đang có lịch đặt, không thể mở");
        }

        // Bỏ ẩn
        table.setStatus(TableStatus.AVAILABLE);
        restaurantRepo.save(table);

        // REALTIME
        messagingTemplate.convertAndSend(
                "/topic/booking",
                Map.of(
                        "event", "TABLE_SHOWN",
                        "data", Map.of(
                                "tableId", table.getTableId(),
                                "tableName", table.getTableName(),
                                "status", table.getStatus().name()
                        )
                )
        );

        return "Show table success";
    }


    @Override
    public RestaurantTableDTO ORDERING(Long tableId) {
        RestaurantTable restaurantTable = restaurantRepo.findById(tableId).orElseThrow(() ->
                new APIException("Table not found"));
        Cart cart = cartRepo.findByRestaurantTable_tableId(tableId).orElseThrow(() ->
                new APIException("Cart not found for this table"));
        if (restaurantTable.getStatus() == TableStatus.AVAILABLE) {
            //change status
            restaurantTable.setStatus(TableStatus.ORDERING);
            restaurantRepo.save(restaurantTable);
            cart.setStatus(ECartStatus.ACTIVE);
            cartRepo.save(cart);
        }
        return modelMapper.map(restaurantTable, RestaurantTableDTO.class);
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
        if (cart.getStatus() == ECartStatus.CLOSED) {
            cart.setStatus(ECartStatus.ACTIVE);
            cartRepo.save(cart);
        }
        if (table.getStatus() == TableStatus.INACTIVE) {
            throw new APIException("Bàn đã ngưng sử dụng");
        }


        if (table.getStatus() == TableStatus.AVAILABLE) {
            table.setStatus(TableStatus.ORDERING);
            restaurantRepo.save(table);
        }

        // 4. Build CartDTO trả về
        CartDTO dto = new CartDTO();
        dto.setCartId(cart.getId());
        dto.setTableId(tableId);
        dto.setRestaurantTable(table.getTableName());
        dto.setStatus(cart.getStatus().name());
        dto.setTotalPrice(cart.getTotalPrice());

        dto.setItems(
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

    public Map<Floor, List<String>> getAllTableNamesGroupedByFloor() {
        return restaurantRepo.findAllTableNamesWithFloor()
                .stream()
                .collect(Collectors.groupingBy(
                        row -> (Floor) row[0],
                        Collectors.mapping(row -> (String) row[1], Collectors.toList())
                ));
    }

    @Override
    public List<FloorTablesResponse> getTablesByAllFloors() {

        List<FloorTablesResponse> result = new ArrayList<>();

        for (Floor floor : Floor.values()) {

            List<String> tables =
                    restaurantRepo.findTableNameByFloor(floor);

            if (tables.isEmpty()) {
                result.add(new FloorTablesResponse(
                        floor,
                        List.of(),
                        "Tầng này chưa có bàn"
                ));
            } else {
                result.add(new FloorTablesResponse(
                        floor,
                        tables,
                        null
                ));
            }
        }

        return result;
    }
    @Override
    public RestaurantTableResponse getAllRestaurantTableHide(
            Integer pageNumber,
            Integer pageSize,
            String sortBy,
            String sortOrder,

            Floor floor){
        Sort sort = sortOrder.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(pageNumber, pageSize, sort);

        Page<RestaurantTable> page = restaurantRepo.filterInactive( floor, pageable);

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
    public List<RestaurantTableDTO> createMultipleTables(
            CreateMultipleTableDTO dto,
            Floor floor

    ) {
        TableType tableType = tableTypeRepo.findById(dto.getTableTypeId())
                .orElseThrow(() -> new APIException("Type table not exist"));

        int quantity = dto.getQuantity();
        if(quantity>=10){
            throw new APIException("chỉ được tạo một lần nhỏ hơn 10 bàn");
        }
        Long startNumber = dto.getStartNumber();

        List<RestaurantTable> tablesToSave = new ArrayList<>();


        for (int i = 0; i < quantity; i++) {
            Long number = startNumber + i;
            String tableName = dto.getTableNamePrefix() + " " + number;

            if (restaurantRepo.findByNumberTable(number).isPresent()) {
                throw new APIException("Table number already exists: " + number);
            }
            if (restaurantRepo.findByTableName(tableName).isPresent()) {
                throw new APIException("Table name already exists: " + tableName);
            }
        }


        for (int i = 0; i < quantity; i++) {
            Long number = startNumber + i;
            String tableName = dto.getTableNamePrefix() + " " + number;

            RestaurantTable table = new RestaurantTable();
            table.setTableName(tableName);
            table.setNumberTable(number);
            table.setSeatCount(dto.getSeatCount());
            table.setFloor(floor);
            table.setTableType(tableType);
            table.setAccessToken(UUID.randomUUID().toString());

            tablesToSave.add(table);
        }

        restaurantRepo.saveAll(tablesToSave);


        String baseUrl = ServletUriComponentsBuilder
                .fromCurrentContextPath()
                .build()
                .toUriString();

        for (RestaurantTable table : tablesToSave) {
            String qrContent = baseUrl + "/menu?token=" + table.getAccessToken();
            table.setQrUrl(qrContent);
            table.setQrImage(qrService.generateQRCode(qrContent, 300, 300));

            Cart cart = new Cart();
            cart.setId(table.getTableId());
            cart.setRestaurantTable(table);
            cart.setStatus(ECartStatus.CLOSED);
            cart.setTotalPrice(BigDecimal.ZERO);
            cartRepo.save(cart);
        }

        restaurantRepo.saveAll(tablesToSave);

        return tablesToSave.stream()
                .map(t -> modelMapper.map(t, RestaurantTableDTO.class))
                .toList();
    }
    @Override
    public RestaurantTableDTO deleteTable(Long tableId) {

        RestaurantTable table = restaurantRepo.findById(tableId)
                .orElseThrow(() -> new APIException("Bàn không tồn tại"));

        if (table.getStatus() == TableStatus.ORDERING
                || table.getStatus() == TableStatus.OCCUPIED) {
            throw new APIException("Bàn đang có khách, không thể xóa");
        }


        boolean hasActiveBooking =
                tableBookingRepo.existsByTable_TableIdAndStatusIn(
                        tableId,
                        List.of(
                                BookingStatus.PENDING,
                                BookingStatus.CONFIRMED,
                                BookingStatus.CHECKED_IN
                        )
                );

        if (hasActiveBooking) {
            throw new APIException("Bàn đang có lịch đặt, không thể xóa");
        }

        Cart cart = cartRepo.findByRestaurantTable_tableId(tableId)
                .orElse(null);

        if (cart != null) {

            if (!cart.getCartItems().isEmpty()) {
                throw new APIException("Bàn đang có món trong giỏ, không thể xóa");
            }

            cartRepo.delete(cart);
        }

        // 4️⃣ Xóa bàn
        restaurantRepo.delete(table);

        // 5️⃣ Realtime
        messagingTemplate.convertAndSend(
                "/topic/booking",
                Map.of(
                        "event", "TABLE_DELETED",
                        "data", Map.of(
                                "tableId", tableId,
                                "tableName", table.getTableName()
                        )
                )
        );

        return modelMapper.map(table, RestaurantTableDTO.class);
    }
    @Override
    @Transactional
    public RestaurantTableDTO updateTableInfo(
            Long tableId,
            UpdateRestaurantTableDTO dto
    ) {

        RestaurantTable table = restaurantRepo.findById(tableId)
                .orElseThrow(() -> new APIException("Bàn không tồn tại"));


        if (table.getStatus() == TableStatus.ORDERING
                || table.getStatus() == TableStatus.OCCUPIED) {
            throw new APIException("Bàn đang có khách, không thể chỉnh sửa");
        }


        boolean hasActiveBooking =
                tableBookingRepo.existsByTable_TableIdAndStatusIn(
                        tableId,
                        List.of(
                                BookingStatus.PENDING,
                                BookingStatus.CONFIRMED,
                                BookingStatus.CHECKED_IN
                        )
                );

        if (hasActiveBooking) {
            throw new APIException("Bàn đang có lịch đặt, không thể chỉnh sửa");
        }


        if (dto.getNumberTable() != null
                && !dto.getNumberTable().equals(table.getNumberTable())
                && restaurantRepo.findByNumberTable(dto.getNumberTable()).isPresent()) {
            throw new APIException("Số bàn đã tồn tại");
        }


        if (dto.getTableName() != null
                && !dto.getTableName().equalsIgnoreCase(table.getTableName())
                && restaurantRepo.findByTableName(dto.getTableName()).isPresent()) {
            throw new APIException("Tên bàn đã tồn tại");
        }


        if (dto.getTableTypeId() != null) {
            TableType tableType = tableTypeRepo.findById(dto.getTableTypeId())
                    .orElseThrow(() -> new APIException("Loại bàn không tồn tại"));
            table.setTableType(tableType);
        }


        if (dto.getTableName() != null) {
            table.setTableName(dto.getTableName());
        }
        if (dto.getNumberTable() != null) {
            table.setNumberTable(dto.getNumberTable());
        }
        if (dto.getSeatCount() != null) {
            table.setSeatCount(dto.getSeatCount());
        }
        if (dto.getFloor() != null) {
            table.setFloor(dto.getFloor());
        }

        restaurantRepo.save(table);


        messagingTemplate.convertAndSend(
                "/topic/booking",
                Map.of(
                        "event", "TABLE_UPDATED",
                        "data", Map.of(
                                "tableId", table.getTableId(),
                                "tableName", table.getTableName(),
                                "numberTable", table.getNumberTable(),
                                "seatCount", table.getSeatCount(),
                                "floor", table.getFloor(),
                                "tableTypeId", table.getTableType().getId(),
                                "status", table.getStatus().name()
                        )
                )
        );


        return modelMapper.map(table, RestaurantTableDTO.class);
    }
    @Override
    public RestaurantTableDTO getTableById(Long tableId) {
        RestaurantTable table = restaurantRepo.findById(tableId)
                .orElseThrow(() -> new APIException("Không tìm thấy bàn"));

        return modelMapper.map(table, RestaurantTableDTO.class);
    }
}
