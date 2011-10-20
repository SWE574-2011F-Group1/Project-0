package models;

import play.*;
import play.db.jpa.*;

import javax.persistence.*;
import java.util.*;

@Entity
public class Task extends Model {
    
    public String name;
    
    public int point;
    
    public Task(String name, int point) {
        this.name = name;
        this.point = point;
    }
    
    public String toString() {
    	return this.name + " (" + this.point + ")";
    }
}

