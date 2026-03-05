package com.example.springai.service;

import java.util.function.BiFunction;
import org.springframework.ai.chat.model.ToolContext;
import lombok.extern.slf4j.Slf4j;

import com.example.springai.model.RunProgramRequest;

@Slf4j
public class RunProgramTool implements BiFunction<RunProgramRequest, ToolContext, Boolean> {
    @Override
    public Boolean apply(RunProgramRequest runProgramRequest, ToolContext context) {
        String userId = (String) context.getContext().get("USER_ID");
        String traceId = (String) context.getContext().get("TRACE_ID");
        log.info("{} {} run program: {}", traceId, userId, runProgramRequest);
        return true;
    }
}
