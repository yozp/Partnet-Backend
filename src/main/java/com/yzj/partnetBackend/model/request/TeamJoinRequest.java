package com.yzj.partnetBackend.model.request;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class TeamJoinRequest implements Serializable {
    @Serial
    private static final long serialVersionUID = 6519473878536414594L;

    /**
     * 队伍id
     */
    private Long teamId;

    /**
     * 队伍密码
     */
    private String password;
}
