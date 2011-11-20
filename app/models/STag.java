package models;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

import play.data.validation.Required;
import play.db.jpa.Model;

@Entity
public class STag extends Model {

	@Required
	public String text;
	
	@ManyToOne
	public Service service;
	
	public STag(String text) {
        this.text = text;
    }
}
