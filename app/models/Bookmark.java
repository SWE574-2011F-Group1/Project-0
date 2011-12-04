package models;

import play.*;
import play.db.jpa.*;

import javax.persistence.*;

import java.util.*;

@Entity
public class Bookmark extends Model {
	
    @ManyToOne
    public SUser bookmarkedUsers;
	
    @ManyToOne
	public SUser owner;
	
	public Date bookmarkDate;
	
	public String bookmarkDateStr;
	
    public static List<Bookmark> findByUserId(long userId){ 
    	return find("select b from Bookmark b where b.owner.id = ? ", userId).fetch();
    }
		
	public static boolean isBookmarked(long uid, long loginUserId) {
		List<Bookmark> bookmarks = new ArrayList<Bookmark>();
		boolean isBookmark = false;
		if (uid == loginUserId)
		{
			isBookmark = false;
		}
		else {
			bookmarks = find ("select b from Bookmark b where b.owner.id = ?", loginUserId).fetch();
			if (bookmarks.size() > 0)
			{
				for(int i = 0; i<bookmarks.size(); i++)
				{
					if (bookmarks.get(i).bookmarkedUsers.id == uid)
					{
						isBookmark = true;
					}
					else
					{
						isBookmark = false;
					}
				}
			}
			else
			{
				isBookmark = false;
			}
		}
		return isBookmark;
	}
	
}
