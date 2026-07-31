package kh.edu.paragoniu.court_admin.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;




@Controller
public class ErrorPageController{
    @GetMapping("/error/403")
    public String accessDenied() {
        return "error/403";
    }

}
