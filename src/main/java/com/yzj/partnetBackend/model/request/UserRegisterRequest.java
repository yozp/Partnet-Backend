package com.yzj.partnetBackend.model.request;

import lombok.Data;

import java.io.Serializable;

@Data
//继承Serializable，实现序列化
public class UserRegisterRequest implements Serializable {
    private static final long serialVersionUID = 698455135158L;

    private String userAccount;
    private String userPassword;
    private String checkPassword;
    private String planetCode;
}
