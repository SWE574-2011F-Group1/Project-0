package controllers;

import play.*;
import play.mvc.*;

import java.util.*;

import com.restfb.DefaultFacebookClient;
import com.restfb.FacebookClient;
import com.restfb.types.User;

import controllers.Secure.Security;
import models.*;

/**
 * Life Cycle of an HTTP request
 *
 * @Before
 * action()
 * @After
 * templaterender()
 * @Finally
 */
public class BaseController extends Controller {

    @Before
    public static void initLogin() {
        if (Security.isConnected()) {
            SUser u = SUser.findByEmail(Security.connected());
            if (null != u) {
                renderArgs.put("user", u.email);
                renderArgs.put("user_name", u.name);
                renderArgs.put("userObj", u);
                renderArgs.put("unreadMsgCount", UserMessage.findUnreadCountByUser(u));
                renderArgs.put("loggedIn", true);
                renderArgs.put("menuItems", new String[] {"Services", "Messages", "Service Types", "Activities"});
                renderArgs.put("message", flash.get("message"));
                Random r = new Random(new Date().getTime());
                String[] greetings = new String[] { "Hello", "Merhaba", "Olà", "Բարեւ", "Bonjour", "Pronto" };
                renderArgs.put("greetings", greetings[r.nextInt(greetings.length)]);
            }
        }
    }

    public static void fbLogin() {
        String token = params.get("token");
        if (null != token && !token.isEmpty()) {
            FacebookClient fb = new DefaultFacebookClient(token);
            User fbUser = fb.fetchObject("me", User.class);
            
            Logger.info("Facebook User:" + fbUser);
            SUser sesUser = SUser.findByFbId(fbUser.getId());
            Logger.info("returned user %s", sesUser);
            if (sesUser == null) {
                Logger.info("Such a user does not exists. Create/Register one...");
                //Register a new...
                //Email uniqueness is controlled by Facebook I suppose, so no need to check on our side...
                sesUser = new SUser(fbUser.getName(), fbUser.getEmail());
                sesUser.fbId = fbUser.getId();
                sesUser.registrationTime=new Date(System.currentTimeMillis());
                sesUser.save();
            }
            session.put("username", sesUser.email);
            session.put("fbToken", token);
        } else {
            redirect("/");
        }
    }
}