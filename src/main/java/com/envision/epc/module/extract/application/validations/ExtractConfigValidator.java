package com.envision.epc.module.extract.application.validations;

import com.envision.epc.infrastructure.util.MsgUtils;
import com.envision.epc.module.extract.application.dtos.ExtractConfigDTO;
import com.envision.epc.module.extract.application.dtos.ParameterConfigDTO;
import com.envision.epc.module.extract.application.dtos.PrimitiveConfigDTO;
import com.envision.epc.module.extract.domain.ParameterTypeEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.List;

/**
 * @author wenjun.gu
 * @since 2025/9/15-12:01
 */
@EnableConfigurationProperties(ExtractValidation.class)
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class ExtractConfigValidator implements ConstraintValidator<ValidExtractConfig, ExtractConfigDTO> {

    private final ExtractValidation extractValidation;

    @Override
    public boolean isValid(ExtractConfigDTO value, ConstraintValidatorContext context) {
        context.disableDefaultConstraintViolation();
        List<ParameterConfigDTO> parameterConfigs = value.getParameterConfigs();
        if (parameterConfigs == null || parameterConfigs.isEmpty()) {
            context.buildConstraintViolationWithTemplate("{parameter.config.empty.error}").addConstraintViolation();
            return false;
        }
        if (parameterConfigs.stream().filter(x -> x.getType() == ParameterTypeEnum.SINGLE).count() > extractValidation.getMaxSingleCount()) {
            context.buildConstraintViolationWithTemplate(MsgUtils.getMessage("max.single.field",
                    extractValidation.getMaxSingleCount())).addConstraintViolation();
            return false;
        }
        if (parameterConfigs.stream().filter(x -> x.getType() == ParameterTypeEnum.COMPOSITE).count() > extractValidation.getMaxCompositeCount()) {
            context.buildConstraintViolationWithTemplate(MsgUtils.getMessage("max.composite.field",
                    extractValidation.getMaxCompositeCount())).addConstraintViolation();
            return false;
        }
        for (ParameterConfigDTO parameterConfig : parameterConfigs) {
            if (parameterConfig == null) {
                context.buildConstraintViolationWithTemplate("{parameter.config.empty.error}").addConstraintViolation();
                return false;
            }
            if (parameterConfig.getType() == ParameterTypeEnum.SINGLE) {
                List<PrimitiveConfigDTO> primitiveConfigs = parameterConfig.getPrimitiveConfigs();
                if (primitiveConfigs == null || primitiveConfigs.size() != 1) {
                    context.buildConstraintViolationWithTemplate("{single.config.error}").addConstraintViolation();
                    return false;
                }
                if (!fieldValid(primitiveConfigs.get(0), context)) {
                    return false;
                }
            } else {
                if (parameterConfig.getName() == null || parameterConfig.getName().isEmpty()) {
                    context.buildConstraintViolationWithTemplate("{composite.config.name.empty.error}").addConstraintViolation();
                    return false;
                }

                if (parameterConfig.getPrimitiveConfigs().size() > extractValidation.getMaxPrimitiveCount()) {
                    context.buildConstraintViolationWithTemplate(MsgUtils.getMessage("max.composite.inner.field",
                            extractValidation.getMaxPrimitiveCount())).addConstraintViolation();
                    return false;
                }

                for (PrimitiveConfigDTO primitiveConfig : parameterConfig.getPrimitiveConfigs()) {
                    if (!fieldValid(primitiveConfig, context)) {
                        return false;
                    }
                }

            }
        }
        return true;
    }

    private Boolean fieldValid(PrimitiveConfigDTO configDTO, ConstraintValidatorContext context) {
        if (configDTO.getName() == null || configDTO.getName().isBlank()) {
            context.buildConstraintViolationWithTemplate("{primitive.config.name.empty.error}").addConstraintViolation();
            return false;
        }
        if (configDTO.getName().length() > extractValidation.getMaxFieldLength()) {
            context.buildConstraintViolationWithTemplate(MsgUtils.getMessage("max.field.limit",
                    extractValidation.getMaxFieldLength())).addConstraintViolation();
            return false;
        }
        if (configDTO.getDescription().length() > extractValidation.getMaxDescriptionLength()) {
            context.buildConstraintViolationWithTemplate(MsgUtils.getMessage("max.description.limit",
                    extractValidation.getMaxDescriptionLength())).addConstraintViolation();
            return false;
        }
        return true;
    }

}
