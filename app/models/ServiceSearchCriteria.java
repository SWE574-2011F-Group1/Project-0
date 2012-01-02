package models;

public class ServiceSearchCriteria {
	private long taskId;
	private String title;
	private String description;
	private String location;
	private String startDate;
	private String endDate;
	private int serviceType=-1;
	private int maxBasePoint;
	private String tags;
	
	public boolean searchSlots;
	public DayOfWeek dayOfWeek;
	public int hourStart;
	public int minStart;
	public int hourEnd;
	public int minEnd;
	public int startTimeMinutesAfterMidnight;
	public int endTimeMinutesAfterMidnight;
	public LocationType locationType=LocationType.ALL;
	public double locationLat=41;
	public double locationLng=32;
	
	public RankingType rankingType;
	public OrderType orderType;
	
	
	public void setStartTime(int hour, int min) {
		this.hourStart = hour;
		this.minStart = min;
		
		this.startTimeMinutesAfterMidnight = (this.hourStart * 60) + this.minStart;
	}
	
	public void setEndTime(int hour, int min) {
		this.hourEnd = hour;
		this.minEnd = min;
		
		this.endTimeMinutesAfterMidnight = (this.hourEnd * 60) + this.minEnd;
	}
	
	public long getTaskId() {
		return taskId;
	}
	public void setTaskId(long taskId) {
		this.taskId = taskId;
	}
	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public String getLocation() {
		return location;
	}
	public void setLocation(String location) {
		this.location = location;
	}
	public String getStartDate() {
		return startDate;
	}
	public void setStartDate(String startDate) {
		this.startDate = startDate;
	}
	public String getEndDate() {
		return endDate;
	}
	public void setEndDate(String endDate) {
		this.endDate = endDate;
	}
	public int getServiceType() {
		return serviceType;
	}
	public void setServiceType(int serviceType) {
		this.serviceType = serviceType;
	}
	public int getMaxBasePoint() {
		return maxBasePoint;
	}
	public void setMaxBasePoint(int maxBasePoint) {
		this.maxBasePoint = maxBasePoint;
	}
	public String getTags() {
		return tags;
	}
	public void setTags(String tags) {
		this.tags = tags;
	}
	
	
}
