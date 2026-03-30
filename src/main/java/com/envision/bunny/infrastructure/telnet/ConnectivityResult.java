package com.envision.bunny.infrastructure.telnet;

import lombok.Data;

/**
 * @author chaoyue.zhao1
 * @since 2026/02/10-15:25
 */
@Data
public class ConnectivityResult {

    private String host;
    private int port;
    private String protocol;

    /** 网络层（DNS + 路由） */
    private boolean networkReachable;

    /** 传输层（TCP connect） */
    private boolean portOpen;

    /** 应用层（HTTP / HTTPS） */
    private Boolean serviceAvailable;

    /** 失败原因 */
    private String reason;

}
