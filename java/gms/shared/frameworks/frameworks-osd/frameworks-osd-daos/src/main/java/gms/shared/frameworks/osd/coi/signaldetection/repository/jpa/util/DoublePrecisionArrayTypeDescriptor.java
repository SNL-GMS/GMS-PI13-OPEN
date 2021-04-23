package gms.shared.frameworks.osd.coi.signaldetection.repository.jpa.util;

import com.vladmihalcea.hibernate.type.array.internal.AbstractArrayTypeDescriptor;

public class DoublePrecisionArrayTypeDescriptor extends AbstractArrayTypeDescriptor<double[]> {

  public static final DoublePrecisionArrayTypeDescriptor INSTANCE =
      new DoublePrecisionArrayTypeDescriptor();

  public DoublePrecisionArrayTypeDescriptor() {
    super(double[].class);
  }

  @Override
  protected String getSqlArrayType() {
    return "float8";
  }
}
