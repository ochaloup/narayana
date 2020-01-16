package com.arjuna.ats.jta.cdi.common;

@FunctionalInterface
public interface RunnableWithException {
    void run() throws Exception;
}
