package com.example.shop.exception;

public class ResourceNotFoundException extends RuntimeException{
    private static final long serialVersionID=1;
    String resourceName;
    String field;
    String fieldName;
    Long fieId;
    public ResourceNotFoundException(){}
    public ResourceNotFoundException(String resourceName,String field,String fieldName){
        super("%s not found with %s: %s".formatted(resourceName,field,fieldName));
        this.resourceName=resourceName;
        this.field=field;
        this.fieldName=fieldName;
    }
    public ResourceNotFoundException(String resourceName,String field,Long fieId){
        super("%s not found with %s: %d".formatted(resourceName,field,fieId));
        this.resourceName=resourceName;
        this.field=field;
        this.fieId=fieId;
    }
}
