package models;

import play.*;
import play.db.jpa.*;

import java.text.SimpleDateFormat;
import javax.persistence.*;
import java.util.*;

import play.data.validation.*;

@Entity
public class UserMessage extends Model {
    
    @ManyToOne
    @Required
    public SUser recipient;
    
    @ManyToOne
    @Required
    public SUser sender;
    
    @Required
    public String subject;
    
    @ManyToOne
    public Service service;
    
    @Lob
    @Required
    public String message;
    
    @Temporal(TemporalType.TIMESTAMP)
    public Date sentTime;
    
    @Temporal(TemporalType.TIMESTAMP)
    public Date readTime;
    
    public static List<UserMessage> findByRecipient(SUser r) {
        return findByXOrdered(r, "recipient");
    }
    
    public static List<UserMessage> findBySender(SUser r) {
        return findByXOrdered(r, "sender");
    }
    
    private static List<UserMessage> findByXOrdered(SUser u, String findType) {
        return find("select m from UserMessage m where " + findType + " = ? order by readTime asc, sentTime desc", u).fetch();
    }
    
    public static int findUnreadCountByUser(SUser u) {
        return find("select m from UserMessage m where recipient = ? and readTime is null", u).fetch().size();
    }
    
    public String getFormattedSentTime() {
        return formatDate(this.sentTime);
    }
    
    public String getFormattedReadTime() {
    	return formatDate(this.readTime);
    }
    
    private String formatDate(Date d) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy");
        return null != d ? sdf.format(d) : "";
    }
    
    public String toString() {
        return "{Message: {sender: '" + sender + "', recipient: '" + recipient + "', subject:'" + subject + "', message: '" + message +"'}}";
    }
    
    public void markRead() {
        this.readTime = new Date();
        this.save();
    }
}

