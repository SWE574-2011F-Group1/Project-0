package jobs;

import models.*;
import play.jobs.*;

import play.test.*;
 
@OnApplicationStart
public class Bootstrap extends Job {
    
    public void doJob() {
    	if (SUser.count() < 0) {
    		Fixtures.loadModels("data.yml");
    	}
    }
    
}