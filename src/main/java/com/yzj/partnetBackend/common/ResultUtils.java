package com.yzj.partnetBackend.common;

/**
 * 返回结果工具类
 * 也就是在结果类上再封装一层，减少输入量
 */
public class ResultUtils {

    /**
     * 成功（对应构造器2）
     * @param data
     * @return
     * @param <T>
     */
    public static <T> BaseResponse<T> success(T data){
        return new BaseResponse<>(0,data,"success");
    }

    /**
     * 失败（对应构造器4）
     * @param errorCode
     * @return
     */
    public static BaseResponse error(ErrorCode errorCode){
        return new BaseResponse<>(errorCode);
    }

    /**
     * 失败（对应构造器1）
     * @param code
     * @param message
     * @param description
     * @return
     */
    public static BaseResponse error(int code,String message,String description){
        return new BaseResponse<>(code,null,message,description);
    }

    /**
     * 失败（对应构造器1）
     * @param errorCode
     * @param message
     * @param description
     * @return
     */
    public static BaseResponse error(ErrorCode errorCode,String message,String description){
        return new BaseResponse(errorCode.getCode(),null,message,description);
    }

    /**
     * 失败（对应构造器2）
     * @param errorCode
     * @param description
     * @return
     */
    public static BaseResponse error(ErrorCode errorCode,String description){
        return new BaseResponse(errorCode.getCode(),errorCode.getMessage(),description);
    }
}
