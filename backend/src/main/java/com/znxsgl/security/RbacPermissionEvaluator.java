package com.znxsgl.security;

import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import java.io.Serializable;

/**
 * RBAC 权限评估器
 *
 * 支持 Spring Security 表达式：hasPermission(null, 'perm:code')
 * 判断当前用户是否拥有指定的权限编码。
 *
 * 说明：
 * - JwtAuthFilter 登录后会将用户的 permissions 列表以 SimpleGrantedAuthority 形式写入 Authentication
 * - 本评估器遍历 Authentication 中的 authorities，匹配权限编码字符串
 */
@Component
public class RbacPermissionEvaluator implements PermissionEvaluator {

    @Override
    public boolean hasPermission(Authentication authentication, Object targetDomainObject, Object permission) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        String required = String.valueOf(permission);
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(required::equals);
    }

    @Override
    public boolean hasPermission(Authentication authentication, Serializable targetId, String targetType, Object permission) {
        return hasPermission(authentication, null, permission);
    }
}
