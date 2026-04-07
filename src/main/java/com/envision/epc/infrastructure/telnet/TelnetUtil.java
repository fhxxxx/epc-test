package com.envision.epc.infrastructure.telnet;

import cn.hutool.http.HttpRequest;

import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * @author chaoyue.zhao1
 * @since 2026/02/10-15:17
 */
public class TelnetUtil {


    /**
     * Telnet 端口检测
     *
     * @param host    主机
     * @param port    端口
     * @param timeout 超时时间（毫秒）
     * @return 是否可连通
     */
    public static ConnectivityResult check(String host, int port, String protocol, int timeout) {
        ConnectivityResult result = new ConnectivityResult();
        result.setHost(host);
        result.setPort(port);
        result.setProtocol(protocol);

        // ---------- L3 网络层 ----------
        try {
            result.setNetworkReachable(true);
        } catch (Exception e) {
            result.setNetworkReachable(false);
            result.setReason("DNS resolve failed");
            return result;
        }

        // ---------- L4 端口层 ----------
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), timeout);
            result.setPortOpen(true);
        } catch (Exception e) {
            result.setPortOpen(false);
            result.setReason("TCP connect failed: " + e.getMessage());
            return result;
        }

        // ---------- L7 应用层 ----------
        if ("HTTP".equalsIgnoreCase(protocol) || "HTTPS".equalsIgnoreCase(protocol)) {
            try {
                String url = protocol.toLowerCase() + "://" + host + ":" + port;
                boolean ok = HttpRequest.head(url)
                        .timeout(timeout)
                        .execute()
                        .isOk();
                result.setServiceAvailable(ok);
                if (!ok) {
                    result.setReason("HTTP response not OK");
                }
            } catch (Exception e) {
                result.setServiceAvailable(false);
                result.setReason("HTTP request failed: " + e.getMessage());
            }
        }

        return result;
    }
}
