package controllers;

import play.*;
import play.cache.Cache;
import play.db.jpa.JPA;
import play.mvc.*;

import java.io.IOException;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import javax.persistence.Query;

import org.apache.commons.lang.SerializationUtils;
import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;
import org.json.JSONArray;
import org.json.JSONObject;

import models.*;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.*;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;
import org.bouncycastle.crypto.engines.ISAACEngine;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.sun.jndi.toolkit.url.UrlUtil;


@With(Secure.class)
@Check("user")
public class Services extends BaseController {
	
	public static int serviceNumberPerPage = 6;


    public static void index() {
        Service service = new Service();
        service.type = ServiceType.REQUESTS;
        service.locationType=LocationType.NORMAL;
        if (params.get("type") != null && params.get("type").equals("0")) {
        	service.type = ServiceType.PROVIDES;
        }
        Collection<Task> tasks = Task.findAllActive();
        render(service, tasks);
    }

    public static void edit(long serviceId) throws Exception {
        Service service = Service.findById(serviceId);
        if (!Auth.connected().equals(service.boss.email)) {
            //Redirect unauthorized ones... Cakaaaaaallllll...
            detail(serviceId);
        }
        Collection<Task> tasks = Task.findAllActive();
        renderTemplate("Services/index.html", service, tasks, serviceId);
    }

    public static void save(String title, ServiceType type, String description, long taskId, 
    		String location, String startDate, String endDate,String tags, List<String> slots,
    		double locationLat,double locationLng,LocationType locationType) {
        Service service;

        Set<STag> deletedTags=null;
        Activity a = new Activity();
        boolean isUpdate=false;
        if (params._contains("serviceId")) {
            service = Service.findById(Long.parseLong(params.get("serviceId")));
            deletedTags = service.stags;
            isUpdate=true;
            a.type = ActivityType.UPDATED_SERVICE;
        } else {
            a.type = ActivityType.ADDED_SERVICE;
            service = new Service();
        }

        service.slots = new ArrayList<ServiceAvailabilitySlot>();

        if (slots != null) {
            for (String slot: slots) {
        	String[] parts = slot.split(",");
        	
        	DayOfWeek day = DayOfWeek.values()[Integer.valueOf(parts[0])];
        	int hourStart = Integer.valueOf(parts[1]);
        	int minuteStart = Integer.valueOf(parts[2]);
        	int hourEnd = Integer.valueOf(parts[3]);
        	int minuteEnd = Integer.valueOf(parts[4]);

        	service.addSlot(day, hourStart, minuteStart, hourEnd, minuteEnd);
            }
        }

        service.title = title;
        service.description = description;
        service.type = type;
        service.locationType=locationType;
        
        if(locationType!=LocationType.NORMAL){
        	service.location = "";
	        service.locationLat=0;
	        service.locationLng=0;
        }
        else{
	        service.location = location;
	        service.locationLat=locationLat;
	        service.locationLng=locationLng;
        }
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy");
        try {
            service.startDate = sdf.parse(startDate);
            service.endDate = sdf.parse(endDate);
        } catch (ParseException e) {
            //FIXME: Find out what to do if this occurs...
        }
        
        SUser serviceOwner = SUser.findByEmail(Secure.Security.connected());
        service.boss = serviceOwner;
        service.stags = new HashSet<STag>();
        
        if (!tags.trim().equals("")) {
        	StringTokenizer st = new StringTokenizer(tags,",");
        	Set<STag> sTags = new HashSet<STag>();
        	while (st.hasMoreTokens()){
        		STag t = new STag(st.nextToken().trim());
        		sTags.add(t);
        		t.service = service;
        	}
        	service.stags = sTags;
        }
        
        Task task = Task.findById(taskId);
        service.task = task;

        service.status = ServiceStatus.PUBLISHED;
        service.save();
        
        if (deletedTags!=null) {
        	for (STag tag: deletedTags) {
				tag.delete();
			}
        }
        
        a.performer = serviceOwner;
        a.affectedService = service;
        a.save();
        
        Service.findMatchServices(service,isUpdate);
        
        detail(service.id);
    }

    public static void myServices() {
        SUser u = SUser.findByEmail(Auth.connected());
        list(u.id, -1);
    }

    public static void list(long uid, int st) {    	
        List<Service> services = null;
        if (params._contains("task") && null != params.get("task") && !params.get("task").equals("")) {
            services = Service.findByTask(Long.valueOf(params.get("task")));
        }
        else if (params._contains("tag") && null != params.get("tag") && !params.get("tag").equals("")) {
           String tag=params.get("tag");
        	Logger.info("service list wit tag %s",tag);
            services = Service.findByTag(tag);
        }
        else if (params._contains("uid") && params._contains("st")) {

        	services = Service.findByUserAndStatus(uid, st);
        } else if (params._contains("uid") && st == -1) {
            services = Service.findByUserAndStatus(uid, -1);
        } else {
        	services = Service.findAll();
        }
        Collection<Task> tasks = Task.findWithWeights();
     
        int maxPageNumber = 0;
        if (services != null) {
        	maxPageNumber = services.size() / serviceNumberPerPage;
        	if (services.size() % serviceNumberPerPage > 0)
        		maxPageNumber++;
        }
        
        List<Service> serializedServices = new ArrayList<Service>();
        for (Service service : services) {
        	serializedServices.add(service);
		}
        Cache.set("filteredServices", serializedServices, "30min");
        
        /*if (maxPageNumber != 0) {
        	services = getServicesForPage(services, 1);
        }*/
       
        List<STagCloud> tagClouds=getTagCloudData();
        
        
        Cache.set("rankingNone", new Boolean(true), "30min");
		Cache.set("rankingNoneWithDistance", new Boolean(false), "30min");
		
		Logger.info("Getting services ranked by registration date...");
		List<Service> regisDateRankedServices=rankServicesWithAscendingRegistationDate(services);
		
		Logger.info("Ranking services with ascending  social point...");
		List<Service> socialPointRankedServices=rankServicesWithAscendingSocalPoint(services);
		
		
		Cache.set("regisDateRankedServices", regisDateRankedServices, "30min");
		Cache.set("socialPointRankedServices", socialPointRankedServices, "30min");
		
		Map<Long,Boolean> servicesRankedBefore=new HashMap<Long,Boolean>();
		Cache.set("servicesRankedBefore", servicesRankedBefore, "30min");

        render(services, tasks,maxPageNumber,tagClouds);

    }
    
