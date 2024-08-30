package com.example.demo.rest;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class ForwardController {

    @RequestMapping(value = {"/{path:[^.]*}"})
    public String forward() {
        return "forward:/";
    }
}
