package com.example.springai;

import java.lang.reflect.Method;

import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.method.MethodToolCallback;
import org.springframework.ai.util.json.schema.JsonSchemaGenerator;
import org.springframework.util.ReflectionUtils;

import com.example.springai.service.DateTool;

public class TmpTest {
    // public static void main(String[] args) {
    //     try {
    //         Method targetMethod = ReflectionUtils.findMethod(DateTool.class, "getDay");

    //         MethodToolCallback dateTool = MethodToolCallback.builder()
    //                 .toolDefinition(ToolDefinition.builder()
    //                         .name("date_tool")
    //                         .description("查询今天是几号")
    //                         .inputSchema(JsonSchemaGenerator.generateForMethodInput(targetMethod))
    //                         .build())
    //                 .toolMethod(targetMethod)
    //                 .toolObject(new DateTool())
    //                 .build();
    //         System.out.println("Success! Schema: " + dateTool.getToolDefinition().getInputSchema());
    //     } catch (Exception e) {
    //         e.printStackTrace();
    //     }
    // }
}
