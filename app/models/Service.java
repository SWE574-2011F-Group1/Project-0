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
}