    private static  List<STagCloud> getTagCloudData(){
    	String sql="Select st.text as tagText, count(*) as tagCount from STag st group by st.text";
        
        
        Query query = JPA.em().createQuery(sql);
        Iterator i = query.getResultList().iterator();
        List<STagCloud> tagClouds=new ArrayList<STagCloud>();
        while (i.hasNext()) {
            STagCloud stc=new STagCloud();
        	Object[] o = (Object[]) i.next();
            stc.tagText=String.valueOf(o[0]);
            stc.tagCount=Long.valueOf((Long)o[1]);
            tagClouds.add(stc);
        }
        
        return tagClouds;
    }
    private static List<Service> getServicesForPage(List<Service> services,int page){
    	int lastindex=page*serviceNumberPerPage;
    	if(lastindex>services.size()){
    		lastindex=services.size();
    	}
    	return services.subList((page-1)*serviceNumberPerPage, lastindex);
    }

    public static void detail(long serviceId) {
        Service service = Service.findById(serviceId);
        boolean isBossUser = Auth.connected().equals(service.boss.email);
        String userEmail=Auth.connected();
        boolean isAppliedBefore=false;
        SUser currentUser=SUser.findByEmail(Auth.connected());
        List<Comment> serviceComments = new ArrayList<Comment>();
        serviceComments = Comment.findByService(serviceId);
        List<ServiceMatch> serviceMatches=null;
        if(!isBossUser){
        	isAppliedBefore=isApplied(service.applicants,SUser.findByEmail(userEmail));
        }
        else{
        	String sql = "select sm from ServiceMatch sm where sm.serviceOfuser.id="+service.id+
					 		" order by matchPoint desc";
			serviceMatches = ServiceMatch.find(sql,null).fetch();
			Logger.info("%s", serviceMatches);
        	//serviceMatches=ServiceMatch.findByServiceOfUser(service);
        }

        List<Reward> rewards = Reward.findAll();

        render(service, isBossUser,userEmail,isAppliedBefore,currentUser,serviceMatches, serviceComments, rewards);
    }
    
    public static void search(
    		int searchDone,
    		String title,
    		int serviceType,
    		String description,
    		long taskId,
    		String location,
    		String startDate,
    		String endDate,
    		int maxBasePoint,
    		String tags,
    		boolean searchSlots,
    		int dayOfWeek,
    		int hourStart,
    		int minStart,
    		int hourEnd,
    		int minEnd,
    		double locationLat,double locationLng,LocationType locationType,
    		RankingType rankingType,
    		OrderType orderType) {
		Date sd, ed;
		// Map<String,String> errors=new HashMap<String,String>();
		String error = "";
		ServiceSearchCriteria sc = new ServiceSearchCriteria();

		DayOfWeek dayOfWeekEnum = DayOfWeek.values()[dayOfWeek];

		if (searchDone == 1) {
			sc.setDescription(description.trim());
			sc.setEndDate(endDate);
			if(locationType==LocationType.NORMAL){
				sc.setLocation(location.trim());
			}
			sc.setServiceType(serviceType);
			sc.setStartDate(startDate);
			sc.setTitle(title.trim());
			sc.setTaskId(taskId);
			sc.setMaxBasePoint(maxBasePoint);
			sc.setTags(tags.trim());
			
			sc.searchSlots = searchSlots;
			sc.dayOfWeek = dayOfWeekEnum;
			sc.setStartTime(hourStart, minStart);
			sc.setEndTime(hourEnd, minEnd);
			
			sc.locationType=locationType;
			if(locationType==LocationType.NORMAL || locationType==LocationType.ALL){
				sc.locationLat=locationLat;
				sc.locationLng=locationLng;
				System.out.println("set edildi");
			}
			sc.rankingType=rankingType;
			sc.orderType=orderType;
		} else if (searchDone == 2) {
			sc.setTitle(title.trim());
		}

		if (searchDone == 1 && !startDate.equals("") && !endDate.equals("")) {
			SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy");
			try {
				sd = sdf.parse(startDate);
				ed = sdf.parse(endDate);
				if (sd.after(ed)) {
					// errors.put("endDate","endDate must be after start date");
					error = "End date must be after start date";
					searchDone = 0;
				}
			} catch (ParseException e) {
				// FIXME: Find out what to do if this occurs...
			}

		}

		if (searchDone == 0) {

			Collection<Task> tasks = Task.findAllActive();
			List<ServiceType> serviceTypes = new ArrayList<ServiceType>();
			serviceTypes.add(ServiceType.REQUESTS);
			serviceTypes.add(ServiceType.PROVIDES);
			sc.rankingType=RankingType.NONE;
			sc.orderType=OrderType.ASC;
			render(error, serviceTypes, tasks, sc);
		} else if (searchDone == 1) {
			searchList(sc, false);
		} else {
			searchList(sc, true);
		}
	}

