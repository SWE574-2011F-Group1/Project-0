package controllers;

import play.*;
import play.mvc.*;

import java.util.*;

import controllers.Secure.Security;
import models.*;

public class BaseController extends Controller {

    @Before
    public static void initLogin() {
        if (Security.isConnected()) {
        	//Email
        	renderArgs.put("user", Security.connected());
        	renderArgs.put("loggedIn", true);
        }
    }
    /*
    @Before
    action()
    @After
    templaterender()
    @Finally
    */
}

