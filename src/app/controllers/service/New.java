package controllers.service;

import play.*;
import play.mvc.*;

import java.util.*;

import com.sun.tools.corba.se.idl.ForwardEntry;

import controllers.BaseController;
import controllers.Secure;
import controllers.Check;
import models.*;

@With(Secure.class)
@Check("user")
public class New extends BaseController {

    public static void index() {
        Service service = new Service();
        //Set something to type to prevent null pointer exception...
        service.type = ServiceType.REQUESTS;
        Collection<Task> tasks = Task.findAll();
        render(service, tasks);
    }
    
    public static void save(String title, ServiceType type, String desc, long taskId) {
        Logger.info("task " + taskId);
        Service service = new Service();
        service.title = title;
        service.desc = desc;
        service.type = type;
        User u = User.findByEmail(session.get("user"));
        service.boss = u;
        Task task = Task.findById(taskId);
        Logger.info("task: " + task);
        service.task = task;
        service.save();
        list();
    }
    
	public static void list() {
		Collection<Service> services = Service.findAll();
		render(services);
	}
}