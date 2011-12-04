package controllers;

import play.*;
import play.mvc.*;

import java.util.*;

import models.*;

public class Tasks extends BaseController {

    public static void suggest() {
        render();
    }
    
    public static void sendModeration(String name, float point) {
        SUser u = SUser.findByEmail(Auth.connected());
        Task t = new Task(name, point);
        t.status = TaskStatus.PENDING;
        t.suggestedBy = u;
        t.save();
        Activity a = new Activity();
        a.performer = u;
        a.affectedTask = t;
        a.type = ActivityType.PROPOSED_TASK;
        a.save();
        thanks();
    }
    public static void proposals() {
        List<Task> tasks = Task.findProposed();
        render(tasks);
    }
    
    public static void thanks() {
        render();
    }
    public static void upvote(long id) {
        Task task = Task.findById(id);
        SUser user = SUser.findByEmail(Auth.connected()); 
        user.upvoteForTask(task);
        user.save();
        
        Activity a = new Activity();
        a.performer = user;
        a.affectedTask = task;
        a.type = ActivityType.VOTED_ON_TASK;
        a.save();
        
        Settings settings = Settings.findById(1L);
        
        if (task.voteDiff >= settings.voteCountThreshold) {
        	task.status = TaskStatus.ACTIVE;
        	task.save();
        	
        	a = new Activity();
            a.performer = user;
            a.affectedTask = task;
            a.type = ActivityType.TASK_APPROVED;
            a.save();
        }
        
        proposals();
    }
    
    public static void downvote(long id) {
        Task task = Task.findById(id);
        SUser user = SUser.findByEmail(Auth.connected()); 
        user.downvoteForTask(task);
        user.save();
        
        Activity a = new Activity();
        a.performer = user;
        a.affectedTask = task;
        a.type = ActivityType.VOTED_ON_TASK;
        a.save();
        
        proposals();
    }
    
    
}

