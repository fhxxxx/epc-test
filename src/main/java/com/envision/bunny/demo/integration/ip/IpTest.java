package com.envision.bunny.demo.integration.ip;

import com.envision.bunny.facade.ip.IpRemote;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


/**
 * @author chaoyue.zhao1
 * @since 2025/12/19-14:47
 */
@RestController
@RequestMapping("/ip")
public class IpTest {
    @Autowired
    private IpRemote ipRemote;

    @GetMapping("/test")
    public boolean test(HttpServletRequest request){
        return ipRemote.checkIpInVdi(request);
    }
}
