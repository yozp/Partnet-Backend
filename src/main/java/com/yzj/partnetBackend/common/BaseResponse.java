package com.yzj.partnetBackend.common;

import lombok.Data;

import java.io.Serializable;

/**
 * 封装结果返回类
 * @param <T>
 */
@Data
public class BaseResponse <T> implements Serializable {

    private int code;

    private T data;

    private String message;

    private String description;

    //构造器参数逐级减少
    //构造器1
    public BaseResponse(int code, T data, String message, String description) {
        this.code = code;
        this.data = data;
        this.message = message;
        this.description = description;
    }

    //构造器2
    public BaseResponse(int code, T data, String message) {
        this(code,data,message,"");
    }

    //构造器3
    public BaseResponse(int code, T data){
        this(code,data,"","");
    }

    //构造器4
    public BaseResponse(ErrorCode errorCode){
        this(errorCode.getCode(),null,errorCode.getMessage(),errorCode.getDescription());
    }
}
