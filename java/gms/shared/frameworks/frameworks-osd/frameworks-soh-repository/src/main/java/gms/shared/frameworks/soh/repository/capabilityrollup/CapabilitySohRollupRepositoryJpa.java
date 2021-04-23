package gms.shared.frameworks.soh.repository.capabilityrollup;

import com.google.common.base.Preconditions;
import gms.shared.frameworks.osd.api.capabilityrollup.CapabilitySohRollupRepositoryInterface;
import gms.shared.frameworks.osd.coi.soh.CapabilitySohRollup;
import gms.shared.frameworks.osd.dao.channel.StationDao;
import gms.shared.frameworks.osd.dao.channel.StationGroupDao;
import gms.shared.frameworks.osd.dao.soh.CapabilitySohRollupDao;
import gms.shared.frameworks.osd.dao.soh.StationSohDao;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Subquery;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CapabilitySohRollupRepositoryJpa implements CapabilitySohRollupRepositoryInterface {

  private static final Logger logger =
      LoggerFactory.getLogger(CapabilitySohRollupRepositoryJpa.class);

  private static final String STATION_GROUP_ATTRIBUTE = "stationGroupDao";
  private static final String NAME_ATTRIBUTE = "name";
  private static final String CAPABILITY_ROLLUP_TIME_ATTRIBUTE = "capabilityRollupTime";

  private EntityManagerFactory entityManagerFactory;

  public CapabilitySohRollupRepositoryJpa(EntityManagerFactory emf) {
    this.entityManagerFactory = emf;
  }

  @Override
  public List<CapabilitySohRollup> retrieveCapabilitySohRollupByStationGroup(
      Collection<String> stationGroups) {

    EntityManager entityManager = entityManagerFactory.createEntityManager();

    try {
      CriteriaBuilder cb = entityManager.getCriteriaBuilder();
      CriteriaQuery<CapabilitySohRollupDao> capabilityQuery =
          cb.createQuery(CapabilitySohRollupDao.class);
      Root<CapabilitySohRollupDao> fromCapability =
          capabilityQuery.from(CapabilitySohRollupDao.class);

      if (!stationGroups.isEmpty()) {
        capabilityQuery.where(fromCapability.get("stationGroupDao").get("name").in(stationGroups));
      }

      List<CapabilitySohRollupDao> capabilitySohRollupDaos =
          entityManager.createQuery(capabilityQuery).getResultList();

      List<CapabilitySohRollup> result;
      result = capabilitySohRollupDaos
          .stream()
          .map(capabilityDao -> new CapabilitySohRollupConverter().toCoi(capabilityDao))
          .collect(Collectors.toList());
      return result;
    } catch (Exception e) {
      logger.error((e.getMessage()));
    } finally {
      entityManager.close();
    }
    return List.of();
  }

  @Override
  public void storeCapabilitySohRollup(Collection<CapabilitySohRollup> capabilitySohRollups) {

    Validate.notNull(capabilitySohRollups);
    EntityManager entityManager = this.entityManagerFactory.createEntityManager();

    Collection<CapabilitySohRollup> validCapabilitySohRollups =
        getListOfValidEntries(capabilitySohRollups, entityManager);

    entityManager.getTransaction().begin();

    try {

      for (CapabilitySohRollup capabilitySohRollup : validCapabilitySohRollups) {

        CapabilitySohRollupDao dao =
            new CapabilitySohRollupConverter().fromCoi(capabilitySohRollup, entityManager);

        entityManager.persist(dao);

      }
      entityManager.getTransaction().commit();

    } catch (Exception e) {
      logger.error("Error committing configuration", e);
      if (entityManager.getTransaction().isActive()) {
        entityManager.getTransaction().rollback();
      }
      throw e;
    } finally {
      entityManager.close();
    }

  }

  @Override
  public List<CapabilitySohRollup> retrieveLatestCapabilitySohRollupByStationGroup(Collection<String> stationGroupNames) {
    Objects.requireNonNull(stationGroupNames);
    Preconditions.checkState(!stationGroupNames.isEmpty());
    EntityManager entityManager = entityManagerFactory.createEntityManager();

    try {
      CriteriaBuilder builder = entityManager.getCriteriaBuilder();
      CriteriaQuery<CapabilitySohRollupDao> capabilityRollupQuery =
          builder.createQuery(CapabilitySohRollupDao.class);
      Root<CapabilitySohRollupDao> fromCapabilityRollup =
          capabilityRollupQuery.from(CapabilitySohRollupDao.class);
      capabilityRollupQuery.select(fromCapabilityRollup);

      Join<CapabilitySohRollupDao, StationGroupDao> stationGroupJoin =
          fromCapabilityRollup.join(STATION_GROUP_ATTRIBUTE);
      Expression<String> stationGroupName = stationGroupJoin.get(NAME_ATTRIBUTE);

      Expression<Instant> time = fromCapabilityRollup.get(CAPABILITY_ROLLUP_TIME_ATTRIBUTE);

      capabilityRollupQuery.where(builder.or(stationGroupNames.stream()
          .map(name -> {
            Subquery<Instant> maxTimeQuery = capabilityRollupQuery.subquery(Instant.class);
            Root<CapabilitySohRollupDao> subFromCapabilityRollup =
                maxTimeQuery.from(CapabilitySohRollupDao.class);
            Expression<Instant>  subTime = subFromCapabilityRollup.get(CAPABILITY_ROLLUP_TIME_ATTRIBUTE);
            Join<CapabilitySohRollupDao, StationGroupDao> subStationGroupJoin =
                subFromCapabilityRollup.join(STATION_GROUP_ATTRIBUTE);
            Expression<String> subStationGroupName = subStationGroupJoin.get(NAME_ATTRIBUTE);
            maxTimeQuery.select(builder.greatest(subTime));
            return builder.and(builder.equal(stationGroupName, name),
                builder.equal(time, maxTimeQuery.where(builder.equal(subStationGroupName, name))));
          }).toArray(Predicate[]::new)));

      CapabilitySohRollupConverter converter = new CapabilitySohRollupConverter();
      return entityManager.createQuery(capabilityRollupQuery)
          .getResultStream()
          .map(converter::toCoi)
          .collect(Collectors.toList());
    } finally {
      entityManager.close();
    }
  }

  private Collection<CapabilitySohRollup> getListOfValidEntries(Collection<CapabilitySohRollup> capabilitySohRollups, EntityManager entityManager) {

    Set<String> allStationGroups = new HashSet<>();
    Set<String> allStations = new HashSet<>();
    Collection<CapabilitySohRollup> validCapabilitySohRollups = new LinkedList<>();

    for (CapabilitySohRollup current : capabilitySohRollups) {
      allStationGroups.add(current.getForStationGroup());
      allStations.addAll(current.getRollupSohStatusByStation().keySet());
    }

    Validate.notEmpty(allStationGroups);
    Validate.notEmpty(allStations);

    CriteriaBuilder cb = entityManager.getCriteriaBuilder();

    CriteriaQuery<String> stationGroupQuery =
        cb.createQuery(String.class);
    Root<StationGroupDao> fromStationGroup = stationGroupQuery.from(StationGroupDao.class);
    Path<String> namePath = fromStationGroup.get("name");
    stationGroupQuery.select(namePath);
    stationGroupQuery.where(fromStationGroup.get("name").in(allStationGroups));

    List<String> storedStationGroups =
        entityManager.createQuery(stationGroupQuery).getResultList();
    allStationGroups.removeAll(storedStationGroups);

    CriteriaQuery<String> stationQuery =
        cb.createQuery(String.class);
    Root<StationDao> fromStation = stationQuery.from(StationDao.class);
    Path<String> stationNamePath = fromStation.get("name");
    stationQuery.select(stationNamePath);
    stationQuery.where(fromStation.get("name").in(allStations));

    List<String> storedStations =
        entityManager.createQuery(stationQuery).getResultList();
    allStations.removeAll(storedStations);

    if (allStationGroups.isEmpty() && allStations.isEmpty()) {
      return capabilitySohRollups;
    }

    for (CapabilitySohRollup current : capabilitySohRollups) {
      if (isValidEntity(current, allStationGroups, allStations)) {
        validCapabilitySohRollups.add(current);
      }

    }

    return validCapabilitySohRollups;

  }

  private boolean isValidEntity(CapabilitySohRollup capabilitySohRollup,
      Set<String> allStationGroups, Set<String> allStations) {

    boolean isValid = true;

    if (allStationGroups.contains(capabilitySohRollup.getForStationGroup())) {
      logger.info("Foreign key {} for station group in capability rollup with UUID {} does not " +
              "exist in OSD, "
              + "not storing this capability rollup", capabilitySohRollup.getForStationGroup(),
          capabilitySohRollup.getId());
      isValid = false;
    }

    Set<String> stations = capabilitySohRollup.getRollupSohStatusByStation().keySet();
    stations.retainAll(allStations);

    if (!stations.isEmpty()) {
      logger.info("Foreign key(s) {} for stations in capability rollup with UUID {} do not exist " +
          "in OSD, "
          + "not storing this capability rollup", stations, capabilitySohRollup.getId());
      isValid = false;
    }

    return isValid;
  }

}
