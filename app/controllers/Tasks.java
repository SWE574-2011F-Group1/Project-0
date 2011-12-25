package controllers;

import play.*;
import play.mvc.*;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import models.*;

public class Tasks extends BaseController {

    public static void suggest() {
        render();
    }
    
    public static void sendModeration(String name, float point, String comment) {
        SUser u = SUser.findByEmail(Auth.connected());
        Task t = new Task(name, point);
        t.status = TaskStatus.PENDING;
        t.suggestedBy = u;
        t.save();
        
        //Task task = Task.findById(serviceId);
        if (comment != null && !"".equals(comment)) {
            Comment cmt = new Comment(u, u, comment);
            Calendar calendar = Calendar.getInstance();
            SimpleDateFormat  formatterTime = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
            SimpleDateFormat  formatterDate = new SimpleDateFormat("dd.MM.yyyy");
            Logger.info("Comment Date:" + formatterTime.format(calendar.getTime()));
          
            try {
            	cmt.commentDate = formatterDate.parse(formatterDate.format(calendar.getTime()));
            } catch (ParseException e) {
                //FIXME: Find out what to do if this occurs...
            }    
            cmt.commentDateWithTime = formatterTime.format(calendar.getTime());
            cmt.task = t;
            cmt.save();
            //t.comments = new ArrayList<Comment>();
            //t.comments.add(cmt);
          
            Logger.info("Comment :" + comment);
        }
       // t.save();
        
        Activity a = new Activity();
        a.performer = u;
        a.affectedTask = t;
        a.type = ActivityType.PROPOSED_TASK;
        a.save();
        thanks();
    }
    
    public static void addComment(long id, String comment) {
        SUser u = SUser.findByEmail(Auth.connected());
        Task t =  Task.findById(id);
      
        //Task task = Task.findById(serviceId);
        if (comment != null && !"".equals(comment)) {
            Comment cmt = new Comment(u, u, comment);
            Calendar calendar = Calendar.getInstance();
            SimpleDateFormat  formatterTime = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
            SimpleDateFormat  formatterDate = new SimpleDateFormat("dd.MM.yyyy");
            Logger.info("Comment Date:" + formatterTime.format(calendar.getTime()));
          
            try {
            	cmt.commentDate = formatterDate.parse(formatterDate.format(calendar.getTime()));
            } catch (ParseException e) {
                //FIXME: Find out what to do if this occurs...
            }    
            cmt.commentDateWithTime = formatterTime.format(calendar.getTime());
            cmt.task = t;
            cmt.save();
            Logger.info("Comment :" + comment);
        }

        taskDetail(id);
    }
    
    
    public static void proposals(String message) {
        List<Task> tasks = Task.findProposed();
        Task task; 
        for(int i=0;i<tasks.size();i++)
        {
        	task=tasks.get(i);
        	task.comments =Comment.findByTask(task.id);
        	Logger.info("task :" + task);
        	Logger.info("Comment size:" + task.comments.size());	
        }
        
        render(tasks,message);
    }
    public static void allTasks() {
        List<Task> tasks = Task.findAllTasks();
        render(tasks);
    }
    public static void taskDetail(long id) {
        Task task  = Task.findById(id);
        task.comments = Comment.findByTask(task.id);
    	Logger.info("task :" + task.name);
        render(task);
    }
    
    
    public static void thanks() {
        render();
    }
    public static void taskVotedMessage() {
    	thanks();
    }
    
    public static void upvote(long id) {
        Task task = Task.findById(id);
        SUser user = SUser.findByEmail(Auth.connected()); 
        boolean isVoted = user.upvoteForTask(task);
        user.save();
        
        if(isVoted)
        {
        Activity a = new Activity();
        a.performer = user;
        a.affectedTask = task;
        a.type = ActivityType.VOTED_ON_TASK;
        a.save();
        }
        
        Settings settings = Settings.findById(1L);
        
        if(isVoted)
        {
        if (task.voteDiff >= settings.voteCountThreshold) {
        	task.status = TaskStatus.ACTIVE;
        	task.save();
        	Activity a = new Activity();
            a.performer = user;
            a.affectedTask = task;
            a.type = ActivityType.TASK_APPROVED;
            a.save();
        }
        }
        String messeage="";
        if(isVoted)
        	messeage="Thanks for voting for the task";
        else
        	messeage="You have already voted for the task";

        proposals(messeage);
    }
    
    public static void downvote(long id) {
        Task task = Task.findById(id);
        SUser user = SUser.findByEmail(Auth.connected()); 
        boolean isVoted = user.downvoteForTask(task);
        user.save();
        
        if(isVoted)
        {
        Activity a = new Activity();
        a.performer = user;
        a.affectedTask = task;
        a.type = ActivityType.VOTED_ON_TASK;
        a.save();
        }
        String messeage="";
        if(isVoted)
        	messeage="Thanks for voting for the task";
        else
        	messeage="You have already voted for the task";

        proposals(messeage);
    }
    
    
}

