package com.envision.bunny.infrastructure.filter.upload;

import cn.hutool.core.io.FileTypeUtil;
import cn.hutool.core.io.file.FileNameUtil;
import cn.hutool.core.text.CharSequenceUtil;
import com.envision.bunny.infrastructure.response.BizException;
import com.envision.bunny.infrastructure.response.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * @author jingjing.dong
 * @since 2021/3/26-10:07
 */
@Aspect
@Component
@Slf4j
public class UploadFileAspect {
    /**
     * 进行幂等检查的切面
     */
    @Before("@annotation(com.envision.bunny.infrastructure.filter.upload.UploadFileType)")
    public void doBefore(JoinPoint jPoint) throws BizException {
        //获取当前方法信息
        Method method = ((MethodSignature) jPoint.getSignature()).getMethod();
        //获取注解
        UploadFileType validateFileType = method.getAnnotation(UploadFileType.class);
        //获取参数的所有值。
        Object[] args = jPoint.getArgs();
        for (Object arg : args) {
            if (arg instanceof MultipartFile) {
                MultipartFile file = (MultipartFile) arg;
                final String fileName = file.getOriginalFilename();
                try (InputStream in = file.getInputStream()) {
                    final String fileType = FileTypeUtil.getType(in, fileName);
                    final String extName = FileNameUtil.extName(fileName);
                    log.info("要求的类型[{}],上传的文件头类型[{}],文件后缀[{}]", Arrays.toString(validateFileType.fileType()), fileType,extName);
                    if (!Arrays.asList(validateFileType.fileType()).contains(fileType) || !Arrays.asList(validateFileType.fileType()).contains(extName)) {
                        throw new BizException(ErrorCode.UPLOAD_FILE_NOT_VALIDATE,Arrays.toString(validateFileType.fileType()));
                    }
                } catch (IOException e) {
                    throw new BizException(ErrorCode.INTERNAL_SERVER_ERROR, "上传文件IO出错");
                } catch (Exception e) {
                    throw new BizException(ErrorCode.UPLOAD_FILE_NOT_VALIDATE, CharSequenceUtil.format("上传文件校验失败，允许的类型[{}]", Arrays.toString(validateFileType.fileType())));
                }
            }
        }
    }

}

