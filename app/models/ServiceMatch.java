package models;

import java.util.List;

import javax.persistence.Entity;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;
import javax.persistence.ManyToOne;

import play.db.jpa.Model;

@Entity
public class ServiceMatch extends Model {
	
	@ManyToOne
	public Service serviceOfuser;
	
	@ManyToOne
	public Service matchService;
	
	@ManyToOne
	public SUser user;
	
	private int matchPoint;

	public Service getServiceOfuser() {
		return serviceOfuser;
	}

	public void setServiceOfuser(Service serviceOfuser) {
		this.serviceOfuser = serviceOfuser;
	}

	public Service getMatchService() {
		return matchService;
	}

	public void setMatchService(Service matchService) {
		this.matchService = matchService;
	}

	public SUser getUser() {
		return user;
	}

	public void setUser(SUser user) {
		this.user = user;
	}

	public int getMatchPoint() {
		return matchPoint;
	}

	public void setMatchPoint(int matchPoint) {
		this.matchPoint = matchPoint;
	}
	 public static List<ServiceMatch> findByServiceOfUser(Service service) {
	        return find("byServiceOfuser", service).fetch();
	 }
	 
	 public static List<ServiceMatch> findByUser(SUser user) {
	        return find("byUser", user).fetch();
	 }
	 
}
