package models;

import play.*;
import play.db.jpa.*;

import javax.persistence.*;
import java.util.*;

@Entity
public class Task extends Model {
    
    public String name;
    
    public float point;
    
    @Transient
    public long weight;
    
    public Task(String name, int point) {
        this.name = name;
        this.point = point;
    }
    
    public String toString() {
    	return this.name + " (" + this.point + ")";
    }
    
    public static List<Task> findWithWeights() {
        Iterator i = find("select t, (select count(s) from Service s where s.task = t) from Task t").fetch().iterator();
        List<Task> tasks = new ArrayList<Task>();
        while (i.hasNext()) {
            Object[] o = (Object[]) i.next();
            Task t = (Task) o[0];
            t.weight = Long.valueOf((Long) o[1]);
            tasks.add(t);
        }
        Logger.info("%s", tasks);
        return tasks;
    }
}