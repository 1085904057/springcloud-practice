package com.springcloud.practice.service.tomcat.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@RestController
public class LongTimeController {
    @RequestMapping(value = "/download", method = RequestMethod.GET)
    public String getLogin(HttpServletRequest request) throws InterruptedException {
        String userAgent = request.getHeader(HttpHeaders.USER_AGENT);
        Thread.sleep(10000);
        System.out.println(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + "----->" + userAgent);
        return "good";
    }
}
