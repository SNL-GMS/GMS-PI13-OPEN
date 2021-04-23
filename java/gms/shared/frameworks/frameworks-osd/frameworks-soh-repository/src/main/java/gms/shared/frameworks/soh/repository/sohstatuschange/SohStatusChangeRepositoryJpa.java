package gms.shared.frameworks.soh.repository.sohstatuschange;

import gms.shared.frameworks.osd.api.statuschange.SohStatusChangeRepositoryInterface;
import gms.shared.frameworks.osd.coi.soh.quieting.QuietedSohStatusChange;
import gms.shared.frameworks.osd.coi.soh.quieting.UnacknowledgedSohStatusChange;
import gms.shared.frameworks.osd.dao.soh.statuschange.SohStatusChangeQuietedDao;
import gms.shared.frameworks.soh.statuschange.converter.SohStatusChangeQuietedDaoConverter;
import gms.shared.frameworks.osd.dao.soh.statuschange.SohStatusChangeDao;
import gms.shared.frameworks.osd.dao.soh.statuschange.SohStatusChangeEventDao;
import gms.shared.frameworks.soh.statuschange.converter.SohStatusChangeDaoConverter;
import gms.shared.frameworks.soh.statuschange.converter.SohStatusChangeEventDaoConverter;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.commons.lang3.Validate;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;


public class SohStatusChangeRepositoryJpa implements SohStatusChangeRepositoryInterface {
    static final Logger logger = LoggerFactory.getLogger(SohStatusChangeRepositoryJpa.class);

    private EntityManagerFactory entityManagerFactory;

    public SohStatusChangeRepositoryJpa(EntityManagerFactory emf) {
        this.entityManagerFactory = emf;
    }

