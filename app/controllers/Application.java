package controllers;

import play.*;
import play.mvc.*;

import java.util.*;

import org.apache.commons.lang.StringUtils;

import models.*;

public class Application extends BaseController {

    public static void index() {
    	
    	//user logged in
//    	if( StringUtils.isNotBlank(Secure.Security.connected()))
//    	{
//    		List<Task> tasks = Task.findProposed();
//            render(tasks);
//    	}
        
        render();
    }
    
    public static void dashBoard()
    {
    	   
    	
    }

}