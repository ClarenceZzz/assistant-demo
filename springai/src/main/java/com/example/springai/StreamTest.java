package com.example.springai;

import org.springframework.ai.chat.model.ChatResponse;
import java.lang.reflect.Method;

public class StreamTest {
    public static void main(String[] args) {
        for (Method m : ChatResponse.class.getMethods()) {
            System.out.println(m.getName() + " -> " + m.getReturnType().getName());
        }
    }
}
