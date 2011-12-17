package models;

public enum LocationType {
	VIRTUAL,
    ANYLOCATION,
    NORMAL;
    
    public int getOrdinal() {
        return ordinal();
    }
}
