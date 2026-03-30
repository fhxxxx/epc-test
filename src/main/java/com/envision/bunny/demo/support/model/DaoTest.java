package com.envision.bunny.demo.support.model;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.envision.bunny.demo.support.model.Customer;
import com.envision.bunny.demo.support.model.CustomerMapper;
import com.envision.bunny.demo.support.model.CustomerQueryService;
import com.envision.bunny.demo.support.model.CustomerService;
import com.envision.bunny.infrastructure.crypto.param.DecryptAnnotation;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.sql.DataSource;
import java.util.List;
import java.util.Objects;

/**
 * CRUD Data
 * @author jingjing.dong
 * @since 2021/4/26-15:31
 */
@RestController
@RequestMapping("/crud")
public class DaoTest {
    @Autowired
    CustomerMapper mapper;
    @Autowired
    CustomerService service;
    @Autowired
    SqlSessionFactory sqlSessionFactory;
    @Autowired
    DataSource dataSource;

    @Autowired
    CustomerQueryService queryService;

    @GetMapping("/common-list")
    public List<Customer> listTest(Customer customer) {
        return queryService.list(customer);
    }

    /**
     * insert数据
     *
     * @param firstName firstname
     * @param lastName  lastname
     * @return int
     * @author jingjing.dong
     * @since 2021/5/7 18:45
     */
    @GetMapping("/create/{id}/aa")
    public String insert(String firstName, @DecryptAnnotation String lastName, @DecryptAnnotation @PathVariable("id") Long id) {
        Customer customer = new Customer();
        customer.setFirstName(firstName);
        customer.setLastName(lastName);
        mapper.insert(customer);
        return "3";
    }

    @PostMapping("/create")
    public long insertByPost(@RequestBody Customer customer) {
        mapper.insert(customer);
        return customer.getCustomerId();
    }

    /**
     * 批量插入-mybatis
     *
     * @param firstName firstname
     * @param lastName  lastname
     * @return java.lang.String
     * @author jingjing.dong
     * @since 2021/5/7 18:46
     */
    @Transactional
    @GetMapping("/batch-mybatis")
    public String addBatch(String firstName, String lastName) {
        List<Customer> list = Lists.newArrayList();
        for (int i = 0; i < 50; i++) {
            Customer customer = new Customer();
            customer.setFirstName(firstName + i);
            customer.setLastName(lastName + i);
            list.add(customer);
        }
        SqlSession session = sqlSessionFactory.openSession(ExecutorType.BATCH, false);
        //ItemMapper mapper = session.getMapper(ItemMapper.class);
        for (int i = 0; i < list.size(); i++) {
            mapper.insert(list.get(i));
            //每500条提交一次防止内存溢出
            if (i % 500 == 499) {
                session.commit();
                session.clearCache();
            }
        }
        session.commit();
        session.clearCache();
        session.close();
        return "Success";
    }

    /**
     * 批量插入 mybatis plus
     * @param firstName firstname
     * @param lastName  lastname
     * @return boolean
     * @author jingjing.dong
     * @since 2021/5/7 18:46
     */
    @Transactional
    @GetMapping("/batch-plus")
    public boolean addBatch2(String firstName, String lastName) {

        List<Customer> list = Lists.newArrayList();
        for (int i = 0; i < 50; i++) {
            Customer customer = new Customer();
            customer.setFirstName(firstName + i);
            customer.setLastName(lastName + i);
            list.add(customer);
        }
        return service.saveBatch(list, 5);
    }
    /**
     * 取分页数据
     *
     * @param id       ID|513
     * @param lastName lastName|ei
     * @param size     页数|2
     * @param current  当前页|1
     */
    @GetMapping("/page")
    public List<Customer> read(@RequestParam(required = false) Integer id,
                               @RequestParam(value = "last-name", required = false) String lastName,
                               @RequestParam(required = false) Integer size,
                               @RequestParam(required = false) Integer current) {
        IPage<Customer> page = new Page<>(Objects.isNull(current) ? 1 : current, Objects.isNull(size) ? 2 : size);
        final IPage<Customer> pageResult = service.lambdaQuery()
                .eq(Objects.nonNull(id), Customer::getCustomerId, id)
                .like(StringUtils.isNotBlank(lastName), Customer::getLastName, lastName)
                .page(page);
        return pageResult.getRecords();
    }

    /**
     * 更新数据
     *
     * @param id    ID|513
     * @param score 分数|7
     */
    @GetMapping("/update")
    public Customer update(int id, int score) {
        final Customer customer = service.lambdaQuery().eq(Customer::getCustomerId, id).one();
        customer.setScore(score);
        service.updateById(customer);
        return customer;
    }
}
