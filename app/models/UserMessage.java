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
    
    @Lob
    @Required
    public String message;
    
    public Date sentTime;
    
    public Date readTime;
    
    public static List<UserMessage> findByRecipient(SUser r) {
        return find("byRecipient", r).fetch();
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
        return "{Message: {sender: '" + sender + "', recipient: '" + recipient + "', message: '" + message +"'}}";
    }
}

