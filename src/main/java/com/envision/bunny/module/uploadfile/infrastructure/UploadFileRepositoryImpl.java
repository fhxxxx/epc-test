package com.envision.bunny.module.uploadfile.infrastructure;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.envision.extract.module.uploadfile.domain.UploadFile;
import com.envision.extract.module.uploadfile.domain.UploadFileRepository;
import com.envision.extract.module.uploadfile.domain.UploadTypeEnum;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author wenjun.gu
 * @since 2025/8/12-16:36
 */
@Service
public class UploadFileRepositoryImpl extends ServiceImpl<UploadFileMapper, UploadFile> implements UploadFileRepository {
    @Override
    public UploadFile getByHash(String hash) {
        return this.lambdaQuery().eq(UploadFile::getHash, hash).last("LIMIT 1").one();
    }

    @Override
    public UploadFile getFile(Long id, UploadTypeEnum type, Long projectId) {
        return this.lambdaQuery().eq(UploadFile::getId, id).eq(UploadFile::getType, type)
                .eq(UploadFile::getProjectId, projectId).last("LIMIT 1").one();
    }

    @Override
    public List<UploadFile> getFile(List<Long> idList, UploadTypeEnum type, Long projectId) {
        return this.lambdaQuery().in(CollUtil.isNotEmpty(idList), UploadFile::getId, idList).eq(UploadFile::getType, type)
                .eq(UploadFile::getProjectId, projectId).list();
    }
}
