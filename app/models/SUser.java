package models;

import play.*;
import play.db.jpa.*;

import javax.persistence.*;

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
    
    @ManyToMany(mappedBy="applicants")
    public List<Service> appliedServices;
    
    public long getAbsoluteSocialPoint() {
        return providerPoint + requesterPoint;
    }

    public SUser(String name, String email) {
        this.email = email;
        this.name = name;
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

    public String toString() {
    	return this.name;
    }
}

