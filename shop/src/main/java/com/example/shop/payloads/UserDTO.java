    package com.example.shop.payloads;

    import com.example.shop.entity.Role;
    import lombok.AllArgsConstructor;
    import lombok.Data;
    import lombok.NoArgsConstructor;
    import java.util.HashSet;
    import java.util.Set;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public class UserDTO {
        private String userName;
        private String phone;
        private String email;
        private String password;
        private Set<String> roleNames;
        private String position;
        private AddressDTO address;
    }
