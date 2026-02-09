package com.example.springaialibaba.tool;

import org.springframework.ai.tool.annotation.ToolParam;

public record WeatherRequest(@ToolParam(description = "待查询的城市名称，例如：厦门") String cityName) {
}