	public static void searchList(ServiceSearchCriteria sc, boolean quickSearch) {
		
		
		List<Service> allServices = null;
		Map<Long,Double> distanceMap=null;
		List<Service> servicesOrdered=null;
		if (!quickSearch) {
			allServices = Service.find(prepareQueryForServiceSearch(sc), null)
					.fetch();
		} else {
			allServices = Service.find(
					prepareQueryForQuickServiceSearch(sc.getTitle()), null)
					.fetch();
		}
		
		Cache.set("rankingNone", new Boolean(false), "30min");
		boolean distanceRanking=false;
		
		if(allServices!=null && allServices.size()>0 && 
				sc.locationType==LocationType.ALL || sc.locationType==LocationType.NORMAL){
			Service searchReferenceService=new Service();
			searchReferenceService.title="Search Reference Service";
			searchReferenceService.locationLat=sc.locationLat;
			searchReferenceService.locationLng=sc.locationLng;
			distanceMap=Service.getDistances(searchReferenceService, allServices);
			
			if(sc.rankingType==RankingType.DISTANCE || sc.rankingType==RankingType.NONE){
				if(sc.rankingType==RankingType.NONE || sc.orderType==OrderType.ASC){
					Logger.info("Ranking services with ascending distance...");
					servicesOrdered=rankServicesWithAscendingDistances(allServices, distanceMap);
				}
				else{
					Logger.info("Ranking services with descending distance...");
					servicesOrdered=rankServicesWithDescendingDistances(allServices, distanceMap);
				}		
				distanceRanking=true;
			}
		}
		
		if(!distanceRanking || (distanceRanking && sc.rankingType==RankingType.NONE)){
			if(sc.rankingType==RankingType.POINT){
				if(sc.orderType==OrderType.ASC){
					Logger.info("Ranking services with ascending point...");
					servicesOrdered=rankServicesWithAscendingPoint(allServices);
				}
				else{
					Logger.info("Ranking services with descending point...");
					servicesOrdered=rankServicesWithDescendingPoint(allServices);
				}		
			}
			else if(sc.rankingType==RankingType.NONE){
				Cache.set("rankingNone", new Boolean(true), "30min");
				
				if(sc.locationType==LocationType.ALL || sc.locationType==LocationType.NORMAL){
					Cache.set("distanceRankServices", servicesOrdered, "30min");
					Cache.set("distanceMap", distanceMap, "30min");
					Cache.set("rankingNoneWithDistance", new Boolean(true), "30min");
				}
				else{
					Cache.set("rankingNoneWithDistance", new Boolean(false), "30min");
				}
				
				Logger.info("Getting services ranked by registration date...");
				List<Service> regisDateRankedServices=rankServicesWithAscendingRegistationDate(allServices);
				
				Logger.info("Ranking services with ascending  social point...");
				List<Service> socialPointRankedServices=rankServicesWithAscendingSocalPoint(allServices);
				
				
				Cache.set("regisDateRankedServices", regisDateRankedServices, "30min");
				Cache.set("socialPointRankedServices", socialPointRankedServices, "30min");
				
				Map<Long,Boolean> servicesRankedBefore=new HashMap<Long,Boolean>();
				Cache.set("servicesRankedBefore", servicesRankedBefore, "30min");
				
				servicesOrdered=allServices;
				
			}
			else{
				servicesOrdered=allServices;
			}
		}
		
		int maxPageNumber=0;
        if(servicesOrdered!=null){
        	maxPageNumber=servicesOrdered.size()/serviceNumberPerPage;
        	if(servicesOrdered.size()%serviceNumberPerPage>0)
        		maxPageNumber++;
        }

        List<Service> serializedServices=new ArrayList<Service>();
        List<Service> services=new ArrayList<Service>();

        for (Service service : servicesOrdered) {
        	serializedServices.add(service);
        	
        	if(sc.searchSlots && service.slots != null && service.slots.size() != 0) {
				for (ServiceAvailabilitySlot serviceSlot : service.slots) {
					if (serviceSlot.dayOfWeek==sc.dayOfWeek) {
						if (sc.startTimeMinutesAfterMidnight <= serviceSlot.endTimeMinutesAfterMidnight
								&& sc.endTimeMinutesAfterMidnight >= serviceSlot.startTimeMinutesAfterMidnight) {
							services.add(service);
							break;
						}
					}
				}
        	}
        	else {
        	    services.add(service);
        	}
		}
		Cache.set("filteredServices", services, "30min");

		if(distanceMap!=null){
			Cache.set("distanceMap", distanceMap, "30min");
		}
		Collection<Task> tasks = Task.findAllActive();
		render(services, tasks,maxPageNumber,distanceMap);
	}

