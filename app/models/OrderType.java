package models;

public enum OrderType {
    ASC,
    DESC;
    
    public int getOrdinal() {
        return ordinal();
    }
}