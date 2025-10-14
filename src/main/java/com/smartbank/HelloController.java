package com.smartbank;

import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {
    @org.springframework.web.bind.annotation.RequestMapping("/hello")
    public String sayHello() {
        return "Hello World Saranya SDE at Paypal with 30 Lak per annum and take home 2 lak per month";
    }
}