	private static List<Service> rankServicesWithAscendingDistances(List<Service> allServices,Map<Long,Double> distanceMap){
		List<Service> servicesOrderedBydistance=new ArrayList<Service>();
		
		int center=-1;
		int left=-1;
		int right=-1;
		for(int i=0;i<allServices.size();i++){
		    Service s=allServices.get(i);
		    
		    double distance=distanceMap.get(new Long(s.id)).doubleValue();
		    left=0;
			right=servicesOrderedBydistance.size();
			center=(left+right)/2;
			if(i==0){
				servicesOrderedBydistance.add(s);
			}
			else{
				boolean found=false;
				while(!found && center>=0 && center<servicesOrderedBydistance.size()){
					Service centerService=servicesOrderedBydistance.get(center);
					double distanceOfCService=distanceMap.get(new Long(centerService.id));
					Logger.info("Start left=%d right=%d center=%d distance=%s distcanceOfCenter=%s",
							left,right,center,""+distance,""+distanceOfCService);
					if(distance==distanceOfCService){
						servicesOrderedBydistance.add(center,s);
						found=true;
						Logger.info("Add to index:%d",center);
					}
					else if(left>=right){
						if(distanceOfCService>distance){
							servicesOrderedBydistance.add(center,s);
							found=true;
							Logger.info("Add to index:%d",center);
						}
						else if(distanceOfCService<distance){
							servicesOrderedBydistance.add(center+1,s);
							found=true;
							Logger.info("Add to index:%d",(center+1));
						}
					}
					else if(distanceOfCService<distance){
						left=center+1;
					}
					else if(distanceOfCService>distance){
						right=center-1;
					}
					center=(left+right)/2;
					Logger.info("End left=%d right=%d center=%d",left,right,center);
				}
				if(!found){
					servicesOrderedBydistance.add(servicesOrderedBydistance.size(),s);
					Logger.info("Add to index:%d",(servicesOrderedBydistance.size()-1));
				}
			}
		}
		return servicesOrderedBydistance;
	}
	private static List<Service> rankServicesWithAscendingPoint(List<Service> allServices){
		List<Service> servicesOrderedBypoint=new ArrayList<Service>();
		
		int center=-1;
		int left=-1;
		int right=-1;
		for(int i=0;i<allServices.size();i++){
		    Service s=allServices.get(i);
		    
		    float point=s.task.point;
		    left=0;
			right=servicesOrderedBypoint.size();
			center=(left+right)/2;
			if(i==0){
				servicesOrderedBypoint.add(s);
			}
			else{
				boolean found=false;
				while(!found && center>=0 && center<servicesOrderedBypoint.size()){
					Service centerService=servicesOrderedBypoint.get(center);
					float pointOfCService=centerService.task.point;
					Logger.info("Start left=%d right=%d center=%d point=%s pointOfCenter=%s",
							left,right,center,""+point,""+pointOfCService);
					if(point==pointOfCService){
						servicesOrderedBypoint.add(center,s);
						found=true;
						Logger.info("Add to index:%d",center);
					}
					else if(left>=right){
						if(pointOfCService>point){
							servicesOrderedBypoint.add(center,s);
							found=true;
							Logger.info("Add to index:%d",center);
						}
						else if(pointOfCService<point){
							servicesOrderedBypoint.add(center+1,s);
							found=true;
							Logger.info("Add to index:%d",(center+1));
						}
					}
					else if(pointOfCService<point){
						left=center+1;
					}
					else if(pointOfCService>point){
						right=center-1;
					}
					center=(left+right)/2;
					Logger.info("End left=%d right=%d center=%d",left,right,center);
				}
				if(!found){
					servicesOrderedBypoint.add(servicesOrderedBypoint.size(),s);
					Logger.info("Add to index:%d",(servicesOrderedBypoint.size()-1));
				}
			}
		}
		return servicesOrderedBypoint;
	}
	private static List<Service> rankServicesWithAscendingSocalPoint(List<Service> allServices){
		List<Service> servicesOrderedBypoint=new ArrayList<Service>();
		
		int center=-1;
		int left=-1;
		int right=-1;
		for(int i=0;i<allServices.size();i++){
		    Service s=allServices.get(i);
		    
		    long socialPoint=s.boss.getAbsoluteSocialPoint();
		    left=0;
			right=servicesOrderedBypoint.size();
			center=(left+right)/2;
			if(i==0){
				servicesOrderedBypoint.add(s);
			}
			else{
				boolean found=false;
				while(!found && center>=0 && center<servicesOrderedBypoint.size()){
					Service centerService=servicesOrderedBypoint.get(center);
					long socialPointOfCService=centerService.boss.getAbsoluteSocialPoint();
					Logger.info("Start left=%d right=%d center=%d socailPoint=%s socialPointOfCenter=%s",
							left,right,center,""+socialPoint,""+socialPointOfCService);
					if(socialPoint==socialPointOfCService){
						servicesOrderedBypoint.add(center,s);
						found=true;
						Logger.info("Add to index:%d",center);
					}
					else if(left>=right){
						if(socialPointOfCService>socialPoint){
							servicesOrderedBypoint.add(center,s);
							found=true;
							Logger.info("Add to index:%d",center);
						}
						else if(socialPointOfCService<socialPoint){
							servicesOrderedBypoint.add(center+1,s);
							found=true;
							Logger.info("Add to index:%d",(center+1));
						}
					}
					else if(socialPointOfCService<socialPoint){
						left=center+1;
					}
					else if(socialPointOfCService>socialPoint){
						right=center-1;
					}
					center=(left+right)/2;
					Logger.info("End left=%d right=%d center=%d",left,right,center);
				}
				if(!found){
					servicesOrderedBypoint.add(servicesOrderedBypoint.size(),s);
					Logger.info("Add to index:%d",(servicesOrderedBypoint.size()-1));
				}
			}
		}
		return servicesOrderedBypoint;
	}
	private static List<Service> rankServicesWithAscendingRegistationDate(List<Service> allServices){
		List<Service> servicesOrderedRegistrationDate=new ArrayList<Service>();
		
		int center=-1;
		int left=-1;
		int right=-1;
		for(int i=0;i<allServices.size();i++){
		    Service s=allServices.get(i);
		    
		    Date rDate=s.boss.registrationTime;
		    left=0;
			right=servicesOrderedRegistrationDate.size();
			center=(left+right)/2;
			if(i==0){
				servicesOrderedRegistrationDate.add(s);
			}
			else{
				boolean found=false;
				while(!found && center>=0 && center<servicesOrderedRegistrationDate.size()){
					Service centerService=servicesOrderedRegistrationDate.get(center);
					Date rDateOfCService=centerService.boss.registrationTime;
					Logger.info("Start left=%d right=%d center=%d rDate=%s rDateOfCenter=%s",
							left,right,center,""+s.boss.getFormattedRegistrationDate(),""+centerService.boss.getFormattedRegistrationDate());
					if(rDate.equals(rDateOfCService)){
						servicesOrderedRegistrationDate.add(center,s);
						found=true;
						Logger.info("Add to index:%d",center);
					}
					else if(left>=right){
						if(rDateOfCService.before(rDate)){
							servicesOrderedRegistrationDate.add(center,s);
							found=true;
							Logger.info("Add to index:%d",center);
						}
						else if(rDateOfCService.after(rDate)){
							servicesOrderedRegistrationDate.add(center+1,s);
							found=true;
							Logger.info("Add to index:%d",(center+1));
						}
					}
					else if(rDateOfCService.after(rDate)){
						left=center+1;
					}
					else if(rDateOfCService.before(rDate)){
						right=center-1;
					}
					center=(left+right)/2;
					Logger.info("End left=%d right=%d center=%d",left,right,center);
				}
				if(!found){
					servicesOrderedRegistrationDate.add(servicesOrderedRegistrationDate.size(),s);
					Logger.info("Add to index:%d",(servicesOrderedRegistrationDate.size()-1));
				}
			}
		}
		return servicesOrderedRegistrationDate;
	}
	private static List<Service> rankServicesWithDescendingPoint(List<Service> allServices){
		List<Service> servicesOrderedBypoint=new ArrayList<Service>();
		
		int center=-1;
		int left=-1;
		int right=-1;
		for(int i=0;i<allServices.size();i++){
		    Service s=allServices.get(i);
		    
		    float point=s.task.point;
		    left=0;
			right=servicesOrderedBypoint.size();
			center=(left+right)/2;
			if(i==0){
				servicesOrderedBypoint.add(s);
			}
			else{
				boolean found=false;
				while(!found && center>=0 && center<servicesOrderedBypoint.size()){
					Service centerService=servicesOrderedBypoint.get(center);
					float pointOfCService=centerService.task.point;
					Logger.info("Start left=%d right=%d center=%d point=%s pointOfCenter=%s",
							left,right,center,""+point,""+pointOfCService);
					if(point==pointOfCService){
						servicesOrderedBypoint.add(center,s);
						found=true;
						Logger.info("Add to index:%d",center);
					}
					else if(left>=right){
						if(pointOfCService<point){
							servicesOrderedBypoint.add(center,s);
							found=true;
							Logger.info("Add to index:%d",center);
						}
						else if(pointOfCService>point){
							servicesOrderedBypoint.add(center+1,s);
							found=true;
							Logger.info("Add to index:%d",(center+1));
						}
					}
					else if(pointOfCService>point){
						left=center+1;
					}
					else if(pointOfCService<point){
						right=center-1;
					}
					center=(left+right)/2;
					Logger.info("End left=%d right=%d center=%d",left,right,center);
				}
				if(!found){
					servicesOrderedBypoint.add(servicesOrderedBypoint.size(),s);
					Logger.info("Add to index:%d",(servicesOrderedBypoint.size()-1));
				}
			}
		}
		return servicesOrderedBypoint;
	}
	private static List<Service> rankServicesWithDescendingDistances(List<Service> allServices,Map<Long,Double> distanceMap){
		List<Service> servicesOrderedBydistance=new ArrayList<Service>();
		
		int center=-1;
		int left=-1;
		int right=-1;
		for(int i=0;i<allServices.size();i++){
		    Service s=allServices.get(i);
		    
		    double distance=distanceMap.get(new Long(s.id)).doubleValue();
		    left=0;
			right=servicesOrderedBydistance.size();
			center=(left+right)/2;
			if(i==0){
				servicesOrderedBydistance.add(s);
				
				Logger.info("First service is added to index:0");
			}
			else{
				boolean found=false;
				while(!found && center>=0 && center<servicesOrderedBydistance.size()){
					Service centerService=servicesOrderedBydistance.get(center);
					double distanceOfCService=distanceMap.get(new Long(centerService.id));
					Logger.info("Start left=%d right=%d center=%d distance=%s distcanceOfCenter=%s",
							left,right,center,""+distance,""+distanceOfCService);
					if(distance==distanceOfCService){
						servicesOrderedBydistance.add(center,s);
						found=true;
						Logger.info("Add to index:%d",center);
					}
					else if(left>=right){
						if(distanceOfCService<distance){
							servicesOrderedBydistance.add(center,s);
							found=true;
							Logger.info("Add to index:%d",center);
						}
						else if(distanceOfCService>distance){
							servicesOrderedBydistance.add(center+1,s);
							found=true;
							Logger.info("Add to index:%d",(center+1));
						}
					}
					else if(distanceOfCService>distance){
						left=center+1;
					}
					else if(distanceOfCService<distance){
						right=center-1;
					}
					center=(left+right)/2;
					Logger.info("End left=%d right=%d center=%d",left,right,center);
				}
				if(!found){
					servicesOrderedBydistance.add(servicesOrderedBydistance.size(),s);
					Logger.info("Add to index:%d",(servicesOrderedBydistance.size()-1));
				}
			}
		}
		return servicesOrderedBydistance;
	}
	public static void apply(long serviceId,String email) throws Exception {
	        Service service = Service.findById(serviceId);
	        SUser user=SUser.findByEmail(email);
	        List<SUser> applicants = service.applicants;
	        boolean isBossUser = service.boss.email.equals(email);
	        if (service.status==ServiceStatus.PUBLISHED && !isBossUser && !isApplied(applicants, user)) {
		        if (applicants==null) {
		        	applicants = new ArrayList<SUser>();
		        }
		        
                Activity a = new Activity();
                a.performer = user;
                a.affectedService = service;
                a.type = ActivityType.APPLIED_SERVICE;
                a.affectedUsers.add(service.boss);
                a.affectedUsers.addAll(service.applicants);
                a.save();
                
                applicants.add(user);
		        service.save();
	        } else {
	        	//APPLIED BEFORE
	        }
		        
	        detail(service.id);
	}
	public static void bossClose(long serviceId,
                                     String email,
                                     String bossComment,
                                     String employeeEmail,
                                     List<String> rewards) throws Exception {
        Service service = Service.findById(serviceId);
        SUser user=SUser.findByEmail(email);
        SUser employer = SUser.findByEmail(employeeEmail);
        boolean isBossUser = service.boss.email.equals(email);
        Logger.info("employeeEmail:" + employeeEmail);

        if (service.status == ServiceStatus.IN_PROGRESS && isBossUser) {
            if (rewards != null && rewards.size() > 0) {
                service.rewards = new HashSet<Reward>();
                for (String rewardIdStr : rewards) {
                    Reward reward = Reward.findById(Long.parseLong(rewardIdStr));
                    service.rewards.add(reward);
                }
            }
            
            service.status=ServiceStatus.WAITING_EMPLOYEE_FINISH;
            service.save();
	    
            Activity a = new Activity();
            a.performer = user;
            a.affectedService = service;
            a.type = ActivityType.FINISHED_SERVICE;
            a.affectedUsers.add(employer);
            a.save();

            if (rewards != null && rewards.size() > 0) {
                Activity rewardActivity = new Activity();
                rewardActivity.performer = service.boss;
                rewardActivity.affectedService = service;
                rewardActivity.type = ActivityType.REWARD_GIVEN;
                rewardActivity.affectedUsers.add(service.employee);
                rewardActivity.save();
            }
        }

        if (bossComment != null && !"".equals(bossComment)) {
            Comment comment = new Comment(user, SUser.findByEmail(employeeEmail), bossComment);
            Calendar calendar = Calendar.getInstance();
            SimpleDateFormat  formatterTime = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
            SimpleDateFormat  formatterDate = new SimpleDateFormat("dd.MM.yyyy");
            Logger.info("Comment Date:" + formatterTime.format(calendar.getTime()));
            try {
            	comment.commentDate = formatterDate.parse(formatterDate.format(calendar.getTime()));
            } catch (ParseException e) {
                //FIXME: Find out what to do if this occurs...
            }    
            comment.commentDateWithTime = formatterTime.format(calendar.getTime());
            comment.service = service;
            comment.save();
        }
        detail(service.id);
    }
	
