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
        boolean inboxView = true;
        render(pms, inboxView);
    }
    
    public static void markAsRead() {
        long messageId = 0;
        try {
            messageId = Long.parseLong(params.get("messageId"));
        } catch (Exception e) {
            //Do nothing...
            return;
        }
        UserMessage m = UserMessage.findById(messageId);
        m.markRead();
        flash.put("message", "Marked As Read");
        inbox();
    }
    
    public static void outbox() {
        SUser r = SUser.findByEmail(Secure.Security.connected());
        List<UserMessage> pms = UserMessage.findBySender(r);
        boolean inboxView = false;
        renderTemplate("UserMessages/inbox.html", pms, inboxView);
    }
    
    public static void detail(long messageId) {
        
    }
    
    public static void compose(long serviceId) {
        Service service = Service.findById(serviceId);
        List<SUser> users = null;
        if (service == null) {
            users = SUser.findAll();
        }
        render(service, users);
    }
    
    public static void send(long serviceId, String message, String subject, long recipientId) {
        SUser sender = SUser.findByEmail(Auth.connected());
        UserMessage um = new UserMessage();
        um.message = message;
        um.subject = subject;
        um.sender = sender;
        um.sentTime = new Date();
        Service s = Service.findById(serviceId);
        if (s != null) {
            um.service = s;
            um.recipient = s.boss;
        } else {
            SUser recipient = SUser.findById(recipientId);
            if (recipient == null) {
                compose(0);
            }
            um.recipient = recipient;
        }
        um.save();
        if (s != null) {
           Services.detail(serviceId);
        } else {
            flash.put("message", "Message Sent!");
            inbox();
        }
    }
}

