package com.envision.epc.infrastructure.crypto.param;

import cn.hutool.crypto.CryptoException;
import com.envision.epc.infrastructure.crypto.CryptoUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.format.Formatter;

import java.util.Locale;

/**
 * @author jingjing.dong
 * @since 2023/4/2-13:09
 */

public class LongDecrypt implements Formatter<Long> {

    @NotNull
    @Override
    public Long parse(@NotNull String text, @NotNull Locale locale) {
        try {
            return Long.valueOf(CryptoUtils.aes().decryptStr(text));
        } catch (CryptoException ex) {
            return Long.valueOf(text);
        }
    }

    @Override
    @NotNull
    public String print(@NotNull Long object, @NotNull Locale locale) {
        return String.valueOf(object);
    }
}