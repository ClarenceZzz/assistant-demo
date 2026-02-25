package com.example.springaialibaba.controller.dto;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

public record Book(@JsonPropertyDescription ("书名") String name,   
                @JsonPropertyDescription("作者") String author,
                @JsonPropertyDescription("简介") String desc) {

}
