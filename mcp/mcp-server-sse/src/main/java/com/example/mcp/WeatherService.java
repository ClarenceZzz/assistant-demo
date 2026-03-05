package com.example.mcp;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;

@Service
public class WeatherService {

    @Tool(name = "getWeather", description = "根据城市名称查询天气信息",returnDirect = true)
    public String getWeather(String city) {
        if (city == null) {
            return "请提供城市名称";
        }
        switch (city) {
            case "北京": return "北京: 晴, 25°C";
            case "上海": return "上海: 多云, 22°C";
            case "深圳": return "深圳: 小雨, 28°C";
            default: return city + ": 下雪, -20°C";
        }
    }
}
