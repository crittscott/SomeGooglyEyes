package com.github.crittscott.assets;

import java.util.ArrayList;
import java.util.List;

/** A node in the part-name hierarchy (skeleton mode). */
public class PartNode {
    public String name;
    public List<PartNode> children = new ArrayList<>();

    public PartNode(String name) {
        this.name = name;
    }
}
