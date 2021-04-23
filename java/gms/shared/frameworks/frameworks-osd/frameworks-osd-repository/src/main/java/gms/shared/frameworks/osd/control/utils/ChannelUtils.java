package gms.shared.frameworks.osd.control.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import gms.shared.frameworks.osd.coi.channel.Channel;
import gms.shared.frameworks.osd.coi.channel.ChannelProcessingMetadataType;
import gms.shared.frameworks.osd.coi.channel.Orientation;
import gms.shared.frameworks.osd.coi.channel.repository.jpa.dataaccessobjects.ChannelConfiguredInputsDao;
import gms.shared.frameworks.osd.coi.channel.repository.jpa.dataaccessobjects.ChannelDao;
import gms.shared.frameworks.osd.coi.channel.repository.jpa.dataaccessobjects.LocationDao;
import gms.shared.frameworks.osd.coi.channel.repository.jpa.dataaccessobjects.OrientationDao;
import gms.shared.frameworks.osd.coi.datatransferobjects.CoiObjectMapperFactory;
import gms.shared.frameworks.osd.coi.signaldetection.Location;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.Tuple;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.ParameterExpression;
import javax.persistence.criteria.Root;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Deprecated(since = "12.4")
public class ChannelUtils {

  private static Logger logger = LoggerFactory.getLogger(ChannelUtils.class);

  private EntityManager entityManager;

  public ChannelUtils(EntityManager entityManager) {
    this.entityManager = entityManager;
  }

  /**
   * store the given channel configured inputs into the database. This is a low-level operation that
   * if executed alone will implicitly create a transaction
   *
   * @param channelDao       - the {@link ChannelDao} of the derived channel whose related channels
   *                         we are storing
   * @param configuredInputs - list of channel names associated with the given derived channel
   *                         (these names are what we query from the database to associate with the
   *                         given derived channel provided). note : This method assumes it is
   *                         wrapped into a transaction.
   */
  public void storeChannelConfiguredInputs(ChannelDao channelDao, List<String> configuredInputs) {
    final String givenChannelParameter = "channelName";
    CriteriaBuilder cb = entityManager.getCriteriaBuilder();

    // 1. we generate a query restricted to a specific entity object. this is different than creating a tuple
    //    query in that this query will generate instantiations of the entity type provided to the createQuery.
    //    The main benefit of the approach is we are guaranteed that either the query will create instances of
    //    this type or JPA will throw an exception.
    CriteriaQuery<ChannelDao> channelQueryCriteria = cb.createQuery(ChannelDao.class);

    // 2. Create the from clause in the query. We store the return value (which is a reference to
    //    the root type in the from clause) in a variable so that we can grab entity attributes (i.e.
    //    instance members) so that we can write constraints on the query (used in the where clause
    //    shown later)
    Root<ChannelDao> fromChannel = channelQueryCriteria.from(ChannelDao.class);

    // 3. Here we are creating a prepared query through the use of Parameter expressions, which
    //    can be considered as placeholder to locations in the prepared query where we can bind a
    //    a value of a particular type. In the case shown here, the query is filtering on the name
    //    of a given channel which is of type String, so we are creating a ParameterExpression object
    //    that will take a String
    ParameterExpression<String> channelName = cb.parameter(String.class, givenChannelParameter);
    channelQueryCriteria.select(fromChannel)
        .where(cb.equal(fromChannel.<String>get("name"), channelName));

    for (String parentChannel : configuredInputs) {
      // 4. Generate query, bind parameters in prepared query and get the single result. Here, notice
      //    we are using a TypedQuery instance instead of a normal Query Object to get better type
      //    safety from JPA
      TypedQuery<ChannelDao> channelQuery = entityManager.createQuery(channelQueryCriteria);
      channelQuery.setParameter(givenChannelParameter, parentChannel);

      // Criteria API gives us the ability to either return a stream of results or, as shown in this case,
      // a single result.
      try {
        ChannelDao parentChannelDao = channelQuery.getSingleResult();
        // 5. We now have all the info to construct the configured input record. So we generate
        //    the appropriate DAO and persist it.
        ChannelConfiguredInputsDao channelConfiguredInputsDao = new ChannelConfiguredInputsDao();
        channelConfiguredInputsDao.setChannelName(channelDao);
        channelConfiguredInputsDao.setRelatedChannelName(parentChannelDao);
        entityManager.persist(channelConfiguredInputsDao);
      } catch (NoResultException ex) {
        // TODO: Log this into log server.
        String msg = String.format(
            "Error storing channel %s. Invalid Configured Channel %s May not exist in database.",
            channelDao.getName(), parentChannel);
        logger.error(msg);
        throw new InvalidStorageException(msg, ex);
      }
    }
  }

