package jobs;

import models.*;
import play.Logger;
import play.jobs.*;
import java.util.*;

import play.test.*;

import models.*;

@OnApplicationStart
public class Bootstrap extends Job {
    public void doJob() {
	    if (SUser.count() == 0) {	    	
            Fixtures.loadModels("data.yml");
            List<Service> list = Service.findAll();
            for (Service s : list) {
            	Service.findMatchServices(s, true);
            }
	    }
    }
}