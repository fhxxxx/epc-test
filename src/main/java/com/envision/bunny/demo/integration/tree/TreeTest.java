package com.envision.bunny.demo.integration.tree;

import cn.hutool.core.lang.tree.TreeUtil;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

/**
 * @author chaoyue.zhao1
 * @since 2026/02/02-17:36
 */
@RestController
@RequestMapping("/tree")
public class TreeTest {
    private static final List<Menu> menuList = new ArrayList<>();
    static {
        menuList.add(new Menu(1L, 0L, "菜单1"));
        menuList.add(new Menu(2L, 0L, "菜单2"));
        menuList.add(new Menu(3L, 1L, "子菜单1-1"));
        menuList.add(new Menu(4L, 1L, "子菜单1-2"));
        menuList.add(new Menu(5L, 2L, "子菜单1-2"));
        menuList.add(new Menu(6L, 3L, "子菜单1-2"));
        menuList.add(new Menu(7L, 3L, "子菜单1-2"));
        menuList.add(new Menu(8L, 4L, "子菜单1-2"));
        menuList.add(new Menu(9L, 4L, "子菜单1-2"));
        menuList.add(new Menu(10L, 8L, "子菜单1-2"));
        menuList.add(new Menu(11L, 8L, "子菜单1-2"));
        menuList.add(new Menu(12L, 10L, "子菜单1-2"));
    }

    @GetMapping("/test")
    public List<Menu> treeTest(Long parentId){
        return TreeUtil.build(
                menuList,
                parentId,                     // 设置根节点的 id
                Menu::getId,                   // 获取 id
                Menu::getParentId,             // 获取 parentId
                Menu::setChildren              // 设置 children 列表
        );
    }
}
