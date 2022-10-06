package eu.gaiax.difs.fc.core.config;

import org.hibernate.dialect.PostgreSQL82Dialect;
import org.hibernate.dialect.function.SQLFunctionTemplate;
import org.hibernate.type.StandardBasicTypes;

/**
 * @author hylke
 */
public class ArrayPostgresSQLDialect extends PostgreSQL82Dialect {

  public ArrayPostgresSQLDialect() {
    super();
    this.registerFunction("inarray", new SQLFunctionTemplate(StandardBasicTypes.TEXT, "ANY(?1)"));
  }

}
