package com.envision.epc.infrastructure.telnet;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author chaoyue.zhao1
 * @since 2026/02/10-14:17
 */
@RestController
public class TelnetController {

    /**
     * Telnet 端口检测
     * @param host    主机
     * @param port    端口
     * @param timeout 超时时间（毫秒）
     * @return 是否可连通
     */
    @GetMapping("/telnet/check")
    public ConnectivityResult check(@RequestParam String host, @RequestParam int port, @RequestParam(defaultValue = "TCP") String protocol, @RequestParam(defaultValue = "3000") int timeout) {
        return TelnetUtil.check(host, port, protocol, timeout);
    }
}
