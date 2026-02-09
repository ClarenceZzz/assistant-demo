package com.example.springaialibaba.tool;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

public record RunProgramRequest(
    @JsonProperty(required = true)
    @JsonPropertyDescription("待运行的程序名称，例如：计算器")
    String programName,

    @JsonProperty(required = false, defaultValue = "15")
    @JsonPropertyDescription("运行时长，默认值为15，单位：分钟")
    Integer mins
) {} 
