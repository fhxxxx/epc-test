package com.envision.bunny.demo.integration.log;


import com.envision.bunny.infrastructure.audit.constants.OperateType;
import io.github.flyhero.easylog.annotation.EasyLog;
import org.hibernate.validator.constraints.Length;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author liang.liu
 * @since 2025/03/04
 */
@RestController
@RequestMapping("/log")
public class EasyLogTest {

    @EasyLog(module = "测试", type = OperateType.SELECT, success = "测试查询{{#name}}信息")
    @GetMapping("/test")
    public String test(@RequestParam @Length(max = 3, message = "删除原因不能超过3个字符") String name) {
        System.out.println("姓名"+ name);
        return name+"124124";
    }

    @EasyLog(module = "测试2", type = OperateType.SELECT, success = "'测试'+ #name", fail = "新增失败：{{#_errMsg}}")
    @GetMapping("/test2")
    public String test2(@RequestParam String name) {
        if ("easylog".equalsIgnoreCase(name)) {
            throw new RuntimeException("测试异常");
        }
        return name;
    }

    @EasyLog(module = "测试3", type = OperateType.ADD, success = "'测试'+ #name",bizNo = "订单id：{{#id}}", fail = "新增失败：{{#_errMsg}}")
    @GetMapping("/test3")
    public String test3(@RequestParam String name,Long id) {
        System.out.println("订单id"+ id);
        return name;
    }

    @EasyLog(module = "测试4", type = OperateType.UPDATE, success = "'测试'+ #name",bizNo = "订单id：{{#id}}",detail = "'这是额外的备注'", fail = "新增失败：{{#_errMsg}}")
    @GetMapping("/test4")
    public String test4(@RequestParam String name,Long id) {
        System.out.println("订单id"+ id);
        return name;
    }

}
