package com.envision.epc.infrastructure.util;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class ListUtils {
   public static <T> List<T> filter(List<T> list,Predicate<T> predicate){
      return list.stream().filter(predicate).collect(Collectors.toList());
   }

   public static <T,K,V> Map<K,V> toMap(List<T> list, Function<T, K> keyFunc, Function<T, V> valueFunc){
      return list.stream().collect(Collectors.toMap(keyFunc, valueFunc));
   }
   public static <K,V> Map<K, V> toMap(List<K> list, Function<K, V> keyFunc){
      return list.stream().collect(Collectors.toMap(Function.identity(), keyFunc));
   }
   public static <T> List<T> of(T... ts) {
      if (Objects.isNull(ts)||ts.length==0) {
          return  Collections.emptyList();
      }
      return Collections.unmodifiableList(Arrays.asList(ts));
   }
   
}