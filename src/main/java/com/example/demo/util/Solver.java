package com.example.demo.util;

import com.twocaptcha.TwoCaptcha;
import com.twocaptcha.captcha.Normal;

public class Solver {

    public String key;

    private final EventLogger eventLogger = new EventLogger(Solver.class);
    public TwoCaptcha solver;

    public Solver() {
        solver = new TwoCaptcha(PropertiesLoader.getProperty("captcha.key"));
        key = PropertiesLoader.getProperty("captcha.key");
    }

    public Normal solveCaptcha(String file) {
        Normal captcha = new Normal();
        captcha.setFile("src/main/screenshots/" + file);
        captcha.setMinLen(4);
        captcha.setMaxLen(20);
        captcha.setCaseSensitive(true);
        captcha.setLang("en");

        try {
            solver.solve(captcha);
            return(captcha);
        } catch (Exception e) {
            eventLogger.logException(e);
            return null;
        }
    }
}
