package controllers;

import play.*;
import play.cache.Cache;
import play.db.jpa.JPA;
import play.mvc.*;

import java.io.Serializable;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import javax.persistence.Query;

import org.apache.commons.lang.SerializationUtils;
import org.joda.time.DateTime;

import models.*;

@With(Secure.class)
@Check("user")
public class Services extends BaseController {
	
	public static int serviceNumberPerPage = 5;


    public static void index() {
        Service service = new Service();
        service.type = ServiceType.REQUESTS;
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
    		String location, String startDate, String endDate,String tags, List<String> slots) {
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
        
        for (String slot: slots) {
        	String[] parts = slot.split(",");
        	
        	DayOfWeek day = DayOfWeek.values()[Integer.valueOf(parts[0])];
        	int hourStart = Integer.valueOf(parts[1]);
        	int minuteStart = Integer.valueOf(parts[2]);
        	int hourEnd = Integer.valueOf(parts[3]);
        	int minuteEnd = Integer.valueOf(parts[4]);

        	service.addSlot(day, hourStart, minuteStart, hourEnd, minuteEnd);
        }

        service.title = title;
        service.description = description;
        service.type = type;
        service.location = location;
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
        
        findMatchServices(service,isUpdate);
        
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
        Cache.set("listServices", serializedServices, "30min");
        
        
        if (maxPageNumber != 0) {
        	services = getServicesForPage(services, 1);
        }
       
        List<STagCloud> tagClouds=getTagCloudData();
        
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
			serviceMatches=ServiceMatch.find(sql,null).fetch();
        	//serviceMatches=ServiceMatch.findByServiceOfUser(service);
        }
        render(service, isBossUser,userEmail,isAppliedBefore,currentUser,serviceMatches, serviceComments);
    }
    
    public static void search(int searchDone,String title, int serviceType, String description, long taskId, String location, String startDate, String endDate, int maxBasePoint,String tags) {
		Date sd, ed;
		// Map<String,String> errors=new HashMap<String,String>();
		String error = "";
		ServiceSearchCriteria sc = new ServiceSearchCriteria();

		if (searchDone == 1) {
			sc.setDescription(description.trim());
			sc.setEndDate(endDate);
			sc.setLocation(location.trim());
			sc.setServiceType(serviceType);
			sc.setStartDate(startDate);
			sc.setTitle(title.trim());
			sc.setTaskId(taskId);
			sc.setMaxBasePoint(maxBasePoint);
			sc.setTags(tags.trim());
		} else if (searchDone == 2) {
			//System.out.println("title=" + title);
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
			render(error, serviceTypes, tasks, sc);
		} else if (searchDone == 1) {
			searchList(sc, false);
		} else {
			searchList(sc, true);
		}
	}

	public static void searchList(ServiceSearchCriteria sc, boolean quickSearch) {

		Collection<Service> services = null;
		if (!quickSearch) {
			services = Service.find(prepareQueryForServiceSearch(sc), null)
					.fetch();
		} else {
			services = Service.find(
					prepareQueryForQuickServiceSearch(sc.getTitle()), null)
					.fetch();
		}
		
		int maxPageNumber=0;
        if(services!=null){
        	maxPageNumber=services.size()/serviceNumberPerPage;
        	if(services.size()%serviceNumberPerPage>0)
        		maxPageNumber++;
        }
        
        List<Service> serializedServices=new ArrayList<Service>();
        for (Service service : services) {
        	serializedServices.add(service);
		}
        Cache.set("listServices", serializedServices, "30min");
		
		Collection<Task> tasks = Task.findAllActive();
		render(services, tasks,maxPageNumber);
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
	public static void bossClose(long serviceId,String email, String bossComment, String employeeEmail) throws Exception {
        Service service = Service.findById(serviceId);
        SUser user=SUser.findByEmail(email);
        SUser employer = SUser.findByEmail(employeeEmail);
        boolean isBossUser = service.boss.email.equals(email);
        Logger.info("employeeEmail:" + employeeEmail);
         if(service.status==ServiceStatus.IN_PROGRESS && isBossUser ){
	     
	        service.status=ServiceStatus.WAITING_EMPLOYEE_FINISH;
	        service.save();
	        
	        Activity a = new Activity();
            a.performer = user;
            a.affectedService = service;
            a.type = ActivityType.FINISHED_SERVICE;
            a.affectedUsers.add(employer);
            a.save();
	        
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
	
	public static void employeeClose(long serviceId,String email, String employeeComment, String bossEmail) throws Exception {
        Service service = Service.findById(serviceId);
        SUser user=SUser.findByEmail(email);

        if(service.status==ServiceStatus.WAITING_EMPLOYEE_FINISH && service.employee.id==user.id){
	      
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

		if (!sc.getLocation().equals("")) {
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
       
        //services = Service.findAll();
        services=Cache.get("listServices",List.class);
       
        Collection<Task> tasks = Task.findWithWeights();
        
        int maxPageNumber=0;
        if(services!=null){
        	maxPageNumber=services.size()/serviceNumberPerPage;
        	maxPageNumber+=services.size()%serviceNumberPerPage;
        }
        if(maxPageNumber!=0){
        	services=getServicesForPage(services, page);
        }
        
        render(services, tasks);
 }
	private  static void findMatchServices(Service service, boolean isUpdate){
		
		if(isUpdate){
			String sql = "select sm from ServiceMatch sm where sm.serviceOfuser.id="+service.id+" " +
					 " or sm.matchService.id="+service.id;
			List<ServiceMatch> serviceMatches=ServiceMatch.find(sql,null).fetch();
			for (ServiceMatch sm : serviceMatches) {
				Logger.info("sm with id %d and wiht service %s is deleted", sm.id,sm.serviceOfuser.title);
				sm.delete();
			}
		}
		
		ServiceType type=service.type;
		int ordinal;
		if(type==ServiceType.PROVIDES){
			ordinal=ServiceType.REQUESTS.getOrdinal();
		}
		else{
			ordinal=ServiceType.PROVIDES.getOrdinal();
		}
		
		String sql = "select s from Service s where s.type="+ordinal+" " +
					 " and s.boss.id!="+service.boss.id+" and s.status="+ServiceStatus.PUBLISHED.ordinal();
		List<Service> services=Service.find(sql,null).fetch();
		
		Logger.info("Find match services size:%d services with ordinal %d found ",services.size(),ordinal);
		
		for (Service s : services) {
			int matchPoint=1;
			String taskName=s.task.name;
			if(taskName.toLowerCase().equals(service.task.name.toLowerCase())){
				matchPoint++;
				Logger.info("TaskName1:%s TaskName2:%s. Match Point is incremented to:%d", service.task.name,taskName,matchPoint);
			}
			if(s.stags!=null && service.stags!=null && s.stags.size()>0 && service.stags.size()>0){
				Set<STag> tags=s.stags;
				for (STag tag : tags) {
					String tText=tag.text.toLowerCase();
					for (STag t : service.stags) {
						String tText2=t.text.toLowerCase();
						if(tText.equals(tText2) || tText.contains(tText2) || tText2.contains(tText)){
							matchPoint++;
							Logger.info("Tag1:%s Tag2:%s. Match Point is incremented to:%d", tText2,tText,matchPoint);
						}
					}
				}
			}
			
			String title=s.title.trim().toLowerCase();
			if(title.contains(service.title.trim().toLowerCase()) || service.title.trim().toLowerCase().contains(title)){
				matchPoint++;
				Logger.info("Title1:%s Title2:%s. Match Point is incremented to:%d", service.title,title,matchPoint);
			}
			int minTitleLength=Math.min(title.length(), service.title.trim().length());
			int diff=calculateTextDifference(title, service.title.trim().toLowerCase());
			if(diff<=minTitleLength){
				matchPoint++;
				Logger.info("Title1:%s Title2:%s. Match Point is incremented to:%d", service.title,title,matchPoint);
			}
			
			String location=s.location.trim().toLowerCase();
			if(location.contains(service.location.trim().toLowerCase()) || service.location.trim().toLowerCase().contains(location)){
				matchPoint++;
				Logger.info("Location1:%s Location2:%s. Match Point is incremented to:%d", service.location,location,matchPoint);
			}
			int minLocationLength=Math.min(location.length(), service.location.trim().length());
			diff=calculateTextDifference(location, service.location.trim().toLowerCase());
			if(diff<=minLocationLength){
				matchPoint++;
				Logger.info("Location1:%s Location2:%s. Match Point is incremented to:%d", service.location,location,matchPoint);
			}
			
			String desc=s.description.trim().toLowerCase();
			if(desc.contains(service.description.trim().toLowerCase()) || service.description.trim().toLowerCase().contains(desc)){
				matchPoint++;
				Logger.info("Desc1:%s Desc2:%s. Match Point is incremented to:%d", service.description,desc,matchPoint);
			}
			int minDescLength=Math.min(desc.length(), service.description.trim().length());
			diff=calculateTextDifference(desc, service.description.trim().toLowerCase());
			if(diff<=minDescLength){
				matchPoint++;
				Logger.info("Desc1:%s Desc2:%s. Match Point is incremented to:%d", service.description,desc,matchPoint);
			}
			
			Date startDate=s.startDate;
			Date endDate=s.endDate;
			
			if(startDate!=null && endDate!=null && service.startDate!=null && service.endDate!=null){
				if((startDate.compareTo(service.startDate)<=0 && startDate.compareTo(service.endDate)>=0)
					|| (endDate.compareTo(service.startDate)>=0 && endDate.compareTo(service.endDate)<=0)	
				){
					matchPoint++;
					Logger.info("StartDate1:%s EndDate1:%s StartDate2:%s EndDate2:%s. Match Point is incremented to:%d", service.startDate, service.endDate,startDate,endDate,matchPoint);
				}
				
			}
			else if(endDate!=null && service.startDate!=null){
				if(endDate.compareTo(service.startDate)>=0){
					matchPoint++;
					Logger.info("StartDate1:%s EndDate2:%s. Match Point is incremented to:%d", service.startDate,endDate,matchPoint);
				}
			}
			else if(startDate!=null && service.endDate!=null){
				if(startDate.compareTo(service.endDate)<=0){
					matchPoint++;
					Logger.info("EndDate1:%s StartDate2:%s. Match Point is incremented to:%d", service.endDate,startDate,matchPoint);
				}
			}
			else{
				matchPoint++;
				Logger.info("No date limitation. Match point is incremented to %d",matchPoint);
			}
			
			ServiceMatch sm=new ServiceMatch();
			sm.setServiceOfuser(service);
			sm.setMatchService(s);
			sm.setMatchPoint(matchPoint);
			sm.setUser(service.boss);
			sm.save();
			
			Logger.info("Service1:%s Service2:%s matchPoint:%d saved",service.title,s.title,matchPoint);

			sm=new ServiceMatch();
			sm.setServiceOfuser(s);
			sm.setMatchService(service);
			sm.setMatchPoint(matchPoint);
			sm.setUser(s.boss);
			sm.save();
			
			Logger.info("Service1:%s Service2:%s matchPoint:%d saved",s.title,service.title,matchPoint);
		}
	}
	private static int calculateTextDifference(String s1,String s2){
		
		int array[][]=new int[s1.length()+1][s2.length()+1];
		
		for(int i=0;i<=s1.length();i++){
			array[i][0]=i;
		}
		
		for(int j=0;j<=s2.length();j++){
			array[0][j]=j;
		}
		
		for(int i=1;i<=s1.length();i++){
			for(int j=1;j<=s2.length();j++){
				String s11=s1.substring(0,s1.length()-1);
				int a=calculateTextDifference(s11,s2)+1;
				
				String s22=s2.substring(0,s2.length()-1);
				int b=calculateTextDifference(s1, s22)+1;
				
				int c=calculateTextDifference(s11, s22);
				if(s1.charAt(s1.length()-1)!=s2.charAt(s2.length()-1)){
					c++;
				}
				
				int d=Math.min(a, b);
				array[i][j]=Math.min(c,d);
			}
		}
		
		return array[s1.length()][s2.length()];
	}

}