    @Override
    public List<UnacknowledgedSohStatusChange> retrieveUnacknowledgedSohStatusChanges(
        Collection<String> stationNames){
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        try {
            CriteriaBuilder cb = entityManager.getCriteriaBuilder();
            CriteriaQuery<SohStatusChangeEventDao> unackQuery = cb.createQuery(SohStatusChangeEventDao.class);
            Root<SohStatusChangeEventDao> fromUnackSoh = unackQuery.from(SohStatusChangeEventDao.class);
            unackQuery.select(fromUnackSoh);

            // only add the where clause if a specific set of stations was requested.
            if (!stationNames.isEmpty()) {
                unackQuery.where(fromUnackSoh.get("station").get("name").in(stationNames));
            }

            List<SohStatusChangeEventDao> unackDaos = entityManager.createQuery(unackQuery).getResultList();

                List<UnacknowledgedSohStatusChange> result = new ArrayList<>();
                for(SohStatusChangeEventDao unackDao : unackDaos) {
                    result.add(new SohStatusChangeEventDaoConverter().toCoi(unackDao));
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
    public void storeUnacknowledgedSohStatusChange(Collection<UnacknowledgedSohStatusChange> unackStatusChanges) {
        Validate.notNull(unackStatusChanges);
        Validate.notEmpty(unackStatusChanges);

        EntityManager entityManager = this.entityManagerFactory.createEntityManager();

        entityManager.getTransaction().begin();

        try {

            for (UnacknowledgedSohStatusChange unackStatusChange : unackStatusChanges) {

                TypedQuery<SohStatusChangeEventDao> query = entityManager.createNamedQuery("SohStatusChangeEventDao.checkExistsByStationName",
                    SohStatusChangeEventDao.class);
                query.setParameter("stationName", unackStatusChange.getStation());

                try{
                    SohStatusChangeEventDao foundUnackStatusChangeDao = query.getSingleResult();
                    //if we pass a unack status change with an empty list of changes, we remove the
                    //unacknowledged status change from the osd
                    if(unackStatusChange.getSohStatusChanges().isEmpty()){
                        entityManager.remove(foundUnackStatusChangeDao);
                        entityManager.flush();
                        continue;
                    }

                    Set<SohStatusChangeDao> sohStatusChangeDaos = unackStatusChange.getSohStatusChanges()
                        .stream()
                        .map(sohStatusChange -> new SohStatusChangeDaoConverter().fromCoi(sohStatusChange, entityManager))
                        .collect(Collectors.toSet());

                    foundUnackStatusChangeDao.setSohStatusChangeDaos(sohStatusChangeDaos);
                } catch (NoResultException e){
                    SohStatusChangeEventDaoConverter converter =new SohStatusChangeEventDaoConverter();
                    SohStatusChangeEventDao dao = converter.fromCoi(unackStatusChange, entityManager);
                    entityManager.persist(dao);
                }
            }
            entityManager.getTransaction().commit();
        } catch (Exception e) {
            logger.error("Error committing UnacknowledgedSohStatusChange", e);
            if (entityManager.getTransaction().isActive()) {
                entityManager.getTransaction().rollback();
            }
            throw e;
        } finally {
            entityManager.close();
        }

    }

    /**
     * This storage function looks to see if the quieted soh status change to be stored has its
     *     monitor type and channel pair already in the osd. If it does, it gets that SohStatusChangeQuitedDao
     *     entity and modifies that. The idea is to only have the most recent quieted monitor type and channel
     *     pair in the osd
     * @param quietedSohStatusChanges
     */
    @Override
    public void storeQuietedSohStatusChangeList(Collection<QuietedSohStatusChange> quietedSohStatusChanges) {
        Validate.notNull(quietedSohStatusChanges);
        Validate.notEmpty(quietedSohStatusChanges);

        EntityManager entityManager = this.entityManagerFactory.createEntityManager();

        //bypassing batching as it's not needed here
        entityManager.unwrap(Session.class).setJdbcBatchSize(1);

        try {
            for (QuietedSohStatusChange quietedSohStatusChange : quietedSohStatusChanges) {
                try {
                    persistOrMergeSohStatusChange(entityManager, quietedSohStatusChange);
                } catch (Exception e) {
                    logger.error("Error committing soh status change", e);
                    if (entityManager.getTransaction().isActive()) {
                        entityManager.getTransaction().rollback();
                    }
                }
            }
        }finally {
            entityManager.close();
        }
    }

    /* gets all quited soh status changes that have a quiet until time that is after the instant
    passed to the argument
     */
    @Override
    public Collection<QuietedSohStatusChange> retrieveQuietedSohStatusChangesByTime(Instant afterTime){
        Validate.notNull(afterTime);

        EntityManager entityManager = this.entityManagerFactory.createEntityManager();
        try {

            CriteriaBuilder builder = entityManager.getCriteriaBuilder();
            CriteriaQuery<SohStatusChangeQuietedDao> query = builder
                .createQuery(SohStatusChangeQuietedDao.class);
            Root<SohStatusChangeQuietedDao> fromQuieted = query
                .from(SohStatusChangeQuietedDao.class);
            query.select(fromQuieted);
            query.where(builder.greaterThanOrEqualTo(fromQuieted.get("quietUntil"), afterTime));

            List<SohStatusChangeQuietedDao> quietedSohStatusChangeList = entityManager
                .createQuery(query)
                .getResultList();

            return quietedSohStatusChangeList.stream()
                .map(dao -> new SohStatusChangeQuietedDaoConverter().toCoi(dao))
                .collect(Collectors.toList());
        } catch (Exception e) {
            logger.error(e.getMessage());
        } finally {
            entityManager.close();
        }
            return List.of();
    }
    /**
     * this method will determine whether to save a new item or update an existing one
     * it is synchronized as it is possible we try to save the same item twice (from different threads)
     * and if the commit has not been called after the first persist, we will get a duplicate key exception
     * since the requirements are not high performant method, this is an acceptable tradefoff to ease of code
     *
     * @param entityManager
     * @param quietedSohStatusChange
     */
    private synchronized void persistOrMergeSohStatusChange(EntityManager entityManager,
        QuietedSohStatusChange quietedSohStatusChange){
        entityManager.getTransaction().begin();
        TypedQuery<SohStatusChangeQuietedDao> query = entityManager.createNamedQuery(
            "SohStatusChangeQuietedDao.checkExistsByChannelNameAndMonitorType",
            SohStatusChangeQuietedDao.class);
        query.setParameter("channelName", quietedSohStatusChange.getChannelName());
        query.setParameter("sohMonitorType", quietedSohStatusChange.getSohMonitorType());

        try {
            SohStatusChangeQuietedDao sohStatusChangeQuietedDao = query.getSingleResult();
            sohStatusChangeQuietedDao.setQuietUntil(quietedSohStatusChange.getQuietUntil());
            sohStatusChangeQuietedDao.setComment(quietedSohStatusChange.getComment().orElse(null));
            sohStatusChangeQuietedDao.setQuietDuration(quietedSohStatusChange.getQuietDuration());
        } catch (NoResultException e){
            SohStatusChangeQuietedDaoConverter sohStatusChangeQuietedDaoConverter = new SohStatusChangeQuietedDaoConverter();
            SohStatusChangeQuietedDao sohStatusChangeQuietedDao = sohStatusChangeQuietedDaoConverter.fromCoi(quietedSohStatusChange, entityManager);
            entityManager.persist(sohStatusChangeQuietedDao);
        }
        entityManager.getTransaction().commit();
    }
}
