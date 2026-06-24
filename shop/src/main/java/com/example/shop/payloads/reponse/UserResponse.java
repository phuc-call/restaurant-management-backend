package com.example.shop.payloads.reponse;

import com.example.shop.payloads.UserDTO;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@NoArgsConstructor
@Data
public class UserResponse {
    private List<UserDTO>content;
    private Integer pageSize;
    private Integer pageNumber;
    private Long totalElements;
    private  Integer totalPage;
    private boolean lastPage;
}
