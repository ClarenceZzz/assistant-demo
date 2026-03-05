package com.example.springai.service;

import java.time.LocalDate;

public class DateTool {
    public String getDay() {
        return LocalDate.now().getDayOfMonth() + "";
    }
}
