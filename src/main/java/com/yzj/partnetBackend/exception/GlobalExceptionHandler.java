package com.yzj.partnetBackend.exception;

import com.yzj.partnetBackend.common.BaseResponse;
import com.yzj.partnetBackend.common.ErrorCode;
import com.yzj.partnetBackend.common.ResultUtils;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器（处理方法）
 *
 * 简单解释一下：
 * 本质就是aop编程
 * 意思是每次发生一次异常都会有对应的处理方法，如发生BusinessException异常
 * 就执行businessExceptionHandler方法，同时将返回结果传给前端ResultUtils.error
 * 这样我们控制层的返回值就使用BusinessException类的构造器即可
 * 如：throw new BusinessException(ErrorCode.PARAMS_ERROR)
 */
@RestControllerAdvice
@Slf4j
@Hidden//这个注解很关键，不加则knife文档打不开！！！
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public BaseResponse businessExceptionHandler(BusinessException e){
        log.error("runtimeException"+ e.getMessage(),e);
        return ResultUtils.error(e.getCode(),e.getMessage(), e.getDescription());
    }

    @ExceptionHandler(RuntimeException.class)
    public BaseResponse runtimeExceptionHandler(Exception e){
        log.error("runtimeException"+ e);
        return ResultUtils.error(ErrorCode.SYSTEM_ERROR,"");
    }
}
