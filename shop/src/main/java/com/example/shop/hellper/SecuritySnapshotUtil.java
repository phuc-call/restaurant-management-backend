package com.example.shop.hellper;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

public class SecuritySnapshotUtil {
    public static String getUserName(){
        Authentication auth= SecurityContextHolder.getContext().getAuthentication();
        if(auth==null||!auth.isAuthenticated()){
            return "SYSTEM";
        }
        return auth.getName();//userName/email
    }
    public static String getRole(){
        Authentication auth=SecurityContextHolder.getContext().getAuthentication();
        if(auth==null) return "UNKNOWN";
        return auth.getAuthorities()
                .stream().findFirst()
                .map(GrantedAuthority::getAuthority).orElse("UNKNOWN");
    }
    public static boolean isAuthenticated() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null
                && auth.isAuthenticated()
                && !"anonymousUser".equals(auth.getPrincipal());
    }

}
