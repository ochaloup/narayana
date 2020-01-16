package com.arjuna.ats.jta.cdi;

@FunctionalInterface
public interface RunnableWithException {
    void run() throws Exception;
}
