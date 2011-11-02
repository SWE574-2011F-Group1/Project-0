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

    public static void edit(long serviceId) throws Exception {
        Service service = Service.findById(serviceId);
        if (!Auth.connected().equals(service.boss.email)) {
            //Redirect unauthorized ones... Cakaaaaaallllll...
            detail(serviceId);
        }
        Collection<Task> tasks = Task.findAll();
        renderTemplate("Services/index.html", service, tasks, serviceId);
    }

    public static void save(String title, ServiceType type, String description, long taskId, String location, String startDate, String endDate) {
        Service service;
        if (params._contains("serviceId")) {
            service = Service.findById(Long.parseLong(params.get("serviceId")));
        } else {
            service = new Service();
        }
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
        service.task = task;
        service.save();
        detail(service.id);
    }

    public static void list() {
        //TODO: Pagination...
        Collection<Service> services = null;
        Logger.info("fooo: %s", params.get("task"));
        if (params._contains("task") && null != params.get("task") && !params.get("task").equals("")) {
            Logger.info("hede ho o");
            services = Service.findByTask(Long.valueOf(params.get("task")));
        } else {
            services = Service.findAll();
        }
        Collection<Task> tasks = Task.findWithWeights();
        render(services, tasks);
    }

    public static void detail(long serviceId) {
        Service service = Service.findById(serviceId);
        boolean showEditBtn = Auth.connected().equals(service.boss.email);
        render(service, showEditBtn);
    }
}