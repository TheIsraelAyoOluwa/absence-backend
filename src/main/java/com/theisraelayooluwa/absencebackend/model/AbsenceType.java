package com.theisraelayooluwa.absencebackend.model;

public enum AbsenceType {
    HOLIDAY(true),
    OTHER_LEAVE(false),
    SCHEDULED_TRAINING(false),
    SICKNESS(false),
    UNEXPLAINED_ABSENCE(false);


    private final Boolean planned;

    AbsenceType(boolean b) {
        this.planned = b;
    }

    public Boolean isPlanned() {
        return planned;
    }
}
