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
        Task t = new Task(name, point);
        t.status = TaskStatus.PENDING;
        t.suggestedBy = SUser.findByEmail(Auth.connected());
        t.save();
        thanks();
    }
    public static void proposals() {
        List<Task> tasks = Task.findProposed();
        render(tasks);
    }
    
    public static void thanks() {
        render();
    }
}

