package com.envision.epc.module.user.infrastructure;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.Arrays;
import java.util.List;

/**
 * @author yakun.meng
 * @since 2024/5/6
 */
@NoArgsConstructor(access= AccessLevel.PRIVATE)
public class Constant {
    //数据湖员工信息服务标记
    public static final String OA_USER_SVC = "it-oa-datalakeuserl2";
    public static final String OA_USER_REQ_PATH_PATTERN = "/apis/it/oa/datalakeuserl2/it_oa_userlevels_l2?filter[emptype][NEQ]=0&filter[caspian_run_date][EQ]={}&fields=empno,empname,bunitno,divname,loginname,deptname,deptno,f034,hrstatus&page[limit]=1000&page[offset]=0";
    public static final String ALL_OA_USER_REQ_PATH_PATTERN = "/apis/it/oa/datalakeuserl2/it_oa_userlevels_l2?filter[hrstatus][EQ]=在职&fields=empno,empname,bunitno,divname,loginname,deptname,deptno,f034,hrstatus&page[limit]=1000&page[offset]=0";
    public static final List<String> LOCALES = Arrays.asList("zh_CN", "en_US");
    public static final String AVATAR_PREFIX = "https://platform.envisioncn.com/apps/it/lightning/photo/";
    public static final String AVATAR_SUFFIX = "_64.jpg";
}
