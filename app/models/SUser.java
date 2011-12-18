package models;

import play.*;
import play.db.jpa.*;

import javax.persistence.*;

import org.hibernate.annotations.CascadeType;

import java.util.*;

import play.data.validation.*;

@Entity
public class SUser extends CommentableModel {
    
    @Required
    public String name;
    
    @Required
    public String email;
    
    public long providerPoint;
    
    public long requesterPoint;
    
    public String password;
    
    public String fbId;
    
    public boolean isAdmin;
    public boolean getIsAdmin()
    {
    	return this.isAdmin;
    }
    
    @ManyToMany(mappedBy="applicants")
    public List<Service> appliedServices;
    
    @OneToMany
    public List<Service> servicesAsEmployee;
    
    @OneToMany
    public List<Service> servicesAsBoss;
    
    @OneToMany(cascade=javax.persistence.CascadeType.ALL,mappedBy="user")
    public List<ServiceMatch> matchServices;

    @OneToMany(cascade=javax.persistence.CascadeType.ALL)
    public List<SUserTaskVote> taskVotes;
    
    public long getAbsoluteSocialPoint() {
        return providerPoint + requesterPoint;
    }
    
    public boolean upvoteForTask(Task task) {
    	for(SUserTaskVote taskVote : this.taskVotes)
    		if (taskVote.votedFor == task) return false;
    	
    	SUserTaskVote vote = new SUserTaskVote(this, task, true);
    	//vote.save();
    	this.taskVotes.add(vote);
    	task.upvote();
    	task.save();
    	return true;
    }
    
    public boolean downvoteForTask(Task task) {
    	for(SUserTaskVote taskVote : this.taskVotes)
    		if (taskVote.votedFor == task) return false;

    	SUserTaskVote vote = new SUserTaskVote(this, task, false);
    	//vote.save();

    	this.taskVotes.add(vote);
    	task.downvote();
    	task.save();
    	return false;
    }

    public SUser(String name, String email) {
        this.email = email;
        this.name = name;
        this.taskVotes = new ArrayList<SUserTaskVote>();
    }

    public static SUser connect(String email, String password) {
        return find("byEmailAndPassword", email, password).first();
    }

    public static SUser findByEmail(String email) {
        return find("byEmail", email).first();
    }

    public static SUser findByFbId(String fbId) {
        return find("byFbId", fbId).first();
    }
    
    public static List<SUser> findByName(String q) {
        return find("select u from SUser u where name like ?", "%" + q + "%").fetch();
    } 

    public String toString() {
    	return this.name;
    }
}

