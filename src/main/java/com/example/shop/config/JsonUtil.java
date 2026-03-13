package com.example.shop.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class JsonUtil {
    @Autowired
    ObjectMapper objectMapper;
    public String toJson(Object ob){
        try {
            return objectMapper.writeValueAsString(ob);
        }catch (Exception e){
            return null;
        }
    }
}