	public static void employeeClose(long serviceId,
                                         String email,
                                         String employeeComment,
                                         String bossEmail,
                                         List<String> rewards) throws Exception {
        Service service = Service.findById(serviceId);
        SUser user=SUser.findByEmail(email);

        if (service.status == ServiceStatus.WAITING_EMPLOYEE_FINISH && service.employee.id == user.id) {
            if (rewards != null && rewards.size() > 0) {
                service.rewards = new HashSet<Reward>();
                for (String rewardIdStr : rewards) {
                    Reward reward = Reward.findById(Long.parseLong(rewardIdStr));
                    service.rewards.add(reward);
                }
            }

            service.status=ServiceStatus.FINISHED;
            service.employee.providerPoint+=service.task.point;
            service.boss.requesterPoint+=service.task.point;
            service.save();
	        
            Activity a = new Activity();
            a.performer = user;
            a.affectedService = service;
            a.type = ActivityType.FINISHED_SERVICE;
            a.affectedUsers.add(service.boss);
            a.save();

            if (rewards != null && rewards.size() > 0) {
                Activity rewardActivity = new Activity();
                rewardActivity.performer = service.employee;
                rewardActivity.affectedService = service;
                rewardActivity.type = ActivityType.REWARD_GIVEN;
                rewardActivity.affectedUsers.add(service.boss);
                rewardActivity.save();
            }
        }

        if (employeeComment != null && !"".equals(employeeComment)) {
	        Comment comment = new Comment(user, SUser.findByEmail(bossEmail), employeeComment);
	        Calendar calendar = Calendar.getInstance();
	        SimpleDateFormat  formatterTime = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
	        SimpleDateFormat  formatterDate = new SimpleDateFormat("dd.MM.yyyy");
	        Logger.info("Comment Date:" + formatterTime.format(calendar.getTime()));
	        try {
	        	comment.commentDate = formatterDate.parse(formatterDate.format(calendar.getTime()));
	        } catch (ParseException e) {
	            //FIXME: Find out what to do if this occurs...
	        }    
	        comment.commentDateWithTime = formatterTime.format(calendar.getTime());
	        comment.service = service;
	        comment.save();
        }
        detail(service.id);
}

