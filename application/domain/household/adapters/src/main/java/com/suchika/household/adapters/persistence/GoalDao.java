package com.suchika.household.adapters.persistence;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.UUID;

@ApplicationScoped
public class GoalDao implements PanacheRepositoryBase<GoalEntity, UUID> {
}
