package controllers;

import play.*;
import play.mvc.*;

import java.util.*;

import models.*;

public class StaticPages extends BaseController {

    public static void about() {
        render();
    }
    
    public static void faq() {
        render();
    }
    
    public static void help() {
        render();
    }
}

