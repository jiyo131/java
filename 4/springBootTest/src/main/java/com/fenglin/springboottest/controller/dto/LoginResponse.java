package com.fenglin.springboottest.controller.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {
    private String token;
    private String username;
    private String nickname;
    /** 权限：1 默认权限 / 0 管理员权限 */
    private Integer permission;
}
