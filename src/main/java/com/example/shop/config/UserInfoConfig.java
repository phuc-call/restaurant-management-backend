    package com.example.shop.config;

    import com.example.shop.entity.Position;
    import com.example.shop.entity.Role;
    import com.example.shop.entity.User;
    import com.example.shop.entity.enums.EAccountStatus;
    import lombok.AllArgsConstructor;
    import lombok.Data;
    import lombok.NoArgsConstructor;
    import org.springframework.security.core.GrantedAuthority;
    import org.springframework.security.core.authority.SimpleGrantedAuthority;
    import org.springframework.security.core.userdetails.UserDetails;

    import java.security.PublicKey;
    import java.util.Collection;
    import java.util.List;
    import java.util.stream.Collectors;
    import java.util.stream.Stream;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public class UserInfoConfig implements UserDetails {
        private static final long serialVersionUID = 1L;
        private String email;
        private String password;
        private Long userId;
        private String fullName;
        private String position;
        private EAccountStatus accountStatus;
        private List<GrantedAuthority> authorities;

        public UserInfoConfig(User user) {
            this.fullName=user.getUserName();
            this.email = user.getEmail();
            this.password = user.getPassword();
            this.userId=user.getUserId();
            this.accountStatus = user.getAccountStatus();

            // Lưu tên tất cả position
            this.position = user.getPositions().stream()
                    .map(pos->pos.getName().name())
                    .collect(Collectors.joining(","));
            // Role
            List<GrantedAuthority> roleAuthorities = user.getRoles().stream()
                    .map(role -> new SimpleGrantedAuthority(role.getRoleName()))
                    .collect(Collectors.toList());
            // Position: đưa vào authority để phân quyền chi tiết
            List<GrantedAuthority> positionAuthorities = user.getPositions().stream()
                    .map(pos -> new SimpleGrantedAuthority(pos.getName().name()))
                    .collect(Collectors.toList());
            this.authorities = Stream.concat(roleAuthorities.stream(), positionAuthorities.stream())
                    .collect(Collectors.toList());
        }


        @Override
        public Collection<? extends GrantedAuthority> getAuthorities() {
            return authorities;
        }

        @Override
        public String getUsername() {
            return email;
        }

        @Override
        public boolean isAccountNonExpired() {
            return true;
        }

        @Override
        public boolean isAccountNonLocked() {

            return accountStatus != EAccountStatus.LOCKED;
        }

        @Override
        public boolean isCredentialsNonExpired() {
            return true;
        }


        @Override
        public boolean isEnabled() {
            return accountStatus == EAccountStatus.ACTIVE;
        }

    }
