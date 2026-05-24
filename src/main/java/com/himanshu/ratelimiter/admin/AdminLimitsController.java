package com.himanshu.ratelimiter.admin;

import com.himanshu.ratelimiter.core.DynamicLimitService;
import com.himanshu.ratelimiter.core.LimitRule;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/limits")
public class AdminLimitsController {

    private final DynamicLimitService dynamicLimitService;

    public AdminLimitsController(DynamicLimitService dynamicLimitService) {
        this.dynamicLimitService = dynamicLimitService;
    }

    @PostMapping
    AdminLimitResponse update(@Valid @RequestBody AdminLimitRequest request) {
        LimitRule rule = dynamicLimitService.save(request);
        return new AdminLimitResponse(
                request.identifierType(),
                request.identifier(),
                request.method(),
                request.endpoint(),
                rule.algorithm(),
                rule.limit(),
                rule.window().toSeconds()
        );
    }
}
