package models;

import play.*;
import play.db.jpa.*;

import javax.persistence.*;
import java.util.*;

@Entity
public class Activity extends Model {
    
    @ManyToOne
    public SUser affected;
    
    @ManyToOne
    public SUser performer;
    
    public ActivityType type;
    
    @Temporal(TemporalType.TIMESTAMP)
    public Date creationTime;
    
    public Activity() {
        creationTime = new Date();
    }
    
    public static List<Activity> findLatest() {
        return findLatest(100);
    }
    
    public static List<Activity> findLatest(int count) {
        return find("select a from Activity a order by creationTime desc").fetch(count);
    }
}

