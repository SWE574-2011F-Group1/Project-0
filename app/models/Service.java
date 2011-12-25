package models;

import play.*;
import play.db.jpa.*;

import javax.persistence.*;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import net.sf.oval.constraint.NotEmpty;

import java.text.SimpleDateFormat;
import java.util.*;

import play.data.validation.*;

@Entity
public class Service extends CommentableModel {

	public Service() {
		this.slots = new ArrayList<ServiceAvailabilitySlot>();
	}

	@Required
	@NotEmpty
	public String title;

	@Lob
	@Required
	@NotEmpty
	public String description;

	@Required
	@NotEmpty
	public String location;

	@Required
	@NotEmpty
	public double locationLat;

	@Required
	@NotEmpty
	public LocationType locationType;

	@Required
	@NotEmpty
	public double locationLng;

	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
	public Set<STag> stags;

	public Date startDate;

	public Date endDate;

	public Date actualDate;

	@Required
	@ManyToOne
	public Task task;

	@ManyToOne(cascade = CascadeType.ALL)
	public SUser boss;

	@ManyToOne(cascade = CascadeType.ALL)
	public SUser employee;

	@ManyToMany(cascade = CascadeType.ALL)
	public List<SUser> applicants;

	@OneToMany(cascade = CascadeType.ALL, mappedBy = "serviceOfuser")
	public List<ServiceMatch> matchesAsBoss;

	@OneToMany(cascade = CascadeType.ALL, mappedBy = "matchService")
	public List<ServiceMatch> matchesAsEmployee;

	/*
	 * @ManyToMany(cascade=CascadeType.ALL) public List<SUser> employees;
	 */

	@Required
	public ServiceStatus status;

	@Required
	public ServiceType type;

	@OneToMany
	public Set<Reward> rewards;

	@OneToMany(cascade = javax.persistence.CascadeType.ALL)
	public List<ServiceAvailabilitySlot> slots;

	public void addSlot(DayOfWeek dayOfWeek, int startTimeHour,
			int startTimeMinute, int endTimeHour, int endTimeMinute) {
		ServiceAvailabilitySlot slot = new ServiceAvailabilitySlot(this,
				dayOfWeek, startTimeHour, startTimeMinute, endTimeHour,
				endTimeMinute);

		this.slots.add(slot);
	}

	public String getFormattedStartDate() {
		return formatDate(this.startDate);
	}

	public String getFormattedEndDate() {
		return formatDate(this.endDate);
	}

	public String getFormattedActualDate() {
		return formatDate(this.actualDate);
	}

	private String formatDate(Date d) {
		SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy");
		return null != d ? sdf.format(d) : "";
	}

	public static List<Service> findByTask(long taskId) {
		return find("byTask.id", taskId).fetch();
	}

	public static List<Service> findByTag(String tag) {
		String sql = "select distinct s from Service s, STag st where st.service=s and st.text='"
				+ tag + "'";
		return find(sql, null).fetch();
	}

	public static List<Service> findByUserAndStatus(long userId, int type) {
		List<Service> services = new ArrayList<Service>();
		StringBuffer sql = new StringBuffer(
				"select s from Service s where s.boss.id = " + userId);
		if (type == 1) { // active sr
			sql.append("and s.status not in (?, ?) and s.type = ?");
			services = find(sql.toString(), ServiceStatus.DRAFT,
					ServiceStatus.FINISHED, ServiceType.REQUESTS).fetch();
		} else if (type == 2) { // active so
			sql.append("and s.status not in (?, ?) and s.type = ?");
			services = find(sql.toString(), ServiceStatus.DRAFT,
					ServiceStatus.FINISHED, ServiceType.PROVIDES).fetch();
		} else if (type == 3) { // planned sr/so
			sql.append("and s.status = (?)");
			services = find(sql.toString(), ServiceStatus.IN_PROGRESS).fetch();
		} else if (type == 4) { // done sr/so
			sql.append("and s.status = (?)");
			services = find(sql.toString(), ServiceStatus.FINISHED).fetch();
		} else {
			services = find(sql.toString()).fetch();
		}

		return services;
	}

