package org.sitmun.tools;

import org.hibernate.dialect.PostgreSQL93Dialect;
import org.hibernate.type.descriptor.sql.LongVarcharTypeDescriptor;
import org.hibernate.type.descriptor.sql.SqlTypeDescriptor;

import java.sql.Types;

public class PostgresSQL93DialectFixed extends PostgreSQL93Dialect {

  @Override
  public SqlTypeDescriptor remapSqlTypeDescriptor(SqlTypeDescriptor sqlTypeDescriptor) {
    if (Types.CLOB == sqlTypeDescriptor.getSqlType()) {
      return LongVarcharTypeDescriptor.INSTANCE;
    }
    return super.remapSqlTypeDescriptor(sqlTypeDescriptor);
  }

}
