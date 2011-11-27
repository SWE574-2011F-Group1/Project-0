package controllers;

import play.*;
import play.mvc.*;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import models.*;

@With(Secure.class)
@Check("user")
public class Services extends BaseController {

    public static void index() {
        Service service = new Service();
        //Set something to type to prevent null pointer exception...
        service.type = ServiceType.REQUESTS;
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
    		String location, String startDate, String endDate,String tags) {
        Service service;
        Set<STag> deletedTags=null;
        if (params._contains("serviceId")) {
            service = Service.findById(Long.parseLong(params.get("serviceId")));
            deletedTags=service.stags;
        } else {
            service = new Service();
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
        SUser u = SUser.findByEmail(Secure.Security.connected());
        service.boss = u;
        service.stags = new HashSet<STag>();
        
        if (!tags.trim().equals("")) {
        	StringTokenizer st = new StringTokenizer(tags,",");
        	Set<STag> sTags = new HashSet<STag>();
        	while (st.hasMoreTokens()){
        		STag t = new STag(st.nextToken().trim());
        		sTags.add(t);
        		t.service=service;
        	}
        	service.stags = sTags;
        }
        
        Task task = Task.findById(taskId);
        service.task = task;

        service.status=ServiceStatus.PUBLISHED;
        service.save();
        
        if(deletedTags!=null){
        	for (STag tag: deletedTags) {
				tag.delete();
			}
        }
        
        detail(service.id);
    }


    public static void list(long uid, int st) {
        //TODO: Pagination...
        Collection<Service> services = null;
        if (params._contains("task") && null != params.get("task") && !params.get("task").equals("")) {
            Logger.info("hede ho o");
            services = Service.findByTask(Long.valueOf(params.get("task")));
        }  else if (params._contains("uid")) {
        	services = Service.findByUserAndStatus(uid, st);
        	if (services.isEmpty()) {
        		
        	}
        }else {
        	services = Service.findAll();
        }
        Collection<Task> tasks = Task.findWithWeights();
        render(services, tasks);
    }

    public static void detail(long serviceId) {
        Service service = Service.findById(serviceId);
        boolean isBossUser = Auth.connected().equals(service.boss.email);
        String userEmail=Auth.connected();
        boolean isAppliedBefore=false;
        SUser currentUser=SUser.findByEmail(Auth.connected());
        if(!isBossUser){
        	isAppliedBefore=isApplied(service.applicants,SUser.findByEmail(userEmail));
        }
        render(service, isBossUser,userEmail,isAppliedBefore,currentUser);
    }
    
    public static void search(int searchDone,String title, int serviceType, 
			String description, long taskId, String location, 
			String startDate, String endDate, int maxBasePoint,String tags) {
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
		Collection<Task> tasks = Task.findAllActive();
		render(services, tasks);
	}

	public static void apply(long serviceId,String email) throws Exception {
	        Service service = Service.findById(serviceId);
	        SUser user=SUser.findByEmail(email);
	        List<SUser> applicants=service.applicants;
	        boolean isBossUser = service.boss.email.equals(email);
	        if(service.status==ServiceStatus.PUBLISHED && !isBossUser && !isApplied(applicants, user)){
		        if(applicants==null){
		        	applicants=new ArrayList<SUser>();
		        }
		        applicants.add(user);
		        service.save();
	        }
	        else{
	        	//APPLIED BEFORE
	        }
		        
	        
	        detail(service.id);
	}
	public static void cancelApply(long serviceId,String email) throws Exception {
        Service service = Service.findById(serviceId);
        SUser user=SUser.findByEmail(email);
        List<SUser> applicants=service.applicants;
        int index=findUserIndex(applicants, user);
        if(index!=-1){
	       applicants.remove(index);
	        service.save();
        }
        else{
        	//NOT APPLIED BEFORE
        	
        }
	                
        detail(service.id);
	}
	public static void startApproval(long serviceId,String email) throws Exception {
		
        Service service = Service.findById(serviceId);
        SUser user=SUser.findByEmail(email);
        List<SUser> applicants=service.applicants;
        boolean isBossUser = Auth.connected().equals(service.boss.email);
        int index=findUserIndex(applicants, user);
        if (isBossUser && index!=-1) {
            render(service,user);
        } else {
        	//NOT APPLIED BEFORE	
        }
	}
	
	public static void processStartApproval(long serviceId,long applicantId, 
											String actualDate,String location) throws Exception {	
        Service service = Service.findById(serviceId);
        SUser user=SUser.findById(applicantId);
        List<SUser> applicants=service.applicants;
        boolean isBossUser = Auth.connected().equals(service.boss.email);
        int index=findUserIndex(applicants, user);
        if (isBossUser && index!=-1 && service.status==ServiceStatus.PUBLISHED) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy");
            try {
            	service.actualDate= sdf.parse(actualDate);
            } catch (ParseException e) {
                //FIXME: Find out what to do if this occurs...
            }
            service.location=location;
            service.status=ServiceStatus.WAITING_EMPLOYEE_APPROVAL;
            service.employee=user;
            service.save();
            
            detail(serviceId);
        } else {
        	//NOT APPLIED BEFORE
        }
	      
	}
	
	public static void employeeApproval(long serviceId,long applicantId){
		Service service = Service.findById(serviceId);
        SUser user=SUser.findById(applicantId);
		render(service,user);
	}
	public static void processEmployeeApproval(long serviceId,long applicantId,int approval){
		Service service = Service.findById(serviceId);
        SUser user=SUser.findById(applicantId);
        List<SUser> applicants=service.applicants;
        //boolean isBossUser = Auth.connected().equals(service.boss.email);
        int index = findUserIndex(applicants, user);
        if(approval==1 && index!=-1 && service.status==ServiceStatus.WAITING_EMPLOYEE_APPROVAL){
        	service.status=ServiceStatus.IN_PROGRESS;
        	service.applicants=null;
			service.save();
			
			List<Service> servicesAsEmployee=user.servicesAsEmployee;
			if(servicesAsEmployee!=null){
				servicesAsEmployee=new ArrayList<Service>();
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
}
