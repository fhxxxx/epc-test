package com.envision.bunny.infrastructure.crypto.param;

import cn.hutool.crypto.CryptoException;
import com.envision.bunny.infrastructure.crypto.CryptoUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.format.Formatter;

import java.util.Locale;

/**
 * @author jingjing.dong
 * @since 2023/4/2-13:09
 */

public class IntegerDecrypt implements Formatter<Integer> {

    @Override
    @NotNull
    public Integer parse(@NotNull String text, @NotNull Locale locale) {
        try {
            return Integer.valueOf(CryptoUtils.aes().decryptStr(text));
        } catch (CryptoException ex){
            return Integer.valueOf(text);
        }
    }

    @Override
    @NotNull
    public String print(@NotNull Integer object, @NotNull Locale locale) {
        return String.valueOf(object);
    }
}