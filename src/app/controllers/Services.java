package controllers;

import play.*;
import play.mvc.*;

import java.util.*;

import models.*;

@With(Secure.class)
@Check("user")
public class Services extends BaseController {

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
		//TODO: Pagination...
		Collection<Service> services = Service.findAll();
		Collection<Task> tasks = Task.findAll();
		render(services, tasks);
	}
	
	public static void detail(long serviceId) {
		Service service = Service.findById(serviceId);
		render(service);
	}
}