package com.envision.bunny.demo.scenario.service;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * @author jingjing.dong
 * @since 2021/3/24-19:31
 */
@Setter
@Getter
@ToString
public class Person {
    /**
     * 姓名
     * @mock 王娟娟
     */
    String name;

    /**
     * 年龄
     * @mock 19
     */
    Integer age;
//    public void setName(String name)
//    {
//        this.name = name;
//    }
//
//    public String getName()
//    {
//        return this.name;
//    }
    public Person(String name){
   this.name = name;
    }
    public Person(){
    }
    public static Person fromPltData(JsonNode attributes) {
        String userId = attributes.get("id_casp").asText();
        Person obj = new Person();
        obj.setName(userId);
        return obj;
    }
}
