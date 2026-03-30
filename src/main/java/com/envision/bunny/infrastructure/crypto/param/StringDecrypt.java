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

public class StringDecrypt implements Formatter<String> {

    @Override
    @NotNull
    public String parse(@NotNull String text, @NotNull Locale locale) {
        try {
            return CryptoUtils.aes().decryptStr(text);
        } catch (CryptoException ex){
            return text;
        }
    }

    @Override
    @NotNull
    public String print(@NotNull String object, @NotNull Locale locale) {
        return object;
    }
}