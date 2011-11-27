package models;

import play.*;
import play.db.jpa.*;

import javax.persistence.*;
import java.util.*;

@Entity
public class Settings extends Model {
	public int voteCountThreshold;
}
