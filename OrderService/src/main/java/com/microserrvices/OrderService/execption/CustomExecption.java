package com.microserrvices.OrderService.execption;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data

public class CustomExecption extends RuntimeException{
    private String errorCode;
    private int status;

    public CustomExecption(String message,String errorCode, int status){
        super(message);
        this.errorCode = errorCode;
        this.status = status;
    }
}
