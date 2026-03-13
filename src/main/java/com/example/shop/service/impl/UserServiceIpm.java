package com.example.shop.service.impl;

import com.example.shop.config.AppConstants;
import com.example.shop.entity.*;
import com.example.shop.entity.enums.EAccountStatus;
import com.example.shop.entity.enums.EPositionType;
import com.example.shop.exception.APIException;
import com.example.shop.exception.ResourceNotFoundException;
import com.example.shop.hellper.SecuritySnapshotUtil;
import com.example.shop.payloads.*;
import com.example.shop.payloads.reponse.GetEmployeesDTO;
import com.example.shop.payloads.reponse.UserResponse;
import com.example.shop.repository.*;
import com.example.shop.security.JWTUtil;
import com.example.shop.service.UserService;
import com.example.shop.hellper.TextNormalizer;

import org.apache.xmlbeans.impl.xb.xsdschema.Attribute;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;


import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
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
    @Autowired
    SimpMessagingTemplate messagingTemplate;
    @Autowired
    FileStorageService fileStorageService;
    @Autowired
    private ImageRepo imageRepo;

    @Override

    public UserDTO register(UserDTO userDTO) {

        // 1. Validate userName
        if (userDTO.getUserName() == null || userDTO.getUserName().trim().isEmpty()) {
            throw new APIException("Họ tên không được để trống");
        }

        if (!userDTO.getUserName().matches("^[\\p{L}]+(\\s+[\\p{L}]+)+$")) {
            throw new APIException(
                    "Họ tên phải có ít nhất 2 từ, không chứa số hoặc ký tự đặc biệt"
            );
        }

        // 2. Validate email
        if (userDTO.getEmail() == null || userDTO.getEmail().trim().isEmpty()) {
            throw new APIException("Email không được để trống");
        }

        if (!userDTO.getEmail().matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
            throw new APIException("Email không hợp lệ");
        }

        // 3. Validate password THÔ
        String rawPassword = userDTO.getPassword();

        if (rawPassword == null || rawPassword.trim().isEmpty()) {
            throw new APIException("Mật khẩu không được để trống");
        }

        if (!rawPassword.matches(
                "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&]).{8,}$"
        )) {
            throw new APIException(
                    "Mật khẩu phải ≥ 8 ký tự, gồm chữ hoa, chữ thường, số và ký tự đặc biệt"
            );
        }

        // 4. Map + encode
        User user = modelMapper.map(userDTO, User.class);

        user.setPassword(passwordEncoder.encode(rawPassword));

        Role role = roleRepo.findById(AppConstants.USER_ID)
                .orElseThrow(() -> new APIException("Not found role"));
        user.getRoles().add(role);

        User saved = userRepo.save(user);
        return modelMapper.map(saved, UserDTO.class);
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
            if (u.getAddresses() != null && !u.getAddresses().isEmpty()) {
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
    public UserDTO registerEmployee(UserDTO userDTO, List<MultipartFile> files) {

        if (userRepo.findByEmail(userDTO.getEmail()).isPresent()) {
            throw new APIException("Email đã được sử dụng");
        }

        if (userDTO.getUserName() == null || userDTO.getUserName().isBlank()) {
            throw new APIException("Họ tên không được để trống");
        }
        if (!userDTO.getUserName().matches("^[\\p{L}]+(\\s+[\\p{L}]+)+$")) {
            throw new APIException(
                    "Họ tên phải có ít nhất 2 từ, không chứa số hoặc ký tự đặc biệt"
            );
        }

        if (userDTO.getEmail() == null || userDTO.getEmail().isBlank()) {
            throw new APIException("Email không được để trống");
        }
        if (!userDTO.getEmail().matches("^[A-Za-z0-9._%+-]+@gmail\\.com$")) {
            throw new APIException("Email phải có định dạng @gmail.com");
        }

        if (userDTO.getPassword() == null || userDTO.getPassword().isBlank()) {
            throw new APIException("Mật khẩu không được để trống");
        }
        if (!userDTO.getPassword().matches(
                "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&]).{8,}$"
        )) {
            throw new APIException(
                    "Mật khẩu phải ≥ 8 ký tự, gồm chữ hoa, chữ thường, số và ký tự đặc biệt"
            );
        }


        if (userDTO.getAddress() == null) {
            throw new APIException("Phải nhập đầy đủ địa chỉ (thành phố, đường)");
        }


        if (userDTO.getBrithDay() == null) {
            throw new APIException("Không được để trống ngày sinh");
        }

        LocalDate now = LocalDate.now();
        if (userDTO.getBrithDay().equals(now)) {
            throw new APIException("Ngày sinh không được lớn hơn này hiện tại");
        }

        if (userDTO.getBrithDay().isBefore(LocalDate.now().minusYears(55))) {
            throw new APIException("Tuổi vượt quá giới hạn cho phép");
        }
        if (userDTO.getBrithDay().isAfter(LocalDate.now().minusYears(18))) {
            throw new APIException("Chưa đủ 18 tuổi");
        }

        User user = new User();
        user.setUserName(TextNormalizer.normalizeName(userDTO.getUserName()));
        user.setEmail(userDTO.getEmail());
        user.setCreateAt(LocalDateTime.now());
        user.setCreateBy(SecuritySnapshotUtil.getUserId());
        user.setPhone(userDTO.getPhone());
        user.setBrithDay(userDTO.getBrithDay());
        user.setPassword(passwordEncoder.encode(userDTO.getPassword()));

        Role role = roleRepo.findById(AppConstants.EMPLOYEE)
                .orElseThrow(() -> new APIException("Not found role EMPLOYEE"));


        EPositionType positionType;
        try {
            positionType = EPositionType.valueOf(
                    userDTO.getPosition().trim().toUpperCase()
            );
        } catch (IllegalArgumentException e) {
            throw new APIException("Position không hợp lệ: " + userDTO.getPosition());
        }

        Position position = positionRepo.findByName(positionType)
                .orElseThrow(() ->
                        new APIException("Position chưa tồn tại trong DB: " + positionType)
                );


        if (userDTO.getAddress() != null) {
            String note = TextNormalizer.normalizeDescriptionAndNotification(
                    userDTO.getAddress().getNote()
            );

            Address address = addressRepo.findByCityAndCountryAndStreetAndDistrictAndNoteAndWard(
                    userDTO.getAddress().getCity(),
                    userDTO.getAddress().getStreet(),
                    userDTO.getAddress().getCountry(),
                    userDTO.getAddress().getDistrict(),

                    note,
                    userDTO.getAddress().getWard()
            );


            if (address == null) {
                address = addressRepo.save(
                        new Address(
                                userDTO.getAddress().getStreet(),
                                userDTO.getAddress().getWard(),
                                userDTO.getAddress().getDistrict(),
                                userDTO.getAddress().getCity(),
                                userDTO.getAddress().getCountry(),
                                note
                        )
                );
            }
            address.setUser(user);
            address = addressRepo.save(address);
            user.getAddresses().add(address);
        }


        user.getRoles().add(role);
        user.getPositions().add(position);


        User savedUser = userRepo.save(user);

        if (files != null && !files.isEmpty()) {
            MultipartFile avatarFile = files.get(0);
            String url = fileStorageService.save(avatarFile);
            Image avatartImage = new Image();
            avatartImage.setImageUrl(url);
            avatartImage.setAltText("Avatart of " + savedUser.getUserName());
            savedUser.setAvatar(avatartImage);
            savedUser = userRepo.save(savedUser);
        }
        UserDTO dto = modelMapper.map(savedUser, UserDTO.class);
        if (savedUser.getAvatar() != null) {
            ImageDTO imageDTO = new ImageDTO();
            imageDTO.setId(savedUser.getAvatar().getId());
            imageDTO.setImageUrl(savedUser.getAvatar().getImageUrl());
            dto.setImageDTO(imageDTO);

        }
        dto.setRoleNames(
                savedUser.getRoles().stream()
                        .map(Role::getRoleName)
                        .collect(Collectors.toSet())
        );

        if (!savedUser.getPositions().isEmpty()) {
            dto.setPosition(
                    savedUser.getPositions().iterator().next().getName().name()
            );
        }

        if (!savedUser.getAddresses().isEmpty()) {
            dto.setAddress(
                    modelMapper.map(
                            savedUser.getAddresses().iterator().next(),
                            AddressDTO.class
                    )
            );
        }

        messagingTemplate.convertAndSend(
                "/topic/users",
                Map.of(
                        "event", "EMPLOYEE_CREATED",
                        "data", dto
                )
        );

        return dto;
    }

    @Override
    public String delete(Long userId) {
        User user = userRepo.findById(userId).orElseThrow(() ->
                new ResourceNotFoundException("User", "user", userId));
        userRepo.delete(user);
        return "User name" + user.getUserName() + " delete success!!";
    }

    @Override
    public RoleDTO getRoleByEmail(String email) {
        User userFromDb = userRepo.findUserWithAddresses(email).orElseThrow(() ->
                new APIException("Not fount role with email!!"));
        Set<String> roleName = userFromDb.getRoles().stream().map(Role::getRoleName).
                collect(Collectors.toSet());//.collect(Collectors.toSet()) tạo ra Set<String>
        RoleDTO roleDTO = new RoleDTO();
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

        // MAP POSITION
        if (user.getPositions() != null && !user.getPositions().isEmpty()) {
            EPositionType ePositionType = user.getPositions().iterator().next().getName();
            userDTO.setPosition(ePositionType.name());
        }
        return userDTO;
    }

    @Override
    public Page<GetEmployeesDTO> getEmployee(
            Integer pageNumber,
            Integer pageSize,
            String sortBy,
            String sortOrder,
            String roleName
    ) {
        Sort sort = sortOrder.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(pageNumber, pageSize, sort);

        Page<User> page;

        if (roleName == null || roleName.trim().isEmpty()) {
            // Lấy tất cả user có role
            page = userRepo.findAllUsersWithRole(pageable);
        } else {
            // Lấy user theo role cụ thể
            page = userRepo.findByRoleName(roleName, pageable);
        }
        return page.map(user -> {
            GetEmployeesDTO getEmployeesDTO = new GetEmployeesDTO();
            getEmployeesDTO.setEmail(user.getEmail());
            getEmployeesDTO.setAccountStatus(user.getAccountStatus());
            getEmployeesDTO.setUserId(user.getUserId());
            getEmployeesDTO.setUserName(user.getUserName());
            if (user.getAvatar() != null) {
                getEmployeesDTO.setImageDTO(
                        new ImageDTO(
                                user.getAvatar().getId(),
                                user.getAvatar().getImageUrl(),
                                user.getAvatar().getAltText()
                        )
                );
            }
            getEmployeesDTO.setPhone(user.getPhone());
            if (user.getPositions() != null && !user.getPositions().isEmpty()) {
                getEmployeesDTO.setPosition(
                        user.getPositions().iterator().next().getName().name()
                );
            } else {
                getEmployeesDTO.setPosition(null);
            }

            return getEmployeesDTO;
        });
    }


    @Override
    public UserDTO getDetailsEmployee(Long userId) {
        User user = userRepo.findByIdAndRoleNotUser(userId).orElseThrow(() -> new APIException("Không tìm thấy user"));
        UserDTO userDTO = new UserDTO();
        userDTO.setUserName(user.getUserName());
        userDTO.setEmail(user.getEmail());
        userDTO.setAccountStatus(user.getAccountStatus());
        userDTO.setPhone(user.getPhone());
        if (user.getAvatar() != null) {
            userDTO.setImageDTO(
                    new ImageDTO(
                            user.getAvatar().getId(),
                            user.getAvatar().getImageUrl(),
                            user.getAvatar().getAltText()
                    )
            );
        }
        userDTO.setCreateAt(user.getCreateAt());
        userDTO.setBrithDay(user.getBrithDay());
        user.getAddresses().stream().findFirst()
                .ifPresent(address -> userDTO.setAddress(
                        new AddressDTO(
                                address.getAddressId(),
                                address.getWard(),
                                address.getStreet(),
                                address.getCity(),
                                address.getDistrict(),
                                address.getCountry(),
                                address.getNote()
                        )
                ));

        Set<String> roleName = user.getRoles().stream().map(Role::getRoleName).collect(Collectors.toSet());
        userDTO.setRoleNames(roleName);
        return userDTO;
    }

    @Override
    public UserDTO updateEmployee(
            Long userId,
            UpdateEmployeeDTO dto,
            MultipartFile file
    ) {
        User user = userRepo.findByIdAndRoleNotUser(userId)
                .orElseThrow(() -> new APIException("Không tìm thấy nhân viên"));

        // ===== UPDATE BASIC INFO =====
        if (dto.getUserName() != null && !dto.getUserName().isBlank()) {
            user.setUserName(
                    TextNormalizer.normalizeName(dto.getUserName())
            );
        }

        if (dto.getPhone() != null) {
            user.setPhone(dto.getPhone());
        }

        if (dto.getBrithDay() != null) {
            user.setBrithDay(dto.getBrithDay());
        }

        // ===== UPDATE POSITION =====
        if (dto.getPosition() != null) {
            EPositionType positionType;
            try {
                positionType = EPositionType.valueOf(
                        dto.getPosition().trim().toUpperCase()
                );
            } catch (IllegalArgumentException e) {
                throw new APIException("Position không hợp lệ");
            }

            Position position = positionRepo.findByName(positionType)
                    .orElseThrow(() ->
                            new APIException("Position chưa tồn tại")
                    );

            user.getPositions().clear();
            user.getPositions().add(position);
        }

        // ===== UPDATE ADDRESS =====
        if (dto.getAddress() != null) {
            AddressDTO a = dto.getAddress();

            String note = TextNormalizer.normalizeDescriptionAndNotification(
                    a.getNote()
            );

            Address address = user.getAddresses().stream().findFirst().orElse(null);

            if (address == null) {
                address = new Address();
                address.setUser(user);
            }

            if (a.getStreet() != null) address.setStreet(a.getStreet());
            if (a.getWard() != null) address.setWard(a.getWard());
            if (a.getDistrict() != null) address.setDistrict(a.getDistrict());
            if (a.getCity() != null) address.setCity(a.getCity());
            if (a.getCountry() != null) address.setCountry(a.getCountry());
            if (note != null) address.setNote(note);


            addressRepo.save(address);

            if (user.getAddresses().isEmpty()) {
                user.getAddresses().add(address);
            }
        }


        if (file != null && !file.isEmpty()) {

            String url = fileStorageService.save(file);

            Image avatar = new Image();
            avatar.setImageUrl(url);
            avatar.setAltText("Avatar of " + user.getUserName());

            user.setAvatar(avatar);
            user = userRepo.save(user);
        }


        user = userRepo.save(user);
        UserDTO result = modelMapper.map(user, UserDTO.class);

        result.setRoleNames(
                user.getRoles().stream()
                        .map(Role::getRoleName)
                        .collect(Collectors.toSet())
        );

        if (!user.getPositions().isEmpty()) {
            result.setPosition(
                    user.getPositions().iterator().next().getName().name()
            );
        }

        if (!user.getAddresses().isEmpty()) {
            result.setAddress(
                    modelMapper.map(
                            user.getAddresses().iterator().next(),
                            AddressDTO.class
                    )
            );
        }

        if (user.getAvatar() != null) {
            result.setImageDTO(
                    new ImageDTO(
                            user.getAvatar().getId(),
                            user.getAvatar().getImageUrl(),
                            user.getAvatar().getAltText()
                    )
            );
        }

        // ===== REALTIME =====
        messagingTemplate.convertAndSend(
                "/topic/users",
                Map.of(
                        "event", "EMPLOYEE_UPDATED",
                        "data", result
                )
        );

        return result;
    }


    @Override
    public UserDTO deleteUserById(Long userId) {

        User user = userRepo.findByIdAndRoleNotUser(userId)
                .orElseThrow(() ->
                        new APIException("Không tìm thấy nhân viên hoặc không được phép xóa USER")
                );

        if (user.getAddresses() != null && !user.getAddresses().isEmpty()) {
            user.getAddresses().forEach(address -> address.setUser(null));
            addressRepo.deleteAll(user.getAddresses());
            user.getAddresses().clear();
        }
        if (user.getPositions() != null) {
            user.getPositions().clear();
        }
        if (user.getAvatar() != null) {
            imageRepo.delete(user.getAvatar());
            user.setAvatar(null);
        }
        user.getRoles().clear();
        userRepo.delete(user);
        return modelMapper.map(user, UserDTO.class);
    }

    @Override
    public void updateAccountStatus(Long userId, EAccountStatus status) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new APIException("Không tìm thấy user"));

        user.setAccountStatus(status);
        userRepo.save(user);
    }
}
