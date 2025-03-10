package com.example.auction_management.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/") // Trang chủ
    public String homePage() {
        return "home/home"; // Trả về file /templates/home/home.html
    }

    @GetMapping("/home") // Nếu người dùng vào /home cũng về trang home
    public String redirectToHome() {
        return "home/home";
    }
}
