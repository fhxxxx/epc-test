package com.envision.bunny.demo.integration.plt;

import com.envision.bunny.demo.scenario.service.Person;
import com.envision.bunny.facade.platform.PlatformRemote;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 数据湖
 *
 * @author jingjing.dong
 * @since 2021/4/10-13:39
 */
@RestController
@RequestMapping("/plt")
public class PltTest {
    @Autowired
    PlatformRemote platformRemote;

    /**
     * 同步人员
     *
     * @return java.lang.String
     * @author jingjing.dong
     */
    @GetMapping("/sync")
    public String test(){
        String url = "https://platform-dev.envision-io" +
                ".com/apis/hr/is/datalakeinternal/hr_is_users?filter[department_name]=智慧运营技术部&page[limit]=5&page" +
                "[offset]=0&fields=id_casp,work_start_date,hiredate,challenge_level,position_name,highest_degree," +
                "caspian_delete_flag,caspian_update_date";
        List<Person> list = platformRemote.fetchFromDataLake("hr-is-datalakeinternal", url, Person::fromPltData);
        return "Size: "+ list.size();
    }
}
