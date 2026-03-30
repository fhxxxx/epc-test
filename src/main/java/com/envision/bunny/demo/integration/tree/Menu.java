package com.envision.bunny.demo.integration.tree;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class Menu {
    private Long id;
    private Long parentId;
    private String name;
    private List<Menu> children;

    public Menu(Long id, Long parentId, String name) {
        this.id = id;
        this.parentId = parentId;
        this.name = name;
        this.children = new ArrayList<>();
    }

}
