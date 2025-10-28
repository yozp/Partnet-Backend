package com.yzj.partnetBackend.model.request;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class TeamQuitRequest implements Serializable {
    @Serial
    private static final long serialVersionUID = 7931675005178775413L;

    /**
     * 队伍id
     */
    private Long teamId;
}
