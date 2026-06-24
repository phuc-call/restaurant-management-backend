package com.example.shop.service.impl;

import com.example.shop.config.AppConstants;
import com.example.shop.entity.Address;
import com.example.shop.entity.Position;
import com.example.shop.entity.Role;
import com.example.shop.entity.User;
import com.example.shop.exception.APIException;
import com.example.shop.exception.ResourceNotFoundException;
import com.example.shop.payloads.AddressDTO;
import com.example.shop.payloads.RoleDTO;
import com.example.shop.payloads.UserDTO;
import com.example.shop.payloads.reponse.UserResponse;
import com.example.shop.repository.AddressRepo;
import com.example.shop.repository.PositionRepo;
import com.example.shop.repository.RoleRepo;
import com.example.shop.repository.UserRepo;
import com.example.shop.security.JWTUtil;
import com.example.shop.service.UserService;
import com.example.shop.hellper.TextNormalizer;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UserServiceIpm implements UserService {
    @Autowired
    private UserRepo userRepo;
    @Autowired
    private ModelMapper modelMapper;
    @Autowired
    private RoleRepo roleRepo;
    @Autowired
    private AddressRepo addressRepo;
    @Autowired
    private PositionRepo positionRepo;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private JWTUtil jwtUtil;

    @Override
    public UserDTO register(UserDTO userDTO) {
        try {
            userDTO.setUserName(TextNormalizer.normalizeName(userDTO.getUserName()));
            User user = modelMapper.map(userDTO, User.class);
            user.setPassword(passwordEncoder.encode(userDTO.getPassword()));
            Role role = roleRepo.findById(AppConstants.USER_ID).orElseThrow(() -> new APIException("Not found role"));
            user.getRoles().add(role);
            User register = userRepo.save(user);
            return modelMapper.map(register, UserDTO.class);
        } catch (DataIntegrityViolationException e) {
            throw new APIException("User already have an account" + userDTO.getEmail());
        }
    }

    @Transactional(readOnly = true)
    @Override
    public UserResponse getAllUser(Integer pageNumber, Integer pageSize,
                                   String sortBy, String sorOrder) {
        Sort sortByAndSortOrder = sorOrder.equalsIgnoreCase("asc") ?
                Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();

        Pageable pageDetails = PageRequest.of(pageNumber, pageSize, sortByAndSortOrder);

        Page<User> pageUsers = userRepo.findAll(pageDetails);
        List<User> users = pageUsers.getContent();
        if (users.isEmpty()) {
            throw new APIException("User not exits");
        }
        List<UserDTO> userDTOS = users.stream().map(u -> {
            UserDTO dto = modelMapper.map(u, UserDTO.class);
            if (u.getAddresses()!=null&&!u.getAddresses().isEmpty()) {
                dto.setAddress(modelMapper.map(u.getAddresses().stream().findFirst().get(), AddressDTO.class));
            }
            return dto;
        }).toList();
        UserResponse userResponse = new UserResponse();
        userResponse.setContent(userDTOS);
        userResponse.setPageNumber(pageUsers.getNumber());
        userResponse.setPageSize(pageUsers.getSize());
        userResponse.setTotalElements(pageUsers.getTotalElements());
        userResponse.setTotalPage(pageUsers.getTotalPages());
        userResponse.setLastPage(pageUsers.isLast());
        return userResponse;
    }

    @Override
    public UserDTO registerEmployee(UserDTO userDTO) {
        userDTO.setUserName(TextNormalizer.normalizeName(userDTO.getUserName()));
        userDTO.setPassword(passwordEncoder.encode(userDTO.getPassword()));
        User user = modelMapper.map(userDTO, User.class);
        Role role = roleRepo.findById(AppConstants.EMPLOYEE).orElseThrow(() ->
                new APIException("Not found role"));

        Position position = positionRepo.findByName(userDTO.getPosition())
                .stream()
                .findFirst().orElseThrow(() -> new APIException("Not found position"));

        if (userDTO.getAddress() != null) {
            String city = userDTO.getAddress().getCity();
            String stress = userDTO.getAddress().getStress();
            String country = userDTO.getAddress().getCountry();
            String district = userDTO.getAddress().getDistrict();
            String ward = userDTO.getAddress().getWard();
            String note = TextNormalizer.normalizeDescriptionAndNotification(userDTO.getAddress().getNote());
            Address address = addressRepo.findByCityAndCountryAndStreetAndDistrictAndNoteAndWard(
                    city, stress, country, district, ward, note);
            if (address == null) {
                address = new Address(stress, ward, district, city, country, note);
                address = addressRepo.save(address);
            }
            user.setAddresses(new HashSet<>(List.of(address)));
        }
        user.getRoles().add(role);
        user.getPositions().add(position);
        user = userRepo.save(user);
        return modelMapper.map(user, UserDTO.class);
    }

    @Override
    public String delete(Long userId) {
        User user = userRepo.findById(userId).orElseThrow(() ->
                new ResourceNotFoundException("User", "user", userId));
        userRepo.delete(user);
        return "User name" + user.getUserName() + " delete success!!";
    }
    @Override
    public RoleDTO getRoleByEmail(String email){
        User userFromDb=userRepo.findUserWithAddresses(email).orElseThrow(()->
                new APIException("Not fount role with email!!"));
        Set<String>roleName=userFromDb.getRoles().stream().map(Role::getRoleName).
                collect(Collectors.toSet());//.collect(Collectors.toSet()) tạo ra Set<String>
        RoleDTO roleDTO=new RoleDTO();
        roleDTO.setRoleNames(roleName);
        return roleDTO;
    }

    @Override
    public UserDTO uploadUser(Long userId, UserDTO userDTO) {
        User userFromDB = userRepo.findById(userId)
                .orElseThrow(() -> new APIException("User not found"));
        if (userDTO.getPassword() != null && !userDTO.getPassword().isEmpty()) {
            userFromDB.setPassword(passwordEncoder.encode(userDTO.getPassword()));
        }
        if (userDTO.getUserName() != null && !userDTO.getUserName().isEmpty()) {
            userFromDB.setUserName(TextNormalizer.normalizeName(userDTO.getUserName()));
        }
        if (userDTO.getPhone() != null) {
            userFromDB.setPhone(userDTO.getPhone());
        }

        userFromDB = userRepo.save(userFromDB);
        return modelMapper.map(userFromDB, UserDTO.class);
    }

    // NOTE: Chú ý logic phần này
    @Override
    public UserDTO getUserByEmail(String email) {
        User user = userRepo.findByEmail(email).orElseThrow(() -> new APIException("User not fount with " + email));
        UserDTO userDTO = modelMapper.map(user, UserDTO.class);
        // MAP ROLE → roleNames
        userDTO.setRoleNames(user.getRoles().stream().map(Role::getRoleName).collect(Collectors.toSet()));
        // MAP ADDRESS
        if (user.getAddresses() != null && !user.getAddresses().isEmpty()) {
            userDTO.setAddress(
                    modelMapper.map(
                            user.getAddresses().iterator().next(),
                            AddressDTO.class
                    )
            );
        }
        // MAP POSITION (nếu bạn chỉ muốn 1 vị trí)
        if (user.getPositions() != null && !user.getPositions().isEmpty()) {
            userDTO.setPosition(
                    user.getPositions().iterator().next().getName()
            );
        }
        return userDTO;
    }
}
