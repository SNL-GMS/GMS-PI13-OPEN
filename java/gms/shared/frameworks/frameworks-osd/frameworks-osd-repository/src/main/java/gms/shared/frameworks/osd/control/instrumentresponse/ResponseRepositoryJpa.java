package gms.shared.frameworks.osd.control.instrumentresponse;

import gms.shared.frameworks.coi.exceptions.DataExistsException;
import gms.shared.frameworks.osd.api.instrumentresponse.ResponseRepositoryInterface;
import gms.shared.frameworks.osd.coi.channel.repository.jpa.dataaccessobjects.ChannelDao;
import gms.shared.frameworks.osd.coi.signaldetection.Response;
import gms.shared.frameworks.osd.coi.signaldetection.repository.jpa.dataaccessobjects.ResponseDao;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import org.apache.commons.lang3.Validate;

/**
 * Implementation of {@link ResponseRepositoryInterface} using JPA
 */
public class ResponseRepositoryJpa implements ResponseRepositoryInterface {

  private final EntityManagerFactory emf;

  /**
   * Constructor
   *
   * @param emf entity manager factory, not null
   */
  public ResponseRepositoryJpa(EntityManagerFactory emf) {
    this.emf = Objects.requireNonNull(emf);
  }

  @Override
  public Map<String, Response> retrieveResponsesByChannels(Set<String> channelNames) {
    Validate.notEmpty(channelNames);
    final EntityManager entityManager = emf.createEntityManager();
    try {
      return entityManager.createQuery(queryByChannelNames(entityManager, channelNames))
          .getResultStream().map(ResponseDao::toCoi)
          .collect(Collectors.toMap(Response::getChannelName, Function.identity()));
    } finally {
      entityManager.close();
    }
  }

  @Override
  public void storeResponses(Collection<Response> responses) {
    final EntityManager entityManager = emf.createEntityManager();
    try {
      final Set<String> responseChannelNames = channelNames(responses);
      validateNoneInStorage(responseChannelNames);
      final Map<String, ChannelDao> channelDaoByName = findChannelsByName(
          entityManager, responseChannelNames);
      entityManager.getTransaction().begin();
      responses.forEach(r -> entityManager.persist(
          new ResponseDao(r, channelDaoByName.get(r.getChannelName()))));
      entityManager.getTransaction().commit();
    } catch (Exception ex) {
      entityManager.getTransaction().rollback();
      throw ex;
    } finally {
      entityManager.close();
    }
  }

  private static CriteriaQuery<ResponseDao> queryByChannelNames(
      EntityManager em, Set<String> channelNames) {
    final CriteriaQuery<ResponseDao> query = em.getCriteriaBuilder()
        .createQuery(ResponseDao.class);
    final Root<ResponseDao> from = query.from(ResponseDao.class);
    return query.select(from).where(from.join("channel").get("name").in(channelNames));
  }

  private void validateNoneInStorage(Set<String> responseChannelNames) {
    final Map<String, Response> existing = retrieveResponsesByChannels(responseChannelNames);
    if (!existing.isEmpty()) {
      throw new DataExistsException(
          "Responses already exist for these channels: " + existing.keySet());
    }
  }

  private static Set<String> channelNames(Collection<Response> responses) {
    final List<String> chans = responses.stream()
        .map(Response::getChannelName)
        .collect(Collectors.toList());
    final Set<String> uniqueChans = new HashSet<>();
    final Set<String> duplicates = new HashSet<>();
    for (String c : chans) {
      if (!uniqueChans.add(c)) {
        duplicates.add(c);
      }
    }
    Validate.isTrue(duplicates.isEmpty(),
        "Responses have duplicate channels: " + duplicates);
    return uniqueChans;
  }

  private static Map<String, ChannelDao> findChannelsByName(EntityManager em, Set<String> names) {
    final Map<String, ChannelDao> m = new HashMap<>(names.size());
    final Set<String> unknownChannels = new HashSet<>();
    for (String name : names) {
      final ChannelDao dao = em.find(ChannelDao.class, name);
      if (dao == null) {
        unknownChannels.add(name);
      } else {
        m.put(name, dao);
      }
    }
    Validate.isTrue(unknownChannels.isEmpty(),
        "Responses refer to these channels not in storage: " + unknownChannels);
    return m;
  }
}
