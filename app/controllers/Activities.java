package controllers;

import play.*;
import play.mvc.*;

import java.util.*;

import models.*;

public class Activities extends BaseController {

    public static void index() {
        List<Activity> list = Activity.findLatest(15);
        render(list);
    }    
}

