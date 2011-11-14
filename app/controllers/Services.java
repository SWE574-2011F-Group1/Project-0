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
        if (params._contains("serviceId")) {
            service = Service.findById(Long.parseLong(params.get("serviceId")));
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
        if(!tags.trim().equals("")){
        	StringTokenizer st=new StringTokenizer(tags,",");
        	Set<STag> sTags=new HashSet<STag>();
        	while(st.hasMoreTokens()){
        		STag t=new STag(st.nextToken().trim());
        		t=t.save();
        		sTags.add(t);
        	}
        	service.stags=sTags;
        }
        
        Task task = Task.findById(taskId);
        service.task = task;
        
        
        service.em().clear();
        service.save();
        detail(service.id);
    }
    public static void list() {
        //TODO: Pagination...
        Collection<Service> services = null;
        if (params._contains("task") && null != params.get("task") && !params.get("task").equals("")) {
            Logger.info("hede ho o");
            services = Service.findByTask(Long.valueOf(params.get("task")));
        } else {
            services = Service.findAll();
        }
        Collection<Task> tasks = Task.findWithWeights();
        render(services, tasks);
    }

    public static void detail(long serviceId) {
        Service service = Service.findById(serviceId);
        boolean showEditBtn = Auth.connected().equals(service.boss.email);
        render(service, showEditBtn);
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
			System.out.println("title=" + title);
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

	private static String prepareQueryForQuickServiceSearch(String title) {
		String sql = "select s from Service s, Task t where s.task=t";
		sql += " and s.title LIKE '%" + title + "%'";
		return sql;
	}

	private static String prepareQueryForServiceSearch(ServiceSearchCriteria sc) {
		String sql = "select s from Service s, Task t where s.task=t";

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
		/*if (!sc.getTags().equals("")) {
			
			sql += " and st.text LIKE '" + sc.getTags()+;
		}*/
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