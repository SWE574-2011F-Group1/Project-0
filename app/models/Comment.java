package models;

import play.*;
import play.db.jpa.*;

import javax.persistence.*;
import java.util.*;

@Entity
public class Comment extends Model {
    
    @ManyToOne
    public SUser commenter;
    
    @Lob
    public String comment;
    
    public int helpfulCount;
    
    public int unhelpfulCount;
    
    
    public Comment(SUser u, String comment) {
        this.commenter = u;
        this.comment = comment;
    }
}

