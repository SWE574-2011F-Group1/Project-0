package models;

import play.*;
import play.db.jpa.*;

import javax.persistence.*;
import java.util.*;

public class CommentableModel extends Model {

    @OneToMany
    public Set<Comment> comments;
}