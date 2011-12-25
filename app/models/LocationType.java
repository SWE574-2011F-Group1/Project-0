package models;

public enum LocationType {
	VIRTUAL,
    ANYLOCATION,
    NORMAL,
    ALL;
    
    public int getOrdinal() {
        return ordinal();
    }
}
