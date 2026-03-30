package com.envision.bunny.demo.support.model;

import com.envision.bunny.demo.support.model.Customer;
import com.envision.bunny.demo.support.model.CustomerService;
import com.envision.bunny.infrastructure.web.AllInOne;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author jingjing.dong
 * @since 2024/4/20-14:58
 */
@RestController
@RequestMapping("/all-in-one")
public class AllInOneDemo extends AllInOne<CustomerService,Customer> {
}
