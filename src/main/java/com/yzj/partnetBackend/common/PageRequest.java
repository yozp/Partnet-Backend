package com.yzj.partnetBackend.common;

import lombok.Data;

import java.io.Serializable;

/**
 * 分页查询默认分页数据
 */
@Data
public class PageRequest implements Serializable {

    private static final long serialVersionUID=51513515351L;

    /**
     * 页面大小
     */
    protected int pageSize=10;

    /**
     * 当前第几页
     */
    protected int pageNum=1;
}
