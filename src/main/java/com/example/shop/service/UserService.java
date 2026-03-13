package com.example.shop.service;

import com.example.shop.entity.User;
import com.example.shop.entity.enums.EAccountStatus;
import com.example.shop.payloads.RoleDTO;
import com.example.shop.payloads.UpdateEmployeeDTO;
import com.example.shop.payloads.UserDTO;
import com.example.shop.payloads.reponse.GetEmployeesDTO;
import com.example.shop.payloads.reponse.UserResponse;
import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

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
    UserDTO registerEmployee(UserDTO userDTO, List<MultipartFile> files);
    Page<GetEmployeesDTO> getEmployee(
            Integer pageNumber,
            Integer pageSize,
            String sortBy,
            String sortOrder,
            String roleName
    ) ;
    UserDTO getDetailsEmployee(Long userId);
    UserDTO updateEmployee(
            Long userId,
            UpdateEmployeeDTO dto,
            MultipartFile file
    );
    UserDTO deleteUserById(Long userId);
    public void updateAccountStatus(Long userId, EAccountStatus status);
}

