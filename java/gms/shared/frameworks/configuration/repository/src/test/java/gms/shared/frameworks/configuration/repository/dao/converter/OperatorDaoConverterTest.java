package gms.shared.frameworks.configuration.repository.dao.converter;

import static org.junit.jupiter.api.Assertions.assertTrue;

import gms.shared.frameworks.configuration.Operator;
import gms.shared.frameworks.configuration.Operator.Type;
import gms.shared.frameworks.configuration.repository.dao.OperatorDao;
import org.junit.jupiter.api.Test;

class OperatorDaoConverterTest {

  @Test
  void toCoi() {
    OperatorDaoConverter operatorDaoConverter = new OperatorDaoConverter();
    OperatorDao operatorDao = new OperatorDao();
    operatorDao.setType(Type.EQ);
    operatorDao.setNegated(true);
    Operator oper = operatorDaoConverter.toCoi(operatorDao);

    assertTrue(oper.getType() == Type.EQ, "Operator has wrong type");
    assertTrue(oper.isNegated(), "Operator should be negated");
  }

  @Test
  void fromCoi() {
    OperatorDaoConverter operatorDaoConverter = new OperatorDaoConverter();

    Operator oper = Operator.from(Type.EQ, true);
    OperatorDao operatorDao = operatorDaoConverter.fromCoi(oper);

    assertTrue(operatorDao.getType() == Type.EQ, "OperatorDao has wrong type");
    assertTrue(operatorDao.isNegated(), "OperatorDao should be negated");
  }
}