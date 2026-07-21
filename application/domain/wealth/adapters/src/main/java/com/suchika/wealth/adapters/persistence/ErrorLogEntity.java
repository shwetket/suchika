package com.suchika.wealth.adapters.persistence;

import com.suchika.sharedadapter.errorlog.AbstractErrorLogEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "error_log", schema = "wealth")
public class ErrorLogEntity extends AbstractErrorLogEntity {
}
