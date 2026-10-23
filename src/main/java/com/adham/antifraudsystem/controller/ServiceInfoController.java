package com.adham.antifraudsystem.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ServiceInfoController {

    @GetMapping("/")
    public ServiceInfoResponse getServiceInfo() {
        return new ServiceInfoResponse(
                "Anti-Fraud System API",
                "UP",
                "/actuator/health",
                "https://github.com/adhambatlouni/anti-fraud-system"
        );
    }

    public record ServiceInfoResponse(
            String name,
            String status,
            String health,
            String repository
    ) {
    }
}
