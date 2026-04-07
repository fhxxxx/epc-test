package com.envision.epc.infrastructure.util;

import com.envision.epc.infrastructure.response.BizException;
import com.envision.epc.infrastructure.response.ErrorCode;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;

import java.io.IOException;
import java.io.StringReader;
import java.util.Map;

/**
 * @author jingjing.dong
 * @since 2021/6/7-15:21
 */
public class FreeMarkerUtils {
    /**
     * 根据模板字符串和传入的参数映射渲染后，输出最终的字符串
     *
     * @param name    自定义模板名称
     * @param content 传入的字符串模板
     * @param params  参数映射对象，此处限定为map
     * @return 渲染后的完整字符串
     */
    public static String parseStringTpl(String name, String content, Map<String, String> params) {
        try {
            Template template = new Template(name, new StringReader(content), new Configuration(Configuration.DEFAULT_INCOMPATIBLE_IMPROVEMENTS), "UTF-8");
            return FreeMarkerTemplateUtils.processTemplateIntoString(template, params);
        } catch (IOException | TemplateException ex) {
            throw new BizException(ErrorCode.PROCESS_TEMPLATE_ERROR, name);
        }
    }

    /**
     * 根据模板名和传入的参数映射渲染后，输出最终的字符串
     *
     * @param viewName 模板名称
     * @param model    传入的参数映射，可以是Map ，也可以是对象
     * @return render后的字符串
     */
    public static String parseTpl(String viewName, Object model) {
        try {
            Configuration cfg = ApplicationContextUtils.getBean(Configuration.class);
            Template template = cfg.getTemplate(viewName + ".ftl");
            return FreeMarkerTemplateUtils.processTemplateIntoString(template, model);
        } catch (IOException | TemplateException e) {
            throw new BizException(ErrorCode.PROCESS_TEMPLATE_ERROR, viewName);
        }
    }

}