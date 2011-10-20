package models;

import play.*;
import play.db.jpa.*;

import javax.persistence.*;
import java.util.*;

import play.data.validation.*;

@Entity
public class Service extends CommentableModel {
    
    public String title;
    
    @Lob
    public String desc;
    
    @Required
    @ManyToOne
    public Task task;
    
    @ManyToOne
    public User boss;
    
    @ManyToOne
    public User employee;
    
    @Required
    public ServiceStatus status;
    
    @Required
    public ServiceType type;
    
    @OneToMany
    public Set<Reward> rewards;
    
}

