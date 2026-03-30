package com.envision.bunny.module.aws.web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import software.amazon.awssdk.services.cloudformation.CloudFormationClient;
import software.amazon.awssdk.services.cloudformation.model.CloudFormationException;
import software.amazon.awssdk.services.cloudformation.model.DescribeStacksResponse;
import software.amazon.awssdk.services.cloudformation.model.Output;
import software.amazon.awssdk.services.cloudformation.model.Stack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 配置控制器
 * <p>
 * 负责查询和展示应用配置信息
 *
 * @author example
 * @version 1.0.0
 */
@RestController
@RequestMapping("/api/config")
public class ConfigController {

    @Autowired(required = false)
    private ConfigurableEnvironment environment;

    @Autowired(required = false)
    private CloudFormationClient cloudFormationClient;

    /**
     * 获取 CloudFormation Stack 的所有 Outputs
     * <p>
     * 该接口用于查询CloudFormation堆栈的输出配置
     * <p>
     * 使用示例：
     * GET /api/config/cloudformation/outputs
     *
     * @return CloudFormation堆栈输出信息
     */
    @GetMapping("/cloudformation/outputs")
    public Map<String, Object> getCloudFormationOutputs() {
        Map<String, Object> result = new HashMap<>();

        try {
            // 1. 从配置中读取 stack name
            String stackName = environment.getProperty("spring.cloud.aws.cloudformation.stack-name");

            if (stackName == null || stackName.trim().isEmpty()) {
                result.put("error", "CloudFormation stack name not configured");
                result.put("configKey", "spring.cloud.aws.cloudformation.stack-name");
                return result;
            }

            // 2. 调用 CloudFormation API
            DescribeStacksResponse response = cloudFormationClient.describeStacks(
                    builder -> builder.stackName(stackName)
            );

            // 3. 提取所有 outputs
            List<Map<String, String>> outputs = new ArrayList<>();
            if (response.hasStacks() && response.stacks().size() > 0) {
                Stack stack = response.stacks().get(0);
                if (stack.hasOutputs()) {
                    for (Output output : stack.outputs()) {
                        Map<String, String> outputMap = new HashMap<>();
                        outputMap.put("outputKey", output.outputKey());
                        outputMap.put("outputValue", output.outputValue());
                        if (output.description() != null) {
                            outputMap.put("description", output.description());
                        }
                        outputs.add(outputMap);
                    }
                }
            }

            // 4. 返回结果
            result.put("stackName", stackName);
            result.put("outputs", outputs);
            result.put("outputCount", outputs.size());

        } catch (CloudFormationException e) {
            result.put("error", "CloudFormation API error");
            result.put("message", e.getMessage());
            result.put("statusCode", e.statusCode());
        } catch (Exception e) {
            result.put("error", "Internal server error");
            result.put("message", e.getMessage());
        }

        return result;
    }
}