	public static void findMatchServices(Service service, boolean isUpdate) {

		if (isUpdate) {
			String sql = "select sm from ServiceMatch sm where sm.serviceOfuser.id="
					+ service.id + " " + " or sm.matchService.id=" + service.id;
			List<ServiceMatch> serviceMatches = ServiceMatch.find(sql, null)
					.fetch();
			for (ServiceMatch sm : serviceMatches) {
				Logger.info("sm with id %d and wiht service %s is deleted",
						sm.id, sm.serviceOfuser.title);
				sm.delete();
			}
		}

		ServiceType type = service.type;
		int ordinal;
		if (type == ServiceType.PROVIDES) {
			ordinal = ServiceType.REQUESTS.getOrdinal();
		} else {
			ordinal = ServiceType.PROVIDES.getOrdinal();
		}

		String sql = "select s from Service s where s.type=" + ordinal + " "
				+ " and s.boss.id!=" + service.boss.id + " and s.status="
				+ ServiceStatus.PUBLISHED.ordinal() + " and s.task.id="
				+ service.task.id;
		List<Service> services = Service.find(sql, null).fetch();

		Logger.info(
				"Find match services size:%d services with ordinal %d found ",
				services.size(), ordinal);

		Map<Long, Double> distanceToOtherServices = new HashMap<Long, Double>();
		if (service.locationType == LocationType.NORMAL) {
			distanceToOtherServices = getDistances(service, services);
		}

		for (Service s : services) {
			int matchPoint = 2;
			String taskName = s.task.name;
			/*
			 * if(taskName.toLowerCase().equals(service.task.name.toLowerCase()))
			 * { matchPoint++; Logger.info(
			 * "TaskName1:%s TaskName2:%s. Match Point is incremented to:%d",
			 * service.task.name,taskName,matchPoint); }
			 */
			if (s.stags != null && service.stags != null && s.stags.size() > 0
					&& service.stags.size() > 0) {
				Set<STag> tags = s.stags;
				for (STag tag : tags) {
					String tText = tag.text.toLowerCase();
					for (STag t : service.stags) {
						String tText2 = t.text.toLowerCase();
						if (tText.equals(tText2) || tText.contains(tText2)
								|| tText2.contains(tText)) {
							matchPoint++;
							Logger.info(
									"Tag1:%s Tag2:%s. Match Point is incremented to:%d",
									tText2, tText, matchPoint);
						}
					}
				}
			}

			if (service.slots != null && s.slots != null
					&& service.slots.size() > 0 && s.slots.size() > 0) {
				for (ServiceAvailabilitySlot serviceSlot : service.slots) {
					for (ServiceAvailabilitySlot sSlot : s.slots) {
						if (serviceSlot.dayOfWeek == sSlot.dayOfWeek) {
							int serviceStartMinute = serviceSlot.startTimeMinutesAfterMidnight;
							int serviceEndMinute = serviceSlot.endTimeMinutesAfterMidnight;
							int sStartMinute = sSlot.startTimeMinutesAfterMidnight;
							int sEndMinute = sSlot.endTimeMinutesAfterMidnight;

							if (sStartMinute >= serviceStartMinute
									&& sEndMinute <= serviceEndMinute) {
								matchPoint += 3;
								Logger.info(
										"Slot exact match.Match Point is incremented to:%d",
										matchPoint);
							} else if (sStartMinute >= serviceStartMinute
									&& sEndMinute > serviceEndMinute) {
								matchPoint += 2;
								Logger.info(
										"Slot partial match.Match Point is incremented to:%d",
										matchPoint);
							} else if (sStartMinute < serviceStartMinute
									&& sEndMinute > serviceStartMinute) {
								matchPoint += 2;
								Logger.info(
										"Slot partial match.Match Point is incremented to:%d",
										matchPoint);
							}
						}
					}
				}
			}
			String title = s.title.trim().toLowerCase();
			if (title.contains(service.title.trim().toLowerCase())
					|| service.title.trim().toLowerCase().contains(title)) {
				matchPoint++;
				Logger.info(
						"Title1:%s Title2:%s. Match Point is incremented to:%d",
						service.title, title, matchPoint);
			}
			int minTitleLength = Math.min(title.length(), service.title.trim()
					.length());
			int diff = calculateTextDifference(title, service.title.trim()
					.toLowerCase());
			if (diff <= minTitleLength) {
				matchPoint++;
				Logger.info(
						"Title1:%s Title2:%s. Match Point is incremented to:%d",
						service.title, title, matchPoint);
			}

			String location = s.location.trim().toLowerCase();
			if (location.contains(service.location.trim().toLowerCase())
					|| service.location.trim().toLowerCase().contains(location)) {
				matchPoint++;
				Logger.info(
						"Location1:%s Location2:%s. Match Point is incremented to:%d",
						service.location, location, matchPoint);
			}
			int minLocationLength = Math.min(location.length(),
					service.location.trim().length());
			diff = calculateTextDifference(location, service.location.trim()
					.toLowerCase());
			if (diff <= minLocationLength) {
				matchPoint++;
				Logger.info(
						"Location1:%s Location2:%s. Match Point is incremented to:%d",
						service.location, location, matchPoint);
			}

			String desc = s.description.trim().toLowerCase();
			if (desc.contains(service.description.trim().toLowerCase())
					|| service.description.trim().toLowerCase().contains(desc)) {
				matchPoint++;
				Logger.info(
						"Desc1:%s Desc2:%s. Match Point is incremented to:%d",
						service.description, desc, matchPoint);
			}
			int minDescLength = Math.min(desc.length(), service.description
					.trim().length());
			diff = calculateTextDifference(desc, service.description.trim()
					.toLowerCase());
			if (diff <= minDescLength) {
				matchPoint++;
				Logger.info(
						"Desc1:%s Desc2:%s. Match Point is incremented to:%d",
						service.description, desc, matchPoint);
			}

			Date startDate = s.startDate;
			Date endDate = s.endDate;

			if (startDate != null && endDate != null
					&& service.startDate != null && service.endDate != null) {
				if ((startDate.compareTo(service.startDate) <= 0 && startDate
						.compareTo(service.endDate) >= 0)
						|| (endDate.compareTo(service.startDate) >= 0 && endDate
								.compareTo(service.endDate) <= 0)) {
					matchPoint++;
					Logger.info(
							"StartDate1:%s EndDate1:%s StartDate2:%s EndDate2:%s. Match Point is incremented to:%d",
							service.startDate, service.endDate, startDate,
							endDate, matchPoint);
				}

			} else if (endDate != null && service.startDate != null) {
				if (endDate.compareTo(service.startDate) >= 0) {
					matchPoint++;
					Logger.info(
							"StartDate1:%s EndDate2:%s. Match Point is incremented to:%d",
							service.startDate, endDate, matchPoint);
				}
			} else if (startDate != null && service.endDate != null) {
				if (startDate.compareTo(service.endDate) <= 0) {
					matchPoint++;
					Logger.info(
							"EndDate1:%s StartDate2:%s. Match Point is incremented to:%d",
							service.endDate, startDate, matchPoint);
				}
			} else {
				matchPoint++;
				Logger.info(
						"No date limitation. Match point is incremented to %d",
						matchPoint);
			}

			System.out.println(service.locationType);
			if (service.locationType == LocationType.NORMAL) {

				System.out.println("if");

				Double distance = distanceToOtherServices.get(new Long(s.id));
				if (distance != null) {
					Logger.info(
							"Distance is got from map for service:%s. Distance %s km",
							s.title, "" + distance.doubleValue());

					if ((distance.doubleValue() > 0 && distance < 50000)
							|| distance.doubleValue() == -2) {

						if (distance.doubleValue() < 1000) {
							matchPoint += 30;
						} else if (distance.doubleValue() < 5000) {
							matchPoint += 20;
						} else if (distance.doubleValue() < 10000) {
							matchPoint += 15;
						} else if (distance.doubleValue() < 30000) {
							matchPoint += 10;
						} else {
							matchPoint += 5;
						}

						Logger.info(
								"Matchpoint is incremented to %d because of distance %s km",
								matchPoint, "" + distance.doubleValue());

						ServiceMatch sm = new ServiceMatch();
						sm.setServiceOfuser(service);
						sm.setMatchService(s);
						sm.setMatchPoint(matchPoint);
						sm.setUser(service.boss);
						if (distance.doubleValue() == -2) {
							sm.distance = -2;// match service is not location
												// based
						} else {
							sm.distance = distance.doubleValue() / 1000;
						}
						sm.save();

						Logger.info(
								"Service1:%s Service2:%s matchPoint:%d distance:%s saved",
								service.title, s.title, matchPoint, ""
										+ sm.distance);

						sm = new ServiceMatch();
						sm.setServiceOfuser(s);
						sm.setMatchService(service);
						sm.setMatchPoint(matchPoint);
						sm.setUser(s.boss);
						if (distance.doubleValue() == -2) {
							sm.distance = -1;// service itself is not location
												// based
						} else {
							sm.distance = distance.doubleValue() / 1000;
						}
						sm.save();

						Logger.info(
								"Service1:%s Service2:%s matchPoint:%d distance:%s saved",
								s.title, service.title, matchPoint, ""
										+ sm.distance);
					}
				}

			} else {
				System.out.println("else");
				matchPoint += 30;

				Logger.info(
						"Location type is not location based. Matchpoint is incremented to %d because service is not location based",
						matchPoint);

				ServiceMatch sm = new ServiceMatch();
				sm.setServiceOfuser(service);
				sm.setMatchService(s);
				sm.setMatchPoint(matchPoint);
				sm.setUser(service.boss);
				sm.distance = -1;// service itself is not location based
				sm.save();

				Logger.info(
						"Service1:%s Service2:%s matchPoint:%d distance:%s saved",
						service.title, s.title, matchPoint, "" + sm.distance);

				sm = new ServiceMatch();
				sm.setServiceOfuser(s);
				sm.setMatchService(service);
				sm.setMatchPoint(matchPoint);
				sm.setUser(s.boss);
				if (s.locationType != LocationType.NORMAL) {
					sm.distance = -1;// service itself is not location based
				} else {
					sm.distance = -2;// match service is not location based
				}
				sm.save();

				Logger.info(
						"Service1:%s Service2:%s matchPoint:%d distance:%s saved",
						s.title, service.title, matchPoint, "" + sm.distance);
			}
		}
	}

