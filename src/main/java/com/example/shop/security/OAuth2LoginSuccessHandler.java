package com.example.shop.security;

import com.example.shop.config.AppConstants;
import com.example.shop.entity.Role;
import com.example.shop.entity.User;
import com.example.shop.exception.APIException;
import com.example.shop.repository.RoleRepo;
import com.example.shop.repository.UserRepo;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.util.Optional;


@Component
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {
    @Autowired
    UserRepo userRepo;
    @Autowired
    JWTUtil jwtUtil;
    @Autowired
    RoleRepo roleRepo;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication)
            throws IOException {

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");
        Boolean emailVerified = oAuth2User.getAttribute("email_verified");

        if (!Boolean.TRUE.equals(emailVerified)) {
            response.sendRedirect("http://localhost:3000/login?error=Email not verified");
            return;
        }

        // Lấy mode từ cookie
        String mode = null;
        if (request.getCookies() != null) {
            for (var cookie : request.getCookies()) {
                if ("oauth_mode".equals(cookie.getName())) {
                    mode = cookie.getValue();
                }
            }
        }

        Optional<User> existingUser = userRepo.findByEmail(email);
        User user;

        if ("login".equalsIgnoreCase(mode)) {

            if (existingUser.isEmpty()) {
                response.sendRedirect("http://localhost:3000/login?error=Account not found. Please register first.");
                return;
            }

            user = existingUser.get();

        } else if ("register".equalsIgnoreCase(mode)) {

            if (existingUser.isPresent()) {
                response.sendRedirect("http://localhost:3000/register?error=Account already exists. Please login.");
                return;
            }

            user = new User();
            user.setUserName(name);
            user.setEmail(email);
            user.setProvider("GOOGLE");
            user.setPassword(null);

            Role role = roleRepo.findById(AppConstants.USER_ID)
                    .orElseThrow(() -> new APIException("Không tìm thấy vai trò thích hợp"));

            user.getRoles().add(role);
            user = userRepo.save(user);

        } else {
            response.sendRedirect("http://localhost:3000/login?error=Invalid mode");
            return;
        }

        String token = jwtUtil.generaToken(user.getEmail());

        // Xóa cookie sau khi dùng
        response.addHeader("Set-Cookie", "oauth_mode=; Max-Age=0; path=/");

        response.sendRedirect("http://localhost:3000/oauth2-success?token=" + token);
    }
}