package models;

public enum RankingType {
	NONE,
    DISTANCE,
    POINT;
    
    public int getOrdinal() {
        return ordinal();
    }
}
