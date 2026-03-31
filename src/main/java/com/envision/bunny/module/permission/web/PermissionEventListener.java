package com.envision.bunny.module.permission.web;

import com.envision.extract.module.event.ProjectDeleteEvent;
import com.envision.extract.module.permission.application.PermissionCommandService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * @author chaoyue.zhao1
 * @since 2025/08/11-14:41
 */
@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class PermissionEventListener {
    private final PermissionCommandService permissionCommandService;

    @EventListener
    public void handle(ProjectDeleteEvent event){
        permissionCommandService.deleteByProjectId( event);
    }
}
