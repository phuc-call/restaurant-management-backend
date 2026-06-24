package com.example.shop.service;

import com.example.shop.payloads.RoleDTO;
import com.example.shop.payloads.UserDTO;
import com.example.shop.payloads.reponse.UserResponse;

public interface UserService {
    UserDTO register(UserDTO userDTO);
    UserResponse getAllUser(Integer pageNumber,Integer pageSize,
                            String sortBy,String sorOrder);
    UserDTO getUserByEmail(String email);
    RoleDTO getRoleByEmail(String email);
//    UserDTO getUserById(Long userId);
//    UserDTO updateUser(Long userId,UserDTO userDTO);
//    UserDTO getUserById(Long userId,UserDTO userDTO);
    UserDTO uploadUser(Long userId,UserDTO userDTO);
    String delete (Long userId);
    UserDTO registerEmployee(UserDTO userDTO);
}

