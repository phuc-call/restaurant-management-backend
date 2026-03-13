package com.example.shop.controller;

import com.example.shop.config.AppConstants;
import com.example.shop.payloads.UpdateAccountStatusDTO;
import com.example.shop.payloads.UpdateEmployeeDTO;
import com.example.shop.payloads.UserDTO;
import com.example.shop.payloads.reponse.GetEmployeesDTO;
import com.example.shop.service.UserService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api")
@SecurityRequirement(name = "E-commerce Application")
public class EmployeeController {
    @Autowired
    UserService userService;
    @Autowired
    ObjectMapper objectMapper;
    @PostMapping(
            value = "/admin/employees",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<UserDTO> createEmployee(
            @RequestPart("user") String userJson,
            @RequestPart(value = "files", required = false) List<MultipartFile> files
    ) throws JsonProcessingException {

        UserDTO userDTO =
                objectMapper.readValue(userJson, UserDTO.class);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(userService.registerEmployee(userDTO, files));
    }


    @GetMapping("/admin/employees")
    public ResponseEntity<Page<GetEmployeesDTO>>getAllEmployeeByRole(
            @RequestParam(name = "pageNumber",defaultValue = AppConstants.PAGE_NUMBER)Integer pageNumber,
            @RequestParam(name = "PageSize",defaultValue = "15")Integer pageSize,
            @RequestParam(name = "sortBy", defaultValue = AppConstants.SORT_MENU_BY, required = false) String sortBy,
            @RequestParam(name = "order", defaultValue = AppConstants.SORT_DIR, required = false) String order,
            @RequestParam(required = false) String roleName) {
        Page<GetEmployeesDTO> page=userService.getEmployee(pageNumber,pageSize,sortBy,order,roleName);
        return ResponseEntity.ok(page);
    }
    @GetMapping("/admin/employees/{userId}")
    public ResponseEntity<UserDTO>getDetailEmployee(
            @PathVariable Long userId
    ){
        UserDTO userDTO=userService.getDetailsEmployee(userId);
        return ResponseEntity.ok(userDTO);
    }

    @PutMapping(
            value = "/admin/employees/{userId}",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<UserDTO> updateEmployee(
            @PathVariable Long userId,

            @RequestPart("dto") UpdateEmployeeDTO dto,

            @RequestPart(value = "file", required = false) MultipartFile file
    ) {
        UserDTO result = userService.updateEmployee(userId, dto, file);
        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/employees/{id}")
    public ResponseEntity<String> deleteEmployee(@PathVariable Long id) {
        userService.deleteUserById(id);
        return ResponseEntity.ok("Xóa nhân viên thành công");
    }

    @PutMapping("/admin/users/{id}/status")
    public ResponseEntity<?> updateStatus(
            @PathVariable Long id,
            @RequestBody UpdateAccountStatusDTO dto
    ) {
        userService.updateAccountStatus(id, dto.getStatus());
        return ResponseEntity.ok("Cập nhật trạng thái thành công");
    }

}