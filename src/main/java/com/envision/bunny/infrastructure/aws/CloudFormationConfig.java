package com.envision.bunny.infrastructure.aws;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudformation.CloudFormationClient;

/**
 * CloudFormation 客户端配置
 */
@Configuration
public class CloudFormationConfig {

    @Bean
    public CloudFormationClient cloudFormationClient(
            @Value("${cloud.aws.region.static:ap-southeast-1}") String region) {

        return CloudFormationClient.builder()
                .region(Region.of(region))
                .build();
    }
}
