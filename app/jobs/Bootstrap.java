package jobs;

import play.jobs.*;

import play.test.*;

import models.*;

@OnApplicationStart
public class Bootstrap extends Job {
    
    public void doJob() {
	//if (SUser.count() == 0) {
        //	Fixtures.loadModels("data.yml");
	//}
    }
    
}
