package gms.shared.frameworks.osd.coi.signaldetection.repository.jpa.dataaccessobjects;

public interface Updateable<T> {

  boolean update(T updatedValue);

}
