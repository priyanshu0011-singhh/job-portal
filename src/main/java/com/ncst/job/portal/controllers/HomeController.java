package com.ncst.job.portal.controllers;

import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HomeController {

    @GetMapping({"", "/"})
    public Map<String, Object> index() {
        return Map.of(
            "message", "Job Portal API is running",
            "docs", "/swagger-ui/index.html",
            "login", "/api/auth/login",
            "register", "/api/users/register"
        );
    }
}
