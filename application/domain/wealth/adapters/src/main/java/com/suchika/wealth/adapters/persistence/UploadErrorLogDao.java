package com.suchika.wealth.adapters.persistence;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.UUID;

@ApplicationScoped
public class UploadErrorLogDao implements PanacheRepositoryBase<UploadErrorLogEntity, UUID> {
}
