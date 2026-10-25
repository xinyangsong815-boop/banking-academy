package cn.academy.bank.identity.application;

import cn.academy.bank.identity.domain.StaffPrincipal;
import cn.academy.bank.shared.domain.BusinessException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class CurrentStaff {
    public StaffPrincipal get() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !(authentication.getPrincipal() instanceof StaffPrincipal staff)) {
            throw new BusinessException("UNAUTHORIZED", "请先登录");
        }
        return staff;
    }
}
