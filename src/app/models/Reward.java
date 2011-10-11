package models;

import play.*;
import play.db.jpa.*;

import javax.persistence.*;
import java.util.*;

@Entity
public class Reward extends Model {
    public String name;
    
    public Reward(String name) {
        this.name = name;
    }
}

