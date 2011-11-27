package controllers;

import play.*;
import play.mvc.*;

import java.util.*;

import com.restfb.json.JsonObject;


import models.*;
import controllers.*;
import controllers.Secure.Security;

public class Profile extends BaseController {

		
	//JsonObject photosConnection = facebookClient.fetchObject("me/photos", JsonObject.class);
	//String firstPhotoUrl = photosConnection.getJsonArray("data").getJsonObject(0).getString("source");
	
    public static void profile(long uid) {
    	SUser User;
    	List<Comment> comments = new ArrayList<Comment>();
    	if (!params._contains("uid")) {
	    	User = SUser.findByEmail(Security.connected());
	    	comments = Comment.findByUserId(User.id);
	        render(User, comments);
    	} else {
        	User = SUser.findById(uid);
        	comments = Comment.findByUserId(uid);
        	render(User, comments);
    	}
    }
}

