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
    
    static void onDisconnected() {
        //Delete the fbToken...
        session.remove("fbToken");
        redirect("/");
    }
}

