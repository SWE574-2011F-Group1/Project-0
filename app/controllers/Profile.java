package controllers;

import play.*;
import play.mvc.*;

import java.util.*;


import models.*;
import controllers.*;
import controllers.Secure.Security;

public class Profile extends BaseController {

    public static void profile() {
    	SUser User = SUser.findByEmail(Security.connected());
        render(User);
    }
    
    public static void display(long uid)
    {
    	SUser User = SUser.findById(uid);
    	render(User);
    }
    
}

