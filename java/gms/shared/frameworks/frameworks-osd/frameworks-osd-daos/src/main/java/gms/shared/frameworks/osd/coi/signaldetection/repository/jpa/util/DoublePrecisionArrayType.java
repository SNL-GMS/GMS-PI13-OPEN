package gms.shared.frameworks.osd.coi.signaldetection.repository.jpa.util;

import com.vladmihalcea.hibernate.type.array.internal.ArraySqlTypeDescriptor;
import java.util.Properties;
import org.hibernate.type.AbstractSingleColumnStandardBasicType;
import org.hibernate.usertype.DynamicParameterizedType;

public class DoublePrecisionArrayType extends AbstractSingleColumnStandardBasicType<double[]>
    implements DynamicParameterizedType {

  public DoublePrecisionArrayType() {
    super(ArraySqlTypeDescriptor.INSTANCE, DoublePrecisionArrayTypeDescriptor.INSTANCE);
  }

  @Override
  public String getName() {
    return "double-precision-array";
  }

  @Override
  protected boolean registerUnderJavaType() {
    return true;
  }

  @Override
  public void setParameterValues(Properties parameters) {
    ((DoublePrecisionArrayTypeDescriptor) getJavaTypeDescriptor()).setParameterValues(parameters);
  }
}
