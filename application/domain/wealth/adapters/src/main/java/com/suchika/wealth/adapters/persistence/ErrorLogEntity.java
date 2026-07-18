package com.suchika.wealth.adapters.persistence;

import com.suchika.sharedadapter.errorlog.AbstractErrorLogEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "error_log", schema = "wealth")
public class ErrorLogEntity extends AbstractErrorLogEntity {

    public static ErrorLogEntity from(String errorCode, int httpStatus, String message, String details) {
        ErrorLogEntity e = new ErrorLogEntity();
        e.populate(errorCode, httpStatus, message, details);
        return e;
    }
}
