package com.envision.epc.module.user.application;

import com.envision.epc.module.user.domain.User;
import com.envision.epc.module.user.domain.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author chaoyue.zhao1
 * @since 2025/08/11-11:00
 */
@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class UserFacade {
    private final UserRepository repository;

    public User getUserByUserCode(String userCode) {
        return repository.getByUserCode(userCode);
    }
}
