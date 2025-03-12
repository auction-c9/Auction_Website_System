package com.example.auction_management.service.impl;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Random;

@Service
public class CaptchaService {
    private static final String[] OPERATORS = {"+", "-", "*"};
    private static final int MAX_NUMBER = 10;

    public Map<String, String> generateSimpleMathQuestion() {
        Random random = new Random();
        int a = random.nextInt(MAX_NUMBER) + 1;
        int b = random.nextInt(MAX_NUMBER) + 1;
        String operator = OPERATORS[random.nextInt(OPERATORS.length)];

        String question = a + " " + operator + " " + b + " = ?";
        String answer = String.valueOf(calculate(a, b, operator));

        return Map.of(
                "question", question,
                "answer", answer
        );
    }

    private int calculate(int a, int b, String operator) {
        return switch (operator) {
            case "+" -> a + b;
            case "-" -> a - b;
            case "*" -> a * b;
            default -> throw new IllegalArgumentException("Operator không hợp lệ");
        };
    }
}
