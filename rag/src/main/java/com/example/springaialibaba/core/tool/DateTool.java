package com.example.springaialibaba.core.tool;

import java.time.LocalDate;

public class DateTool {
    public String getDay() {
        return LocalDate.now().getDayOfMonth() + "";
    }
}
