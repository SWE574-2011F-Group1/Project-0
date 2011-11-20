package controllers;

import play.*;
import play.mvc.*;

import java.util.*;

import models.*;

@With(Secure.class)
@Check("user")
public class UserMessages extends BaseController {

    public static void index() {
        inbox();
    }
    
    public static void inbox() {
        SUser r = SUser.findByEmail(Secure.Security.connected());
        List<UserMessage> pms = UserMessage.findByRecipient(r);
        render(pms);
    }
    
    public static void outbox() {
        
    }
    
    public static void detail(long messageId) {
        
    }
    
    public static void compose(long taskId) {
        
    }
    
    public static void send(long taskId, String message, long userId) {
        
    }
}

