package controllers;

import play.*;
import play.mvc.*;

import java.util.*;

import org.apache.commons.lang.StringUtils;

import models.*;

public class Application extends BaseController {

    public static void index() {
    	
    	//user logged in
    	if( StringUtils.isNotBlank(Secure.Security.connected()))
    	{
    		//Proposed tasks-begin
    		SUser r = SUser.findByEmail(Secure.Security.connected());
    		List<Task> tasks = Task.findProposed();

    		//Proposed tasks-end
    		
    		//Service matches-begin
    		List<Service> services = new ArrayList<Service>();
    		
    		
    	
    		ServiceMatch sm=new ServiceMatch();
			List<ServiceMatch> sms = sm.findByUser(r);
    		
			for (ServiceMatch servicematch : sms) {
				 Logger.info("TaskName1:%s", servicematch.matchService.task.name);
				 services.add(servicematch.matchService);
			}
			Logger.info("TaskName1:%s", sms.size());
    		
    		//Service matches-end
		
    		//activities-begin
    		List<Activity> activities = Activity.findByAffected(r);
    		Logger.info("%s", r.email);
    		Logger.info("%s", activities);
    		
    		
    		
    		//activities-end
    		
    		
           render(tasks,services,activities);
    	}
    	else
    	{
        
        render();
    	}
    }
    
    public static void dashBoard()
    {
    	   
    	
    }

}