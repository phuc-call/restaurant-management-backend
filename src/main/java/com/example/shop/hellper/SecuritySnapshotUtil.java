package com.example.shop.hellper;

import com.example.shop.config.UserInfoConfig;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;



public class SecuritySnapshotUtil {
    public static UserInfoConfig getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null
                || !auth.isAuthenticated()
                || "anonymousUser".equals(auth.getPrincipal())) {
            return null;
        }

        Object principal = auth.getPrincipal();
        if (principal instanceof UserInfoConfig user) {
            return user;
        }
        return null;
    }


    public static String getRole(){
        Authentication auth=SecurityContextHolder.getContext().getAuthentication();
        if(auth==null) return "UNKNOWN";
        return auth.getAuthorities()
                .stream().findFirst()
                .map(GrantedAuthority::getAuthority).orElse("UNKNOWN");
    }

    public static String getEmployeeName() {
        UserInfoConfig user = getCurrentUser();
        return user != null ? user.getFullName() : "SYSTEM";
    }

    public static String getEmail(){
        UserInfoConfig user=getCurrentUser();
        return user!=null?user.getEmail():"SYSTEM";
    }

    public static Long getUserId() {
        UserInfoConfig user = getCurrentUser();
        return user != null ? user.getUserId() : null;
    }



    public static String getPosition() {
        UserInfoConfig user = getCurrentUser();
        return user != null ? user.getPosition() : "UNKNOWN";
    }

    public static boolean isAuthenticated() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null
                && auth.isAuthenticated()
                && !"anonymousUser".equals(auth.getPrincipal());
    }

}
