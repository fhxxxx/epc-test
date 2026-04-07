package com.envision.epc.infrastructure.util;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.web.server.ResponseStatusException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @author jingjing.dong
 * @since 2020/11/25-18:04
 */
public class JsonUtils {
  private static final ObjectMapper JSON = new ObjectMapper();
  static {
    JSON.setSerializationInclusion(Include.NON_NULL);
    JSON.configure(SerializationFeature.INDENT_OUTPUT, Boolean.TRUE);
    JSON.registerModule(new JavaTimeModule());
  }

  public static String toJsonStr(Object obj) {
    try {
      return JSON.writeValueAsString(obj);
    } catch (JsonProcessingException e) {
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage(), e.getCause());
    }
  }

  public static JsonNode toJsonNode(String str) {
    try {
      return JSON.readTree(str);
    } catch (JsonProcessingException e) {
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage(), e.getCause());
    }
  }
  public static JsonNode toJsonNode(Object obj) {
    return JSON.valueToTree(obj);
  }
  public static <T> T toObjValue(String str,Class<T> clz) {
    try {
      return JSON.readValue(str,clz);
    } catch (JsonProcessingException e) {
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage(), e.getCause());
    }
  }
  public static <T> List<T> toObjValues(ArrayNode arr,Class<T> clz) {
    try {
      List<T> lists = new ArrayList<>();
      for (JsonNode x : arr) {
        lists.add(JSON.treeToValue(x, clz));
      }
      return lists;
    } catch (JsonProcessingException e) {
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage(), e.getCause());
    }
  }
  public static <T> T toObjValue(File file, Class<T> clz) {
    try {
      return JSON.readValue(file, clz);
    } catch (IOException e) {
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage(), e.getCause());
    }
  }
  public static <T> T toObjValue(JsonNode node, Class<T> clz) {
    try {
      return JSON.treeToValue(node, clz);
    } catch (IOException e) {
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage(), e.getCause());
    }
  }
  public static File toTempFile(String prefix, String suffix, Object obj) {
    try {
      File tempFile = File.createTempFile(prefix, suffix);
      JSON.writeValue(tempFile, obj);
      return tempFile;
    } catch (IOException e) {
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage(), e.getCause());
    }
  }

  /**
   * json字符串转成list
   *
   * @param jsonString json字符串
   * @param cls        class
   * @return List对象
   */
  public static <T> List<T> toObjList(@NonNull String jsonString, Class<T> cls) {
    try {
      return JSON.readValue(jsonString, getCollectionType(List.class, cls));
    } catch (JsonProcessingException e) {
      String className = cls.getSimpleName();
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage(), e.getCause());
    }
  }

  /**
   * json字符串转成Set
   *
   * @param jsonString json字符串
   * @param cls        class
   * @return Set对象
   */
  public static <T> Set<T> toObjSet(@NonNull String jsonString, Class<T> cls) {
    try {
      return JSON.readValue(jsonString, getCollectionType(Set.class, cls));
    } catch (JsonProcessingException e) {
      String className = cls.getSimpleName();
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage(), e.getCause());
    }
  }

  /**
   * 获取泛型的Collection Type
   *
   * @param collectionClass 泛型的Collection
   * @param elementClasses  实体bean
   * @return JavaType Java类型
   */
  private static JavaType getCollectionType(Class<?> collectionClass, Class<?>... elementClasses) {
    return JSON.getTypeFactory().constructParametricType(collectionClass, elementClasses);
  }


}
