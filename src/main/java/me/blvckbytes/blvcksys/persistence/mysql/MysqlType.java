package me.blvckbytes.blvcksys.persistence.mysql;

import com.google.common.primitives.Primitives;
import lombok.AllArgsConstructor;
import lombok.Getter;
import me.blvckbytes.blvcksys.persistence.query.EqualityOperation;

import java.util.Date;
import java.util.Optional;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/05/2022

  Maps a data-type in a MySQL database to it's corresponding Java type and provides
  flags like whether the type is of fixed length and what operations it supports.
*/
@AllArgsConstructor
public enum MysqlType {
  UUID(
    new String[] {
      "BINARY(16)"
    },
    new Class[] {
      java.util.UUID.class
    },
    true,
    new EqualityOperation[] {
      EqualityOperation.EQ,
      EqualityOperation.NEQ
    }
  ),

  TEXT(
    new String[] {
      "TEXT"
    },
    new Class[] {
      String.class
    },
    false,
    new EqualityOperation[] {
      EqualityOperation.EQ,
      EqualityOperation.NEQ,
      EqualityOperation.EQ_IC,
      EqualityOperation.NEQ_IC
    }
  ),

  VARCHAR(
    new String[] {
    "VARCHAR(255)"
    },
    new Class[] {
      String.class
    },
    true,
    new EqualityOperation[] {
      EqualityOperation.EQ,
      EqualityOperation.NEQ,
      EqualityOperation.EQ_IC,
      EqualityOperation.NEQ_IC
    }
  ),

  BOOLEAN(
    new String[] {
      "BOOL", "tinyint(1)"
    },
    new Class[] {
      boolean.class
    },
    false,
    new EqualityOperation[] {
      EqualityOperation.EQ,
      EqualityOperation.NEQ
    }
  ),

  LONG(
    new String[] {
      "BIGINT"
    },
    new Class[] {
      long.class
    },
    false,
    new EqualityOperation[] {
      EqualityOperation.EQ,
      EqualityOperation.NEQ,
      EqualityOperation.GT,
      EqualityOperation.GTE,
      EqualityOperation.LT,
      EqualityOperation.LTE
    }
  ),

  INTEGER(
    new String[] {
      "INT", "int"
    },
    new Class[] {
      int.class
    },
    false,
    new EqualityOperation[] {
      EqualityOperation.EQ,
      EqualityOperation.NEQ,
      EqualityOperation.GT,
      EqualityOperation.GTE,
      EqualityOperation.LT,
      EqualityOperation.LTE
    }
  ),

  DOUBLE(
    new String[] {
      "DOUBLE"
    },
    new Class[] {
      double.class
    },
    false,
    new EqualityOperation[] {
      EqualityOperation.EQ,
      EqualityOperation.NEQ,
      EqualityOperation.GT,
      EqualityOperation.GTE,
      EqualityOperation.LT,
      EqualityOperation.LTE
    }
  ),

  FLOAT(
    new String[] {
      "FLOAT"
    },
    new Class[] {
      float.class
    },
    false,
    new EqualityOperation[] {
      EqualityOperation.EQ,
      EqualityOperation.NEQ,
      EqualityOperation.GT,
      EqualityOperation.GTE,
      EqualityOperation.LT,
      EqualityOperation.LTE
    }
  ),

  DATETIME(
    new String[] {
      "DATETIME"
    },
    new Class[] {
      Date.class
    },
    false,
    new EqualityOperation[] {
      EqualityOperation.EQ,
      EqualityOperation.NEQ,
      EqualityOperation.GT,
      EqualityOperation.GTE,
      EqualityOperation.LT,
      EqualityOperation.LTE
    }
  )
  ;

  private final String[] queryTypes;

  @Getter
  private final Class<?>[] javaEquivalents;

  private final boolean hasLength;
  private final EqualityOperation[] supportedOps;

  @Override
  public String toString() {
    // Just choose the first type, as multiple types are only used for "aliases"
    return queryTypes[0];
  }

  /**
   * Checks whether the provided sql type string (as spit out by the DESC command)
   * matches the current type
   * @param sqlTypeStr SQL type string
   * @return True on match, false otherwise
   */
  public boolean matchesSQLTypeStr(String sqlTypeStr) {
    sqlTypeStr = sqlTypeStr.toLowerCase().replace(" ", "");

    for (String type : queryTypes) {
      if (sqlTypeStr.equalsIgnoreCase(type))
        return true;
    }

    return false;
  }

  /**
   * Converts a java type to a MySQL type
   * @param javaType Java type to convert
   * @param lengthRequired Whether this type has to have a specified length
   * @return Optional MySQL type
   */
  public static Optional<MysqlType> fromJavaType(Class<?> javaType, boolean lengthRequired) {
    if (Primitives.isWrapperType(javaType))
      javaType = Primitives.unwrap(javaType);

    for (MysqlType type : MysqlType.values()) {
      for (Class<?> javaEq : type.javaEquivalents) {
        if (javaEq.equals(javaType) && (!lengthRequired || type.hasLength))
          return Optional.of(type);
      }
    }
    return Optional.empty();
  }

  /**
   * Check if this type supports a given equality operation
   * @param op Equality operation to check
   */
  public boolean supportsOp(EqualityOperation op) {
    for (EqualityOperation supOp : supportedOps) {
      if (supOp == op)
        return true;
    }
    return false;
  }
}
