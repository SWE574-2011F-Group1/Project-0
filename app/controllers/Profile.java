package controllers;

import play.*;
import play.mvc.*;

import java.util.*;


import models.*;
import controllers.*;
import controllers.Secure.Security;

public class Profile extends BaseController {

    public static void profile(long uid) {
    	SUser User;
    	if (!params._contains("uid")) {
	    	User = SUser.findByEmail(Security.connected());
	        render(User);
    	} else {
        	User = SUser.findById(uid);
        	render(User);
    	}
    }
}

