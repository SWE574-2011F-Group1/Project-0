package models;

public enum RankingType {
	NONE,
    DISTANCE,
    POINT,
	REGISTDATEFORNONE;
	
    public int getOrdinal() {
        return ordinal();
    }
}
