package com.yzj.partnetBackend.model;

import   com.baomidou.mybatisplus.annotation.*;

import java.io.Serializable;
import java.util.Date;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @TableName user
 */
@TableName(value ="user")
@Data
//这里必须加上implements Serializable 进行序列化，否则不能使用redis分布式登录
public class User implements Serializable {
    @TableId(type = IdType.AUTO)
    private long id;

    private String username;

    private String userAccount;

    private String avatarUrl;

    private Integer gender;

    private String profile;

    private String userPassword;

    private String phone;

    private String email;

    private Integer userStatus;

    private Date createTime;

    private Date updateTime;

    @TableLogic
    private Integer isDelete;

    private Integer userRole;

    private String planetCode;

    private String tags;
}