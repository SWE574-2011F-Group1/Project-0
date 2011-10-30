package controllers;

import java.util.*;
import models.*;
import play.*;

public class Auth extends Secure.Security {

    static boolean authenticate(String username, String password) {
        SUser u = SUser.connect(username, password);
        if (null != u) {
            return true;
        }
        return false;
    }
    
    /**
     * This one is for roles, like admin or user...
     */
    static boolean check(String profile) {
        if (isConnected()) {
            SUser u = SUser.findByEmail(connected());
            if (null == u) {
                return false;
            }
            if (profile.equals("user")) {
                return true;
            } else if (profile.equals("admin") && u.isAdmin) {
                return true;
            }
        }
        return false;
    }
    
    public static void fbLogin(String token, SUser user) {
        //He's either just registered or an already existing user...
        Logger.info("user (" + user.email + ") logged in with token: " + token);
        //Set the username session in order to comply with security module...
        session.put("username", user.email);
        session.put("fbToken", token);
    }
    
    static void onDisconnected() {
        //Delete the fbToken...
        session.remove("fbToken");
        redirect("/");
    }
}