	public static void cancelApply(long serviceId,String email) throws Exception {
        Service service = Service.findById(serviceId);
        SUser user=SUser.findByEmail(email);
        List<SUser> applicants=service.applicants;
        int index = findUserIndex(applicants, user);
        if (index != -1) {
	        applicants.remove(index);
	        service.save();
	        
	        Activity a = new Activity();
            a.performer = user;
            a.affectedService = service;
            a.type = ActivityType.CANCEL_APPLICATION_SERVICE;
            a.affectedUsers.add(service.boss);
            a.affectedUsers.addAll(service.applicants);
            a.save();
        } else {
        	//NOT APPLIED BEFORE
        }
	                
        detail(service.id);
	}
	public static void startApproval(long serviceId,String email) throws Exception {
		
        Service service = Service.findById(serviceId);
        SUser user = SUser.findByEmail(email);
        List<SUser> applicants = service.applicants;
        boolean isBossUser = Auth.connected().equals(service.boss.email);
        int index = findUserIndex(applicants, user);
        if (isBossUser && index != -1) {
            render(service,user);
        } else {
        	//NOT APPLIED BEFORE	
        }
	}
	
	public static void processStartApproval(long serviceId, long applicantId, 
											String actualDate, String location) throws Exception {	
        Service service = Service.findById(serviceId);
        SUser user = SUser.findById(applicantId);
        List<SUser> applicants = service.applicants;
        boolean isBossUser = Auth.connected().equals(service.boss.email);
        int index = findUserIndex(applicants, user);
        if (isBossUser && index != -1 && service.status == ServiceStatus.PUBLISHED) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy");
            try {
            	service.actualDate = sdf.parse(actualDate);
            } catch (ParseException e) {
                //FIXME: Find out what to do if this occurs...
            }
            service.location = location;
            service.status = ServiceStatus.WAITING_EMPLOYEE_APPROVAL;
            service.employee = user;
            service.save();
            
            Activity a = new Activity();
            a.performer = user;
            a.affectedService = service;
            a.type = ActivityType.STARTED_SERVICE;
            a.affectedUsers.addAll(applicants);
            a.save();
            
            detail(serviceId);
        } else {
        	//NOT APPLIED BEFORE
        }
	      
	}
	
	public static void employeeApproval(long serviceId, long applicantId) {
	    
		Service service = Service.findById(serviceId);
        SUser user = SUser.findById(applicantId);
        if (null == service || null == user) {
            index();
        }
		render(service, user);
	}
	
	public static void processEmployeeApproval(long serviceId,long applicantId,int approval){
		Service service = Service.findById(serviceId);
        SUser user = SUser.findById(applicantId);
        List<SUser> applicants = service.applicants;
        //boolean isBossUser = Auth.connected().equals(service.boss.email);
        int index = findUserIndex(applicants, user);
        if (approval == 1 && index != -1 && service.status == ServiceStatus.WAITING_EMPLOYEE_APPROVAL) {
            
            Activity a = new Activity();
            a.performer = user;
            a.affectedService = service;
            a.type = ActivityType.STARTED_SERVICE;
            a.affectedUsers.add(service.boss);
            a.affectedUsers.addAll(service.applicants);
            a.save();
            
        	service.status = ServiceStatus.IN_PROGRESS;
        	service.applicants = null;
			service.save();
			
			List<Service> servicesAsEmployee = user.servicesAsEmployee;
			if (servicesAsEmployee != null) {
				servicesAsEmployee = new ArrayList<Service>();
			}
			servicesAsEmployee.add(service);
			user.save();
        }
		if (approval != 1 && index != -1) {
			service.status = ServiceStatus.PUBLISHED;
			service.employee = null;
			service.save();
			
			int serviceIndex = findServiceIndex(user.appliedServices, service);
			if (serviceIndex != -1) {
				user.appliedServices.remove(serviceIndex);
				user.save();
			}
			
			Activity a = new Activity();
            a.performer = user;
            a.affectedService = service;
            a.type = ActivityType.REJECTED_SERVICE_OFFER;
            a.affectedUsers.add(service.boss);
            a.affectedUsers.addAll(service.applicants);
            a.save();
			
		} else {
			//NOT AN APPLICANT
		}
		detail(serviceId);
		
	}
	
	private static boolean isApplied(List<SUser> applicants,SUser user){
		int index = findUserIndex(applicants, user);
		return index != -1;
	}
	private static int findUserIndex(List<SUser> applicants,SUser user){
		int result=-1;
		if (applicants != null) {
			for (int i = 0; i < applicants.size() && result == -1; i++) {
				SUser applicant = applicants.get(i);
				if (applicant.id == user.id) {
					result = i;
				}
			}
		}
		return result;
	}
	private static int findServiceIndex(List<Service> services,Service service){
		int result = -1;
		if (services != null) {
			for (int i=0; i < services.size() && result == -1; i++) {
				Service s = services.get(i);
				if (s.id == service.id) {
					result = i;
				}
			}
		}
		return result;
	}
	private static String prepareQueryForQuickServiceSearch(String title) {
		String sql = "select s from Service s, Task t where s.task=t";
		sql += " and s.title LIKE '%" + title + "%'";
		return sql;
	}

	private static String prepareQueryForServiceSearch(ServiceSearchCriteria sc) {
		String sql = "select distinct s from Service s, Task t ";
		
		if (!sc.getTags().trim().equals("")) {
			sql+=",STag st where s.task=t and st.service=s";
		}
		else{
			sql+="where s.task=t";
		}

		if (sc.getTaskId() != -1) {
			Task task = Task.findById(sc.getTaskId());
			sql += " and s.task=" + task.id;
		}

		if (!sc.getTitle().equals("")) {
			sql += " and s.title LIKE '%" + sc.getTitle() + "%'";
		}

		if (!sc.getDescription().equals("")) {
			sql += " and s.description LIKE '%" + sc.getDescription() + "%'";
		}

		if (!sc.getDescription().equals("")) {
			sql += " and s.description LIKE '%" + sc.getDescription() + "%'";
		}

		if (sc.getMaxBasePoint() != -1) {
			sql += " and t.point<=" + sc.getMaxBasePoint();
		}

		if (sc.locationType==LocationType.NORMAL && !sc.getLocation().equals("")) {
			sql += " and s.location LIKE '%" + sc.getLocation() + "%'";
		}

		if (sc.getServiceType() != -1) {
			sql += " and s.type=" + sc.getServiceType();
		}
		if (!sc.getTags().trim().equals("")) {
			StringTokenizer st=new StringTokenizer(sc.getTags().trim()," ");
			sql += " and (";
			while(st.hasMoreTokens()){
				sql+=" st.text LIKE '" + st.nextToken()+"%'";
				if(st.hasMoreTokens()){
					sql+=" or";
				}
			}
			sql += ")";
		}
		if (!sc.getStartDate().equals("")) {
			String sd = "";
			StringTokenizer st = new StringTokenizer(sc.getStartDate(), ".");
			String d = st.nextToken();
			String m = st.nextToken();
			String y = st.nextToken();
			sd = y + "-" + m + "-" + d;
			sql += " and s.startDate>='" + sd + "'";
		}
		if (!sc.getEndDate().equals("")) {
			String ed = "";
			StringTokenizer st = new StringTokenizer(sc.getEndDate(), ".");
			String d = st.nextToken();
			String m = st.nextToken();
			String y = st.nextToken();
			ed = y + "-" + m + "-" + d;
			sql += " and s.endDate<='" + ed + "'";
		}
		if(sc.locationType!=LocationType.ALL){
			sql += " and s.locationType="+sc.locationType.getOrdinal();
		}
		
		return sql;
	}

	public static void ajaxDeneme() {

		String suggestword = params.get("suggestword");
		String query = "select s.name from SUser s where s.name like '"
				+ suggestword + "%'";
		List<SUser> users = SUser.find(query).fetch();
		renderJSON(users);
	}
	public static void listPage() {
	 	int page=1;	
    	if(params.get("page")!=null){
    		page=Integer.parseInt(params.get("page"));
    	}
    	
    	List<Service> services = null;
    	Collection<Task> tasks = Task.findWithWeights();
    	Map<Long,Double> distanceMap=Cache.get("distanceMap",Map.class);
    	Boolean rankingNone=Cache.get("rankingNone",Boolean.class);

    	if(rankingNone.booleanValue()==true){
    		
    		Logger.info("Ranking is none. Hybrid list will be prepared.");
    		
    		Boolean rankingNoneWithDistance=Cache.get("rankingNoneWithDistance",Boolean.class);
    		List<Service> regisDateRankedServices=Cache.get("regisDateRankedServices",List.class);
    		List<Service> socialPointRankedServices=Cache.get("socialPointRankedServices",List.class);
    		Map<Long,Boolean> servicesRankedBefore=Cache.get("servicesRankedBefore",Map.class);
    		
    		if(rankingNoneWithDistance.booleanValue()==true){
    			List<Service> distanceRankServices=Cache.get("distanceRankServices",List.class);
    			Logger.info("Hybrid list with distance is prepared.");
    			services=prepareHybridRankServicesWithDistance(regisDateRankedServices, socialPointRankedServices, distanceRankServices, servicesRankedBefore, page);
    		}
    		else{
    			Logger.info("Hybrid list without distance is prepared.");
    			services=prepareHybridRankServicesWithoutDistance(regisDateRankedServices, socialPointRankedServices, servicesRankedBefore, page);
    		}
    		Cache.set("servicesRankedBefore", servicesRankedBefore, "30min");
    		
    		if(services.size()==0){
    			services=Cache.get("servicesAddedBefore"+page,List.class);
    		}
    		else{
    			Cache.set("servicesAddedBefore"+page, services, "30min");
    		}
    	}
    	else{
    	
	        //services = Service.findAll();
	        services=Cache.get("filteredServices",List.class);
	
	        int maxPageNumber=0;
	        if(services!=null){
	        	maxPageNumber=services.size()/serviceNumberPerPage;
	        	maxPageNumber+=services.size()%serviceNumberPerPage;
	        }
	        if(maxPageNumber!=0){
	        	services=getServicesForPage(services, page);
	        }
    	}
        
        render(services, tasks,distanceMap);
 }
	
	private static List<Service> prepareHybridRankServicesWithDistance(List<Service> regisDateRankedServices,
														   List<Service> socialPointRankedServices,
														   List<Service> distanceRankServices,
														   Map<Long,Boolean> servicesRankedBefore,int page) {
		
		List<Service> result=new ArrayList<Service>();
		int startFrom=2*(page-1);
		
		int index=startFrom;
		int added=0;
		while(index<regisDateRankedServices.size() && added<2){
			Service s=regisDateRankedServices.get(index);
			Boolean isAddedBefore=servicesRankedBefore.get(new Long(s.id));
			
			if(isAddedBefore==null || isAddedBefore.booleanValue()==false){
				result.add(s);
				servicesRankedBefore.put(new Long(s.id), new Boolean(true));
				added++;
				Logger.info("Service with id %s is added(Regist date). index=%d added=%d",""+s.id,index,added);
			}
			index++;
		}
		
		index=startFrom;
		added=0;
		while(index<socialPointRankedServices.size() && added<2){
			Service s=socialPointRankedServices.get(index);
			Boolean isAddedBefore=servicesRankedBefore.get(new Long(s.id));
			
			if(isAddedBefore==null || isAddedBefore.booleanValue()==false){
				result.add(s);
				servicesRankedBefore.put(new Long(s.id), new Boolean(true));
				added++;
				Logger.info("Service with id %s is added(Social Point). index=%d added=%d",""+s.id,index,added);
			}
			index++;
		}
		
		index=startFrom;
		added=0;
		while(index<distanceRankServices.size() && added<2){
			Service s=distanceRankServices.get(index);
			Boolean isAddedBefore=servicesRankedBefore.get(new Long(s.id));
			
			if(isAddedBefore==null || isAddedBefore.booleanValue()==false){
				result.add(s);
				servicesRankedBefore.put(new Long(s.id), new Boolean(true));
				added++;
				Logger.info("Service with id %s is added(Distance). index=%d added=%d",""+s.id,index,added);
			}
			index++;
		}
			
		return result;
	}
	private static List<Service> prepareHybridRankServicesWithoutDistance(List<Service> regisDateRankedServices,
			   List<Service> socialPointRankedServices,
			   Map<Long,Boolean> servicesRankedBefore,int page) {

				List<Service> result=new ArrayList<Service>();
				int startFrom=3*(page-1);
				
				int index=startFrom;
				int added=0;
				while(index<regisDateRankedServices.size() && added<3){
					Service s=regisDateRankedServices.get(index);
					Boolean isAddedBefore=servicesRankedBefore.get(new Long(s.id));
					
					if(isAddedBefore==null || isAddedBefore.booleanValue()==false){
						result.add(s);
						servicesRankedBefore.put(new Long(s.id), new Boolean(true));
						added++;
						Logger.info("Service with id %s is added(Regist date). index=%d added=%d",""+s.id,index,added);
					}
					index++;
				}
				
				index=startFrom;
				added=0;
				while(index<socialPointRankedServices.size() && added<3){
					Service s=socialPointRankedServices.get(index);
					Boolean isAddedBefore=servicesRankedBefore.get(new Long(s.id));
					
					if(isAddedBefore==null || isAddedBefore.booleanValue()==false){
						result.add(s);
						servicesRankedBefore.put(new Long(s.id), new Boolean(true));
						added++;
						Logger.info("Service with id %s is added(Social point). index=%d added=%d",""+s.id,index,added);
					}
					index++;
				}
				
				return result;
	}
}
