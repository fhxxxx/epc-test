package com.envision.bunny.module.uploadfile.web;

import com.envision.extract.module.event.ProjectDeleteEvent;
import com.envision.extract.module.uploadfile.application.UploadFileCommandService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * @author chaoyue.zhao1
 * @since 2025/08/11-14:42
 */
@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class UploadFileEventListener {
    private final UploadFileCommandService uploadFileCommandService;

    @EventListener
    public void handle(ProjectDeleteEvent event) {
        uploadFileCommandService.deleteByProjectId( event);
    }
}
