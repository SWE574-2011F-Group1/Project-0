package controllers;

import play.*;
import play.mvc.*;

import java.util.*;

import models.*;

public class Application extends BaseController {

    public static void index() {
        //List<User> xx = User.findAll();
        render();
    }

}