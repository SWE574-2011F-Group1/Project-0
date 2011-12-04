package models;

import play.*;
import play.db.jpa.*;

import javax.persistence.*;
import java.util.*;
import java.text.SimpleDateFormat;

@Entity
public class Activity extends Model {
    
    @ManyToMany(cascade=CascadeType.ALL)
    public List<SUser> affectedUsers;
    
    @ManyToOne
    public SUser performer;
    
    public ActivityType type;
    
    @Temporal(TemporalType.TIMESTAMP)
    public Date creationTime;
    
    @ManyToOne
    public Service affectedService;
    
    @ManyToOne
    public Task affectedTask;
    
    public Activity() {
        creationTime = new Date();
        affectedUsers = new ArrayList<SUser>();
    }
    
    public static List<Activity> findLatest() {
        return findLatest(100);
    }
    
    public static List<Activity> findLatest(int count) {
        return find("select a from Activity a order by creationTime desc").fetch(count);
    }
    
    public String getFormattedCreationDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm");
        return sdf.format(this.creationTime);
    }
}

