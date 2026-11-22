package com.sanskaracademy.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    // localhost:8080/  ->  login page
    @GetMapping("/")
    public String home() {
        return "redirect:/login";
    }

    // localhost:8080/dashboard  ->  dashboard of the logged-in user
    @GetMapping("/dashboard")
    public String myDashboard(Authentication auth) {
        if (auth == null) {
            return "redirect:/login";
        }
        for (GrantedAuthority a : auth.getAuthorities()) {
            switch (a.getAuthority()) {
                case "ROLE_ADMIN":   return "redirect:/admin/dashboard";
                case "ROLE_TEACHER": return "redirect:/teacher/dashboard";
                case "ROLE_STUDENT": return "redirect:/student/dashboard";
                case "ROLE_PARENT":  return "redirect:/parent/dashboard";
                default: break;
            }
        }
        return "redirect:/login";
    }
}
