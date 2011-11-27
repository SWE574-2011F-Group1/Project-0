package models;

import play.*;
import play.db.jpa.*;

import javax.persistence.*;
import java.util.*;

@Entity
public class SUserTaskVote extends Model {

	@ManyToOne
	public SUser votedBy;

	@ManyToOne
	public Task votedFor;
	
	public boolean isUpvote;
	
	public SUserTaskVote(SUser votedBy, Task votedFor, boolean isUpvote) {
		this.votedBy = votedBy;
		this.votedFor = votedFor;
		this.isUpvote = isUpvote;
	}
}
