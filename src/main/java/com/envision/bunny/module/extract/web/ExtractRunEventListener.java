package com.envision.bunny.module.extract.web;

import com.envision.extract.module.event.ProjectDeleteEvent;
import com.envision.extract.module.extract.application.ExtractCommandService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * @author chaoyue.zhao1
 * @since 2025/08/22-14:51
 */
@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class ExtractRunEventListener {
    private final ExtractCommandService extractCommandService;

    @EventListener
    public void handle(ProjectDeleteEvent event){
        extractCommandService.deleteByProjectId(event);
    }
}
