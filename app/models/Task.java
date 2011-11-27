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
    
    public TaskStatus status;
    
    public int upvoteCount;
    
    public int downvoteCount;
    
    public int voteDiff;
    
    @ManyToOne
    public SUser suggestedBy;
    
    public Task(String name, float point) {
        this.name = name;
        this.point = point;
    }
    
    public String toString() {
    	return this.name + " (" + this.point + ")";
    }
    
    public void upvote() {
    	this.upvoteCount++;
    	this.voteDiff++;
    }
    
    public void downvote() {
    	this.downvoteCount++;
    	this.voteDiff--;
    }
    
    public static List<Task> findWithWeights() {
        Iterator i = find("select t, (select count(s) from Service s where s.task = t) from Task t WHERE t.status = ?", TaskStatus.ACTIVE).fetch().iterator();
        List<Task> tasks = new ArrayList<Task>();
        while (i.hasNext()) {
            Object[] o = (Object[]) i.next();
            Task t = (Task) o[0];
            t.weight = Long.valueOf((Long) o[1]);
            tasks.add(t);
        }
        return tasks;
    }
    
    public static List<Task> findProposed() {
        return findByStatus(TaskStatus.PENDING);
    }
    
    public static List<Task> findAllActive() {
        return findByStatus(TaskStatus.ACTIVE);
    }
    
    private static List<Task> findByStatus(TaskStatus st) {
        return find("byStatus", st).fetch();
    }
}