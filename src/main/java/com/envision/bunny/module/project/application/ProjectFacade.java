package com.envision.bunny.module.project.application;

import com.envision.extract.infrastructure.response.BizException;
import com.envision.extract.infrastructure.response.ErrorCode;
import com.envision.extract.module.project.domain.Project;
import com.envision.extract.module.project.domain.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * @author chaoyue.zhao1
 * @since 2025/08/22-13:37
 */
@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class ProjectFacade {
    private final ProjectRepository repository;

    public Project getById(Long id) {
        Optional<Project> optById = repository.getOptById(id);
        if (optById.isEmpty()){
            throw new BizException(ErrorCode.BAD_REQUEST, "项目不存在");
        }
        return optById.get();
    }
}
