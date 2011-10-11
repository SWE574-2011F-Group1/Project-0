package controllers;

import java.util.*;
import models.*;
import play.*;

public class Auth extends Secure.Security {

    static boolean authenticate(String username, String password) {
        //FIXME: Nice login auth right?
        return (username.equals("admin@roy.com") && password.equals("test"))
                || (username.equals("user@roy.com") && password.equals("test"));
    }
    
    /**
     * This one is for roles, like admin or user...
     */
    static boolean check(String profile) {
        if ("admin".equals(profile)) {
            return connected().equals("admin@roy.com");
        } else if ("user".equals(profile)) {
            return connected().equals("user@roy.com");
        }
        return false;
    }
}