  public List<Channel> constructChannels(List<ChannelDao> channelDaos, String stationName)
      throws IOException {
    List<Channel> result = new ArrayList<>();
    Map<String, List<String>> configuredInputs = retrieveRelatedChannels(channelDaos);
    for (ChannelDao channelDao : channelDaos) {
      final LocationDao location = channelDao.getLocation();
      final OrientationDao orientation = channelDao.getOrientationAngles();
      result.add(Channel.from(
          channelDao.getName(),
          channelDao.getCanonicalName(),
          channelDao.getDescription(),
          stationName,
          channelDao.getChannelDataType(),
          channelDao.getChannelBandType(),
          channelDao.getChannelInstrumentType(),
          channelDao.getChannelOrientationType(),
          channelDao.getChannelOrientationCode(),
          channelDao.getUnits(),
          channelDao.getNominalSampleRateHz(),
          Location
              .from(location.getLatitude(), location.getLongitude(), location.getDepth(),
                  location
                      .getElevation()),
          Orientation.from(orientation.getHorizontalAngleDeg(), orientation.getVerticalAngleDeg()),
          configuredInputs.get(channelDao.getName()),
          CoiObjectMapperFactory.getJsonObjectMapper()
              .readValue(channelDao.getProcessingDefinition(),
                  new TypeReference<Map<String, Object>>() {
                  }),
          CoiObjectMapperFactory.getJsonObjectMapper()
              .readValue(channelDao.getProcessingMetadata(),
                  new TypeReference<Map<ChannelProcessingMetadataType, Object>>() {
                  })));

    }
    return result;
  }

  /*
   * private method that will return a list of all related channel names (or in the case of
   * raw channels, an empty list).
   *
   * @param channelDao - given channel dao to return list of related channels for.
   * @return list of all channel names that given channel object was derived from (could be empty
   * if ChannelDao object is a raw channel.
   */
  private Map<String, List<String>> retrieveRelatedChannels(List<ChannelDao> channelDaos) {
    // 1. grab a criteria builder to build up our query
    CriteriaBuilder cb = entityManager.getCriteriaBuilder();

    // 2. Instantiate CriteriaQuery template from the CriteriaBuilder.
    CriteriaQuery<Tuple> relatedChannelQuery = cb.createTupleQuery();

    // 3. Add the from clause into the query. We store the return value (which is a reference to
    //    the root type in the from clause) in a variable so that we can grab entity attributes (i.e.
    //    instance members) so that we can write constraints on the query (used in the where clause
    //    shown later)
    Root<ChannelConfiguredInputsDao> fromRelatedChannel = relatedChannelQuery
        .from(ChannelConfiguredInputsDao.class);

    // 4. Generate the projection query. Here we want all the channels related to a specific derived
    //    channel so we need to grab the relatedChannelName from ChannelConfiguredInputsDao
    relatedChannelQuery.multiselect(
        fromRelatedChannel.get("channelName"),
        fromRelatedChannel.get("relatedChannelName"))
        .where(fromRelatedChannel.get("channelName")
            .in(channelDaos.stream().map(ChannelDao::getName).collect(
                Collectors.toList())));

    // 5. Generate the query through the entity manager, return the result stream and run
    //    transforms on the results to change them to a list of strings representing the channel
    //    names prior to collecting the lists and returning.
    var resultSet = entityManager.createQuery(relatedChannelQuery).getResultList();
    Map<String, List<String>> result = new HashMap<>();
    for (var res : resultSet) {
      final var channelName = (res.get(0, ChannelDao.class)).getName();
      if (!result.containsKey(channelName)) {
        result.put(channelName, new ArrayList<>());
      }
      result.get(channelName).add((res.get(1, ChannelDao.class)).getName());
    }
    return result;
  }
}
