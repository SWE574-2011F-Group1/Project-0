package jobs;

import play.jobs.*;

import play.test.*;
 
@OnApplicationStart
public class Bootstrap extends Job {
    
    public void doJob() {
        //Fixtures.loadModels("data.yml");
    }
    
}