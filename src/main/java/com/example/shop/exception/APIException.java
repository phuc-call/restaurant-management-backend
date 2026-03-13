package com.example.shop.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class APIException extends RuntimeException {
    private static final long serialVersionID=1;
    public APIException(String message) {
        super(message);
    }
    public APIException(String ...messages){
        super(String.join(" ",messages));
    }

}
