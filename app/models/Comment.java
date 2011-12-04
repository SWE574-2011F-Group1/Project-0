package models;

import play.*;
import play.data.binding.types.DateTimeBinder;
import play.db.jpa.*;

import javax.persistence.*;

import org.joda.time.DateTime;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

@Entity
public class Comment extends Model {
    
    @ManyToOne
    public SUser commenter;
    
    @ManyToOne
    public SUser owner;
    
    @ManyToOne
    public Service service;
    
    @Lob
    public String comment;
    
    public int helpfulCount;
    
    public int unhelpfulCount;
    
    public String commentDateWithTime;
    
    public Date commentDate;
    
    public Comment(SUser u, SUser owner, String comment) {
        this.commenter = u;
        this.owner = owner;
        this.comment = comment;
    }
    
    public static List<Comment> findByUserId(long userId){
    	//List<Comment> services = new ArrayList<Comment>();
    	return find("select c from Comment c where c.owner.id = ?", userId).fetch();
    }
    
    public static List<Comment> findByService(long serviceId) {
    	return find ("select c from Comment c where c.service.id = ?", serviceId).fetch();
    }
}

