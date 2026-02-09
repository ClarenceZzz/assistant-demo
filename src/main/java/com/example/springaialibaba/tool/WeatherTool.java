package com.example.springaialibaba.tool;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

@Service
public class WeatherTool {
    @Tool(description = "查询城市温度")
    public String getTemperature(@ToolParam(description = "待查询的城市名称，例如：厦门") String cityName) {
        return cityName + "的温度是0°C";
    }

    @Tool(description = "查询城市天气")
    public String getWeather(@ToolParam(description = "待查询的城市名称，例如：厦门") String cityName) {
        return cityName + "的天气是下雪";
    }
}
