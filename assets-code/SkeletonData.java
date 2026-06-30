package com.github.crittscott.assets;

import java.util.ArrayList;
import java.util.List;

/** Skeleton-mode output: the part-name hierarchy only, no geometry. */
public class SkeletonData {
    public String entityType;
    public String modelClass;
    public List<PartNode> roots = new ArrayList<>();
}
