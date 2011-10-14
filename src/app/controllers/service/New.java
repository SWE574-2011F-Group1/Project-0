package controllers.service;

import play.*;
import play.mvc.*;

import java.util.*;
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
        render(service);
    }
    
    public static void save(String title, ServiceType type, String desc) {
        //TODO: buralari yaz roy, buralari yaaaazzzz...
        renderText(title + "-" + type + "-" + desc);
    }
}