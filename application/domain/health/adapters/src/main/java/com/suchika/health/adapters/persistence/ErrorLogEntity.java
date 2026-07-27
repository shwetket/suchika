package com.suchika.health.adapters.persistence;

import com.suchika.sharedadapter.errorlog.AbstractErrorLogEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "error_log", schema = "health")
public class ErrorLogEntity extends AbstractErrorLogEntity {
}
