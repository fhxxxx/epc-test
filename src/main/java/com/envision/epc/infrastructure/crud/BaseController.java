package com.envision.epc.infrastructure.crud;


import cn.hutool.core.net.URLEncodeUtil;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.CharsetUtil;
import com.alibaba.excel.EasyExcelFactory;
import com.alibaba.excel.write.style.column.LongestMatchColumnWidthStyleStrategy;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.envision.epc.infrastructure.mybatis.AuditingEntity;
import com.envision.epc.infrastructure.mybatis.BasicPagination;
import com.envision.epc.infrastructure.mybatis.BasicPaging;
import com.envision.epc.infrastructure.util.Pair;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotNull;
import org.apache.poi.ss.formula.functions.T;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;


import java.io.IOException;
import java.util.List;

/**
 * 核心公共类
 * @author jingjing.dong
 * @since 2023/11/13-13:49
 */
@Validated
public class BaseController<S extends IService<E>, E extends AuditingEntity> {

    @Autowired
    protected S baseService;

    Class<T> e;

    @PostMapping
    public boolean insert(@RequestBody E entity) {
        return baseService.save(entity);
    }

    @DeleteMapping("/{ids}")
    public boolean delete(@PathVariable List<Long> ids) {
        return baseService.removeByIds(ids);
    }
    @PutMapping("/{id}")
    public boolean updateById(@NotNull @PathVariable Long id, @RequestBody E entity) {
//        entity.setId(id);
        return baseService.updateById(entity);
    }

    @GetMapping("/{id}")
    public E getById(@PathVariable Long id) {
        return baseService.getById(id);
    }

    @PutMapping
    public boolean save(@RequestBody E entity) {
        return baseService.saveOrUpdate(entity);
    }

    @GetMapping("/list")
    public List<E> list(E entity) {
        QueryWrapper<E> queryWrapper = ApprenticeUtil.getQueryWrapper(entity);
        return baseService.list(queryWrapper);
    }

    @GetMapping
    public BasicPagination<E> page(BasicPaging pagingQuery, E entity) {
        QueryWrapper<E> queryWrapper = ApprenticeUtil.getQueryWrapper(entity);
        //限制条件
        if (pagingQuery.getPageNum() < 1) {
            pagingQuery.setPageNum(1);
        }

        if (pagingQuery.getPageSize() > 100) {
            pagingQuery.setPageSize(100);
        }
        Page<E> page = new Page<>(pagingQuery.getPageNum(), pagingQuery.getPageSize());
        if (pagingQuery.needSort()) {
            final List<Pair<String, String>> allOrderPair = pagingQuery.getAllOrder();
            allOrderPair.forEach(pair -> {
                String tableField = ApprenticeUtil.getBbField(pair.getKey(),entity);
                if (CharSequenceUtil.equals(pair.getValue(),"+")){
                    queryWrapper.orderByAsc(tableField);
                } else {
                    queryWrapper.orderByDesc(tableField);
                }
            });
        }
        return BasicPagination.of(baseService.page(page, queryWrapper));
    }

    @GetMapping("/count")
    public long count(E entity) {
        QueryWrapper<E> queryWrapper = ApprenticeUtil.getQueryWrapper(entity);
        return baseService.count(queryWrapper);
    }

    @PostMapping("/upload")
    public String upload(MultipartFile file) throws IOException {
        EasyExcelFactory.read(file.getInputStream(), baseService.getEntityClass(), new CommonReadListener(baseService)).sheet().doRead();
        return "上传成功";
    }

    @GetMapping("/download")
    public void download(HttpServletResponse response) throws IOException {
        // 这里注意 有同学反应使用swagger 会导致各种问题，请直接用浏览器或者用postman
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        // 这里URLEncoder.encode可以防止中文乱码 当然和easyexcel没有关系
        String fileName = URLEncodeUtil.encode(baseService.getEntityClass().getSimpleName(),  CharsetUtil.CHARSET_UTF_8).replace("+", "%20");
        response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");
        EasyExcelFactory.write(response.getOutputStream(), baseService.getEntityClass()).registerWriteHandler(new LongestMatchColumnWidthStyleStrategy())
                .sheet("数据内容").doWrite(baseService.list());

    }
}
