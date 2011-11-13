package models;

import play.*;
import play.db.jpa.*;

import javax.persistence.*;

import net.sf.oval.constraint.NotEmpty;

import java.text.SimpleDateFormat;
import java.util.*;

import play.data.validation.*;

@Entity
public class Service extends CommentableModel {
    
	@Required
	@NotEmpty
    public String title;
    
    @Lob
    @Required
    @NotEmpty
    public String description;
    
    @Required
    @NotEmpty
    public String location;
    
    public Date startDate;
    
    public Date endDate;
    
    @Required
    @ManyToOne
    public Task task;
    
    @ManyToOne
    public SUser boss;
    
    @ManyToOne
    public SUser employee;
    
    @Required
    public ServiceStatus status;
    
    @Required
    public ServiceType type;
    
    @OneToMany
    public Set<Reward> rewards;
    
    public String getFormattedStartDate() {
        return formatDate(this.startDate);
    }
    
    public String getFormattedEndDate() {
    	return formatDate(this.endDate);
    }
    
    private String formatDate(Date d) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy");
        return null != d ? sdf.format(d) : "";
    }
    
    public static List<Service> findByTask(long taskId) {
        return find("byTask.id", taskId).fetch();
    }
    
    public static List<Service> findByUserAndStatus(long userId, int type) {
    	ArrayList l = new ArrayList();
    	l.add(ServiceStatus.DRAFT);
    	l.add(ServiceStatus.FINISHED);
    	Logger.info("St: " + ServiceType.REQUESTS);
        return find("select s from Service s where s.id = " + userId + " and " +
        		"s.status not in (?, ?) and " +
        		"s.type = ? ", ServiceStatus.DRAFT, ServiceStatus.FINISHED, ServiceType.REQUESTS).fetch(); 
        
    }
}

