package com.suchika.health.adapters.persistence;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.UUID;

@ApplicationScoped
public class ErrorLogDao implements PanacheRepositoryBase<ErrorLogEntity, UUID> {
}
