package gms.shared.frameworks.osd.control.station;

import gms.shared.frameworks.osd.api.station.StationRepositoryInterface;
import gms.shared.frameworks.osd.coi.signaldetection.Station;
import gms.shared.frameworks.osd.coi.signaldetection.repository.jpa.dataaccessobjects.StationDao;
import gms.shared.frameworks.osd.control.utils.StationUtils;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StationRepositoryJpa implements StationRepositoryInterface {

  private EntityManagerFactory entityManagerFactory;

  private static Logger logger = LoggerFactory.getLogger(StationRepositoryJpa.class);

  public StationRepositoryJpa(EntityManagerFactory entityManagerFactory) {
    this.entityManagerFactory = entityManagerFactory;
  }

  @Override
  public List<Station> retrieveAllStations(Collection<String> stationNames) {
    EntityManager entityManager = entityManagerFactory.createEntityManager();
    StationUtils stationUtils = new StationUtils(entityManager);

    CriteriaBuilder cb = entityManager.getCriteriaBuilder();
    CriteriaQuery<StationDao> stationQuery = cb.createQuery(StationDao.class);
    Root<StationDao> fromStation = stationQuery.from(StationDao.class);

    stationQuery.select(fromStation);

    // only add the where clause if a specific set of stations was requested.
    if (!stationNames.isEmpty()) {
      stationQuery.where(cb.or(stationNames.stream()
          .map(stationName -> cb.equal(fromStation.get("name"), stationName))
          .toArray(Predicate[]::new)));
    }

    List<StationDao> stationDaos = entityManager.createQuery(stationQuery).getResultList();

    try {
      List<Station> result = new ArrayList<>();
      for(StationDao stationDao : stationDaos) {
        result.add(stationUtils.generateStation(stationDao, true));
      }
      return result;
    } catch (Exception e) {
      logger.error(e.getMessage());
    } finally {
      entityManager.close();
    }

    return List.of();
  }

  @Override
  public void storeStations(Collection<Station> stations) {
    Validate.notNull(stations);
    EntityManager entityManager = entityManagerFactory.createEntityManager();
    StationUtils stationUtils = new StationUtils(entityManager);

    entityManager.getTransaction().begin();
    try {
      for (Station station : stations) {
        stationUtils.storeStation(station);
      }
      entityManager.getTransaction().commit();
    } catch (Exception e) {
      logger.error(e.getMessage());
      entityManager.getTransaction().rollback();
      throw e;
    } finally {
      entityManager.close();
    }
  }

}
