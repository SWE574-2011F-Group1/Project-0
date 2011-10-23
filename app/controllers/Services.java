package controllers;

import play.*;
import play.mvc.*;

import java.text.ParseException;
import java.text.SimpleDateFormat;
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
    
    public static void save(String title, ServiceType type, String description, long taskId, String location, String startDate, String endDate) {
        Logger.info("task " + taskId);
        Service service = new Service();
        service.title = title;
        service.description = description;
        service.type = type;
        service.location = location;
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy");
        try {
        	service.startDate = sdf.parse(startDate);
            service.endDate = sdf.parse(endDate);
        } catch (ParseException e) {
        	//FIXME: Find out what to do if this occurs...
        }
        SUser u = SUser.findByEmail(Secure.Security.connected());
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