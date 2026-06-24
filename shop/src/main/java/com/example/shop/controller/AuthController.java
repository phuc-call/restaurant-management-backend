package com.example.shop.controller;

import com.example.shop.payloads.UserDTO;
import com.example.shop.payloads.reponse.LoginCredentials;
import com.example.shop.security.JWTUtil;
import com.example.shop.service.UserService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

@RestController
@RequestMapping("/api")
@SecurityRequirement(name = "E-commerce Application")

public class AuthController {
    @Autowired
    private UserService userService;
    @Autowired
    private JWTUtil jwtUtil;
    @Autowired
    private AuthenticationManager authenticationManager;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @PostMapping("/public/login")
    public ResponseEntity<Map<String, Object>>loginHandle(
            @Valid @RequestBody LoginCredentials credentials) {
        // Tạo đối tượng Authentication chứa email + password
            UsernamePasswordAuthenticationToken authRequest =
                    new UsernamePasswordAuthenticationToken(
                            credentials.getEmail(),
                            credentials.getPassword()
                    );
        // Xác thực tài khoản
        authenticationManager.authenticate(authRequest);
        // Tạo JWT token
        String token = jwtUtil.generaToken(credentials.getEmail());
        // Chuẩn bị trả về dạng JSON
        Map<String, Object> response = new HashMap<>();
        response.put("status",true);
        response.put("message","Login success");
        response.put("jwt-token", token);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/public/register")
    public ResponseEntity<Map<String,Object>>register(@Valid @RequestBody UserDTO user) throws UsernameNotFoundException{
        UserDTO userDTO=userService.register(user);
        String token=jwtUtil.generaToken(userDTO.getEmail());
        return new ResponseEntity<>(Collections.singletonMap("jwt-token",token), HttpStatus.CREATED);
    }
}