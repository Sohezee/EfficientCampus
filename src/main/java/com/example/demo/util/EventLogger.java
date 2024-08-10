package com.example.demo.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.io.StringWriter;


public class EventLogger {
    private final Logger logger;
    private final StringWriter stringWriter = new StringWriter();
    private final PrintWriter printWriter = new PrintWriter(stringWriter);

    public EventLogger(Class<?> clazz) {
        this.logger = LoggerFactory.getLogger(clazz);
    }

    public synchronized void logException(Exception e) {
        stringWriter.getBuffer().setLength(0);
        e.printStackTrace(printWriter);
        logger.error(stringWriter.toString());
    }
    public synchronized void logException(String message) {
        logger.error(message);
    }
    public synchronized void logException(Exception e, String message) {
        stringWriter.getBuffer().setLength(0);
        e.printStackTrace(printWriter);
        logger.error("{}\n{}", message, stringWriter);
    }

}
