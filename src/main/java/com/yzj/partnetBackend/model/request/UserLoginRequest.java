package com.yzj.partnetBackend.model.request;


import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class UserLoginRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 698463775876496748L;

    private String userAccount;

    private String userPassword;
}
