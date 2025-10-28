package com.yzj.partnetBackend.common;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Set;

/**
 * 同用的删除请求
 */
@Data
public class DeleteRequest implements Serializable {
    @Serial
    private static final long serialVersionUID = -3781578379244875608L;

    private long id;
}
