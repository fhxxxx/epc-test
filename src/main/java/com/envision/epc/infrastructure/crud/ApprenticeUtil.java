package com.envision.epc.infrastructure.crud;

import cn.hutool.core.date.LocalDateTimeUtil;
import cn.hutool.core.text.CharSequenceUtil;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.envision.epc.infrastructure.response.BizException;
import com.envision.epc.infrastructure.response.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.ObjectUtils;
import org.springframework.util.ReflectionUtils;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Apprentice系统Util
 *
 * @author jingjing.dong
 * @since 2023/11/13-13:55
 * @since JDK 1.8.0
 */
@Slf4j
public class ApprenticeUtil {

    private static final Pattern HUMP_PATTERN = Pattern.compile("[A-Z]");
    private static final Pattern LINE_PATTERN = Pattern.compile("_(\\w)");

    /**
     * 驼峰转下划线
     */
    private static String humpToLine(String str) {
        Matcher matcher = HUMP_PATTERN.matcher(str);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(sb, "_" + matcher.group(0).toLowerCase());
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    /**
     * 获取DB 字段名称
     */
    public static <E> String getBbField(String column, E entity) {
        Field[] fields = entity.getClass().getDeclaredFields();
        Field[] superFields = entity.getClass().getSuperclass().getDeclaredFields();
        Stream<Field> allFields = Stream.concat(Arrays.stream(fields), Arrays.stream(superFields));
        final Optional<Field> fieldOpt = allFields.filter(field -> CharSequenceUtil.equals(field.getName(), column)).findFirst();
        if (fieldOpt.isPresent()) {
            return getBbField(fieldOpt.get());
        }
        throw new BizException(ErrorCode.INVALID_INPUT, column);
    }

    /**
     * 获取DB 字段名称
     */
    public static <E> String getBbField(String column, Class<?> clz) {
        Field[] fields = clz.getDeclaredFields();
        Field[] superFields = clz.getSuperclass().getDeclaredFields();
        Stream<Field> allFields = Stream.concat(Arrays.stream(fields), Arrays.stream(superFields));
        final Optional<Field> fieldOpt = allFields.filter(field -> CharSequenceUtil.equals(field.getName(), column)).findFirst();
        if (fieldOpt.isPresent()) {
            return getBbField(fieldOpt.get());
        }
        throw new BizException(ErrorCode.INVALID_INPUT, column);
    }

    /**
     * 获取DB 字段名称
     */
    public static String getBbField(Field field) {
        if (field.isAnnotationPresent(TableField.class) && CharSequenceUtil.isNotEmpty(field.getAnnotation(TableField.class).value())) {
            return field.getAnnotation(TableField.class).value();
        }
        return humpToLine(field.getName());
    }


    /**
     * 下划线转驼峰
     */
    public static String lineToHump(String str) {
        str = str.toLowerCase();
        Matcher matcher = LINE_PATTERN.matcher(str);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(sb, matcher.group(1).toUpperCase());
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    /**
     * 获取QueryWrapper
     */
    public static <E> QueryWrapper<E> getQueryWrapper(E entity) {
        Field[] fields = entity.getClass().getDeclaredFields();
        Field[] superFields = entity.getClass().getSuperclass().getDeclaredFields();
        Stream<Field> allFields = Stream.concat(Arrays.stream(fields), Arrays.stream(superFields));
        QueryWrapper<E> eQueryWrapper = new QueryWrapper<>();
        allFields.filter(field -> !Modifier.isFinal(field.getModifiers())).filter(field -> !field.isSynthetic())
                .forEach(field -> processField(entity, eQueryWrapper, field));
        return eQueryWrapper;

    }

    private static <E> void processField(E entity, QueryWrapper<E> eQueryWrapper, Field field) {
        field.setAccessible(true);
        try {
            Object obj = field.get(entity);
            if (ObjectUtils.isEmpty(obj)) {
                return;
            }
            if (CharSequenceUtil.equals("deleted", field.getName()) && field.isAnnotationPresent(TableLogic.class)) {
                return;
            }
            String name = ApprenticeUtil.getBbField(field);
            if (field.getType() == String.class) {
                if (field.isAnnotationPresent(EqualMatch.class)) {
                    eQueryWrapper.eq(name, obj);
                } else {
                    eQueryWrapper.like(name, obj);
                }
            } else if (field.getType().isPrimitive() || Number.class.isAssignableFrom(field.getType()) || field.getType() == Boolean.class || field.getType().isEnum()) {
                eQueryWrapper.eq(name, obj);
            } else if (field.getType() == LocalDateTime.class) {
                LocalDateTime localDateTime = (LocalDateTime) obj;
                eQueryWrapper.eq(name, LocalDateTimeUtil.formatNormal(localDateTime));
            } else if (field.getType() == LocalDate.class) {
                LocalDate localDate = (LocalDate) obj;
                eQueryWrapper.eq(name, LocalDateTimeUtil.formatNormal(localDate));
            } else {
                eQueryWrapper.like(name, obj);
            }

        } catch (IllegalAccessException e) {
            log.error(e.getMessage(), e);
        }
    }


    /**
     * 反射获取字段值
     * [entity, value] value 值为 "id" "name" 等
     */
    public static <E> Object getValueForClass(E entity, String value) {
        Field id;
        PropertyDescriptor pd = null;
        try {
            id = entity.getClass().getDeclaredField(value);
            pd = new PropertyDescriptor(id.getName(), entity.getClass());
        } catch (NoSuchFieldException | IntrospectionException e) {
            log.error(e.getMessage(), e);
        }
        //获取get方法
        Method getMethod = Objects.requireNonNull(pd).getReadMethod();
        return ReflectionUtils.invokeMethod(getMethod, entity);
    }
}