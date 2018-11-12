package ru.dkadyrov.calculate.pi.distributed.common;

import java.util.ArrayList;
import java.util.List;

public class ChildrenCache {

    protected List<String> children;

    public ChildrenCache() {
        this.children = null;
    }

    public ChildrenCache(List<String> children) {
        this.children = children;
    }

    public List<String> getList() {
        return children;
    }

    public List<String> addedAndSet(List<String> newChildren) {
        ArrayList<String> diff = null;

        if (children == null) {
            diff = new ArrayList<>(newChildren);
        } else {
            for (String s : newChildren) {
                if (!children.contains(s)) {
                    if (diff == null) {
                        diff = new ArrayList<>();
                    }

                    diff.add(s);
                }
            }
        }
        this.children = newChildren;

        return diff;
    }

    public List<String> removedAndSet(List<String> newChildren) {
        List<String> diff = null;

        if (children != null) {
            for (String s : children) {
                if (!newChildren.contains(s)) {
                    if (diff == null) {
                        diff = new ArrayList<>();
                    }

                    diff.add(s);
                }
            }
        }
        this.children = newChildren;

        return diff;
    }
}
