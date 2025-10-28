package com.yzj.partnetBackend.model.dto;

import com.yzj.partnetBackend.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 队伍查询封装类
 * @TableName team
 */
@EqualsAndHashCode(callSuper = true)//该注解自动生成equals()和hashCode()方法的实现代码
//callSuper参数是一个布尔值，默认为false。当设置为true时，Lombok在生成equals()和hashCode()方法时，
// 不仅考虑当前类的字段，还会调用父类的相应方法，合并父类字段的影响
@Data
public class TeamQuery extends PageRequest {
    /**
     * id
     */
    private Long id;

    /**
     * id 列表
     */
    private List<Long> idList;

    /**
     * 搜索关键词（同时对队伍名称和描述搜索）
     */
    private String searchText;

    /**
     * 队伍名称
     */
    private String name;

    /**
     * 描述
     */
    private String description;

    /**
     * 最大人数
     */
    private Integer maxNum;

    /**
     * 用户id
     */
    private Long userId;

    /**
     * 0 - 公开，1 - 私有，2 - 加密
     */
    private Integer status;
}