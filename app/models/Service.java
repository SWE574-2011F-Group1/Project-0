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
    
    @OneToMany(cascade=CascadeType.ALL,fetch=FetchType.EAGER)
    public Set<STag> stags;
    
    public Date startDate;
    
    public Date endDate;
    
    public Date actualDate;
    
    @Required
    @ManyToOne
    public Task task;
    
    @ManyToOne(cascade=CascadeType.ALL)
    public SUser boss;
    
    @ManyToOne(cascade=CascadeType.ALL)
    public SUser employee;
    
    @ManyToMany(cascade=CascadeType.ALL)
    public List<SUser> applicants;
    
    @OneToMany(cascade=CascadeType.ALL,mappedBy="serviceOfuser")
    public List<ServiceMatch> matchesAsBoss;
    
    @OneToMany(cascade=CascadeType.ALL,mappedBy="matchService")
    public List<ServiceMatch> matchesAsEmployee;

    /*@ManyToMany(cascade=CascadeType.ALL)
    public List<SUser> employees;*/
        
    
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
    
    public String getFormattedActualDate() {
    	return formatDate(this.actualDate);
    }
    
    private String formatDate(Date d) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy");
        return null != d ? sdf.format(d) : "";
    }
    
    public static List<Service> findByTask(long taskId) {
        return find("byTask.id", taskId).fetch();
    }
    
    public static List<Service> findByUserAndStatus(long userId, int type) {
    	List<Service> services = new ArrayList<Service>();
    	Logger.info("type: " + type);
    	Logger.info("userId: " + userId);
    	StringBuffer sql = new StringBuffer("select s from Service s where s.boss.id = " + userId + " and ");
    	if (type == 1) { //active sr
    		sql.append("s.status not in (?, ?) and s.type = ?");
    		services = find(sql.toString(), ServiceStatus.DRAFT, ServiceStatus.FINISHED, ServiceType.REQUESTS).fetch();
    	} else if (type == 2) { //active so
    		Logger.info("Type2");
    		sql.append("s.status not in (?, ?) and s.type = ?");
    		services = find(sql.toString(), ServiceStatus.DRAFT, ServiceStatus.FINISHED, ServiceType.PROVIDES).fetch();
    	} else if (type == 3) { //planned sr/so
    		sql.append("s.status = (?)");
    		services = find (sql.toString(), ServiceStatus.IN_PROGRESS).fetch();
    	} else if (type == 4) { //done sr/so
    		sql.append("s.status = (?)");
    		services = find (sql.toString(), ServiceStatus.FINISHED).fetch();
    	}
    		
    	Logger.info("type: " + type);
    	Logger.info("userId: " + userId);
        return services;
    	
    	/*return find("select s from Service s where s.boss.id = " + userId + " and " +
        		"s.status not in (?, ?) and " + 
        		"s.type = ?", ServiceStatus.DRAFT, ServiceStatus.FINISHED, ServiceType.REQUESTS).fetch();*/    
    }
}

