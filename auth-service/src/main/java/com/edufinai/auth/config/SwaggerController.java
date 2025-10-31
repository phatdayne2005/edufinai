package com.edufinai.auth.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SwaggerController {

    @GetMapping("/")
    public String redirectToSwagger() {
        return "redirect:/swagger-ui/index.html";
    }

    @GetMapping("/swagger")
    public String redirectToSwagger2() {
        return "redirect:/swagger-ui/index.html";
    }

    @GetMapping("/docs")
    public String redirectToSwagger3() {
        return "redirect:/swagger-ui/index.html";
    }
}