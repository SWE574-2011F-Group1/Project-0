package controllers;

import play.*;
import play.mvc.*;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import com.restfb.json.JsonObject;


import models.*;
import controllers.*;
import controllers.Secure.Security;

@With(Secure.class)
@Check("user")
public class Profile extends BaseController {

		
	//JsonObject photosConnection = facebookClient.fetchObject("me/photos", JsonObject.class);
	//String firstPhotoUrl = photosConnection.getJsonArray("data").getJsonObject(0).getString("source");
	
    public static void profile(long uid) {
    	SUser User;
    	List<Comment> comments = new ArrayList<Comment>();
    	List<Bookmark> bookmarks = new ArrayList<Bookmark>();
    	boolean loginUser;
    	boolean isBookmarked;
    	if (!params._contains("uid")) {
	    	User = SUser.findByEmail(Security.connected());
	    	comments = Comment.findByUserId(User.id);
	    	bookmarks = Bookmark.findByUserId(User.id);
	    	loginUser = true;
	        render(User, comments, loginUser, bookmarks);
    	} else {
        	User = SUser.findById(uid);
        	comments = Comment.findByUserId(uid);
        	bookmarks = Bookmark.findByUserId(SUser.findByEmail(Security.connected()).id);
        	if (uid == SUser.findByEmail(Security.connected()).id)
        		loginUser = true;
        	else
        		loginUser = false;
        	isBookmarked = Bookmark.isBookmarked(uid, SUser.findByEmail(Security.connected()).id);
        	render(User, comments, loginUser, isBookmarked, bookmarks);
    	}
    }
    
    public static void addBookmark(long userId) throws Exception {
    	Bookmark bookmark = new Bookmark();
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat  formatterDate = new SimpleDateFormat("dd.MM.yyyy");
        SimpleDateFormat  formatterTime = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
        try {
        	bookmark.bookmarkDate = formatterDate.parse(formatterDate.format(calendar.getTime()));
        } catch (ParseException e) {
            //FIXME: Find out what to do if this occurs...
        }    
        bookmark.bookmarkDateStr = formatterTime.format(calendar.getTime());
        bookmark.bookmarkedUsers = SUser.findById(userId);
    	bookmark.owner = SUser.findByEmail(Security.connected());
    	bookmark.save();
    	profile(bookmark.owner.id);
    }
    
    public static void removeBookmark(long userId) {
    	Bookmark.delete("bookmarkedUsers.id = ? and owner.id = ?", userId, SUser.findByEmail(Security.connected()).id);
    	profile(SUser.findByEmail(Security.connected()).id);
    }
}

