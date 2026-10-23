package com.fenglin.springboottest.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 档案分类（多级）：parent_id 自关联，level 记录层级，0 表示一级分类的父 id。
 */
@Data
@TableName("star_category")
public class Category {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 父分类 id，0 表示一级分类 */
    private Long parentId;

    /** 分类编码（唯一） */
    private String code;

    /** 分类名称 */
    private String name;

    /** 层级：1 一级 / 2 二级 / … */
    private Integer level;

    /** 图标（emoji） */
    private String icon;

    /** 分类主色 #RRGGBB */
    private String color;

    /** 分类说明 */
    private String description;

    /** 同级排序，小值优先 */
    private Integer sortNo;

    /** 1 启用 / 0 停用 */
    private Integer status;

    private LocalDateTime createdAt;
}
