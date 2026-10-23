package com.adham.antifraudsystem.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ServiceInfoController {

    @GetMapping("/")
    public String openApiDocumentation() {
        return "redirect:/swagger-ui.html";
    }
}
