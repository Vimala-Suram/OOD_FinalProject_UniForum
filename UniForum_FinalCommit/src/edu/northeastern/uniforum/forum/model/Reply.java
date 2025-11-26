package edu.northeastern.uniforum.forum.model;

import java.util.ArrayList;
import java.util.List;

public class Reply {

    private String text;
    private List<Reply> children = new ArrayList<>();

    public Reply(String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }

    public List<Reply> getChildren() {
        return children;
    }

    public void addChild(Reply reply) {
        children.add(reply);
    }
}
