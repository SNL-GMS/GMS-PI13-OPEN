package gms.shared.frameworks.soh.repository.performancemonitoring.converter;

import com.google.common.base.Preconditions;
import gms.shared.frameworks.osd.coi.soh.DurationSohMonitorValueAndStatus;
import gms.shared.frameworks.osd.coi.soh.PercentSohMonitorValueAndStatus;
import gms.shared.frameworks.osd.coi.soh.SohMonitorType;
import gms.shared.frameworks.osd.coi.soh.SohMonitorType.SohValueType;
import gms.shared.frameworks.osd.coi.soh.SohMonitorValueAndStatus;
import gms.shared.frameworks.osd.dao.soh.SohMonitorValueAndStatusDao;
import gms.shared.frameworks.utilities.jpa.EntityConverter;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import javax.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SohMonitorValueAndStatusDaoConverter implements
    EntityConverter<SohMonitorValueAndStatusDao, SohMonitorValueAndStatus> {

  private static final Logger logger =
      LoggerFactory.getLogger(SohMonitorValueAndStatusDaoConverter.class);

  @Override
  public SohMonitorValueAndStatusDao fromCoi(SohMonitorValueAndStatus coi,
      EntityManager entityManager) {
    Objects.requireNonNull(coi);
    Objects.requireNonNull(entityManager);
    Preconditions.checkState(entityManager.getTransaction().isActive());

    SohMonitorValueAndStatusDao dao = new SohMonitorValueAndStatusDao();
    if (coi instanceof PercentSohMonitorValueAndStatus) {
      Double val = (Double) coi.getValue().orElse(null);
      if(val != null){
        dao.setPercent(val.floatValue());
      }
    } else if (coi instanceof DurationSohMonitorValueAndStatus) {
      Duration duration = (Duration) coi.getValue().orElse(null);
      //prefer int to long in order to save DB storage space but int won't store nanosec precision, but does store sec
      //and don't need nano-sec precision
      if(duration != null){
        dao.setDuration((int) duration.getSeconds());
      }
    } else {
      throw new IllegalArgumentException("Unknown SohMonitorValueAndStatusType: " + coi.getClass().getTypeName());
    }

    dao.setMonitorType(coi.getMonitorType());
    dao.setStatus(coi.getStatus());

    return dao;
  }

  @Override
  public SohMonitorValueAndStatus<?> toCoi(SohMonitorValueAndStatusDao entity) {
    Objects.requireNonNull(entity);

    SohMonitorType sohMonitorType = entity.getMonitorType();
    if (sohMonitorType.getSohValueType() == SohValueType.PERCENT) {
      return PercentSohMonitorValueAndStatus.from(
          entity.getPercent() == null ? null :  entity.getPercent().doubleValue(),
          entity.getStatus(),
          entity.getMonitorType());
    } else if (sohMonitorType.getSohValueType() == SohValueType.DURATION) {

      //convert to secs...don't need more precision and allows us to store in smaller columns
      return DurationSohMonitorValueAndStatus.from(
          entity.getDuration() == null ? null : Duration.ofSeconds(Long.valueOf(entity.getDuration())),
          entity.getStatus(),
          entity.getMonitorType());
    } else {
      throw new IllegalArgumentException("Unknown SohMonitorValueAndStatusDao type: " + entity.getClass().getTypeName());
    }
  }
}
