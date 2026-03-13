package com.example.shop.controller;

import com.example.shop.config.AppConstants;
import com.example.shop.payloads.MenuItemDTO;
import com.example.shop.payloads.UpdateEmployeeDTO;
import com.example.shop.payloads.UserDTO;
import com.example.shop.payloads.reponse.UserResponse;
import com.example.shop.security.JWTUtil;
import com.example.shop.service.UserService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;


@RestController
@RequestMapping("/api")
@SecurityRequirement(name = "E-commerce Application")
public class UserController {
    @Autowired
    private UserService userService;
    @Autowired
    private JWTUtil jwtUtil;

    @GetMapping("admin/users")
    public ResponseEntity<UserResponse> getUsers(
            @RequestParam(name = "pageNumber", defaultValue = AppConstants.PAGE_NUMBER, required = false) Integer pageNumber,
            @RequestParam(name = "pageSize", defaultValue = AppConstants.PAGE_SIZE, required = false) Integer pageSize,
            @RequestParam(name = "sortBy", defaultValue = AppConstants.SORT_USER_BY, required = false) String sortBy,
            @RequestParam(name = "order", defaultValue = AppConstants.SORT_DIR, required = false) String order) {
        UserResponse userResponse = userService.getAllUser(pageNumber, pageSize, sortBy, order);
        return new ResponseEntity<>(userResponse, HttpStatus.FOUND);
    }

    @DeleteMapping("public/users/{userId}")
    public ResponseEntity<String> deleted(@PathVariable Long userId) {
        String status = userService.delete(userId);
        return new ResponseEntity<String>(status, HttpStatus.NOT_FOUND);
    }

    @PutMapping("public/users/{userId}")
    public ResponseEntity<UserDTO> update(@PathVariable Long userId, @RequestBody UserDTO userDTO) {
        UserDTO update = userService.uploadUser(userId, userDTO);
        return new ResponseEntity<>(update, HttpStatus.OK);
    }
    @GetMapping("/auth/me")
    public ResponseEntity<UserDTO>getCurrentUser(@RequestHeader("Authorization") String authHeader){
        String token=authHeader.substring(7);
        String email=jwtUtil.validateTokenAndRetrieveSubject(token);
        UserDTO userDTO=userService.getUserByEmail(email);
        return ResponseEntity.ok(userDTO);
    }



}
