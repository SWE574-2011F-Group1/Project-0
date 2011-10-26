package controllers.user;

import play.*;
import play.mvc.*;

import java.util.*;

import models.*;
import controllers.*;

public class Register extends BaseController {

	public static void register(String email, String name, String password) {
		if (null != SUser.findByEmail(email)) {
			throw new RuntimeException("Email is not unique...");
		}
		SUser u = new SUser(name, email);
		//FIXME: Hash the password...
		u.password = password;
		u.save();
		redirect("/");
	}
	
	public static void emailCheck(String email) {
		if (null == SUser.findByEmail(email)) {
			renderJSON(true);
		} else {
			renderJSON(false);
		}
	}
}
