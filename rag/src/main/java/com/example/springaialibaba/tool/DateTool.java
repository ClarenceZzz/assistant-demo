package com.example.springaialibaba.tool;

import java.time.LocalDate;

public class DateTool {
    public String getDay() {
        return LocalDate.now().getDayOfMonth() + "";
    }
}
