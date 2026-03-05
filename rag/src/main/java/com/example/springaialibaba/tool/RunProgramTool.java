package com.example.springaialibaba.tool;

import java.util.function.BiFunction;
import org.springframework.ai.chat.model.ToolContext;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RunProgramTool implements BiFunction<RunProgramRequest, ToolContext, Boolean> {
    @Override
    public Boolean apply(RunProgramRequest runProgramRequest, ToolContext context) {
        String userId = (String) context.getContext().get("USER_ID");
        log.info("{} run program: {}", userId, runProgramRequest);
        return true;
    }
}
