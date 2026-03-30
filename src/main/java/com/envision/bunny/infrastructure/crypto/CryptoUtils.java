package com.envision.bunny.infrastructure.crypto;

import cn.hutool.crypto.symmetric.AES;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;


import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

/**
 * @author jingjing.dong
 * @since 2023/4/3-11:25
 */
@Component
public class CryptoUtils {
    private static AES aes;

    @Value("${custom.crypto.secret-key}")
    private String secretKey;

    @PostConstruct
    public void init() {
        CryptoUtils.aes = new AES(generateFinalAESKey(this.secretKey));
    }

    private static SecretKeySpec generateFinalAESKey(String key) {
        final byte[] finalKey = new byte[16];
        int i = 0;
        for (byte b : key.getBytes(StandardCharsets.UTF_8)) {
            finalKey[i++ % 16] ^= b;
        }
        return new SecretKeySpec(finalKey, "AES");
    }

    public static AES aes(){
        return aes;
    }

}