	private static int calculateTextDifference(String s1, String s2) {

		int array[][] = new int[s1.length() + 1][s2.length() + 1];

		for (int i = 0; i <= s1.length(); i++) {
			array[i][0] = i;
		}

		for (int j = 0; j <= s2.length(); j++) {
			array[0][j] = j;
		}

		for (int i = 1; i <= s1.length(); i++) {
			for (int j = 1; j <= s2.length(); j++) {

				int a = array[i - 1][j] + 1;
				int b = array[i][j - 1] + 1;
				int c = array[i - 1][j - 1];
				if (s1.charAt(i - 1) != s2.charAt(j - 1)) {
					c++;
				}

				int d = Math.min(a, b);
				array[i][j] = Math.min(c, d);
			}
		}

		Logger.info("String1:%s String2:%s is compared. Difference is %d", s1,
				s2, array[s1.length()][s2.length()]);

		return array[s1.length()][s2.length()];
	}

	private static Map<Long, Double> getDistances(Service originService,
			List<Service> matchServices) {

		List<Service> locationBasedMatchServices = new ArrayList<Service>();
		Map<Long, Double> distanceMap = new HashMap<Long, Double>();

		for (Service s : matchServices) {
			if (s.locationType == LocationType.NORMAL) {
				locationBasedMatchServices.add(s);
			} else {
				distanceMap.put(new Long(s.id), new Double(-2));
			}
		}

		HttpClient client = new DefaultHttpClient();
		String origins = "" + originService.locationLat + ","
				+ originService.locationLng;
		String destinations = "";
		int sentServices = 0;
		boolean first = true;
		sentServices = 0;
		int index = 0;
		try {
			while (index < locationBasedMatchServices.size()) {
				while (index < locationBasedMatchServices.size()
						&& sentServices <= 30) {
					if (!first) {
						destinations += "%7C";
					}
					Service s = locationBasedMatchServices.get(index);
					destinations += "" + s.locationLat + "," + s.locationLng;

					first = false;
					sentServices++;
					index++;
				}
				String URL = "http://maps.googleapis.com/maps/api/distancematrix/json?sensor=false&";
				HttpGet get = new HttpGet(URL + "origins=" + origins
						+ "&destinations=" + destinations);
				get.setHeader("Accept", "text/xml");
				HttpResponse response = client.execute(get);

				Logger.info(
						"Index %d iken distance almak icin request yollandi",
						index);

				HttpEntity entity = response.getEntity();
				if (entity != null) {
					String resp = "";

					resp = EntityUtils.toString(entity);

					JSONObject jsonObject = new JSONObject(resp);
					JSONObject jsonObject2 = new JSONObject(jsonObject
							.getString("rows").substring(1,
									jsonObject.getString("rows").length() - 1));
					JSONArray array = new JSONArray(
							jsonObject2.getString("elements"));

					for (int i = 0; i < array.length(); i++) {
						JSONObject o = array.getJSONObject(i);

						int whichSet = index / 30;
						if (index != 1 && (index - 1) % 30 == 0) {
							whichSet--;
						}
						int whichIndex = whichSet * 30 + i;
						Service whichService = locationBasedMatchServices
								.get(whichIndex);

						if (o.getString("status").equals("OK")) {
							JSONObject jo = new JSONObject(
									o.getString("distance"));

							distanceMap.put(new Long(whichService.id),
									new Double(jo.getDouble("value")));
							Logger.info("Service1:%s Service2:%s Distance:%s",
									originService.title, whichService.title,
									jo.getString("value"));
						} else {
							Logger.info(
									"Service1:%s Service2:%s Status Ok degil. Distance alınamadı",
									originService.title, whichService.title);
						}
					}
				}
				sentServices = 0;
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			Logger.info("Uzaklıklar alınırken hata olustu");

		}

		return distanceMap;
	}
}
