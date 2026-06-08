package com.hnust.health.aop;

import com.hnust.health.annotation.RequireRole;
import com.hnust.health.exception.BusinessException;
import com.hnust.health.mapper.SysUserMapper;
import com.hnust.health.model.SysUser;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static com.hnust.health.constant.Constants.REQUEST_ATTR_USER_ID;

@Aspect
@Component
@RequiredArgsConstructor
public class RoleCheckAspect {

    private final SysUserMapper sysUserMapper;

    @Around("@annotation(requireRole)")
    public Object checkRole(ProceedingJoinPoint joinPoint, RequireRole requireRole) throws Throwable {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) throw new BusinessException(500, "无法获取请求上下文");

        HttpServletRequest request = attrs.getRequest();
        Long userId = (Long) request.getAttribute(REQUEST_ATTR_USER_ID);
        if (userId == null) throw new BusinessException(401, "未认证");

        SysUser user = sysUserMapper.selectById(userId);
        if (user == null || !requireRole.value().equalsIgnoreCase(user.getRole())) {
            throw new BusinessException(403, "权限不足，需要 " + requireRole.value() + " 角色");
        }
        return joinPoint.proceed();
    }
}
