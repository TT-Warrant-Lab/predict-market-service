package com.fadx.predict_market_service.modules.common.utils;

import static com.fadx.predict_market_service.modules.market.api.enums.Operator.BETWEEN;

import com.fadx.predict_market_service.modules.market.api.dtos.FilterCriteria;
import com.fadx.predict_market_service.modules.market.api.dtos.SortCriteria;
import com.fadx.predict_market_service.modules.market.api.enums.Operator;
import com.fadx.predict_market_service.modules.market.api.enums.SortDirection;
import com.fadx.predict_market_service.modules.common.entity.CWAnalysis;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import lombok.NonNull;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

public class GenericSpecification {

  public static <T> Specification<@NonNull T> build(List<List<FilterCriteria>> filters) {

    return (root, query, cb) -> {
      List<Predicate> orPredicates = new ArrayList<>();

      for (List<FilterCriteria> andGroup : filters) {

        List<Predicate> andPredicates = new ArrayList<>();

        for (FilterCriteria f : andGroup) {
          Path<?> path = root.get(f.getField());

          Predicate p = buildPredicate(cb, path, f);
          andPredicates.add(p);
        }

        orPredicates.add(cb.and(andPredicates.toArray(new Predicate[0])));
      }

      return cb.or(orPredicates.toArray(new Predicate[0]));
    };
  }

  private static Predicate buildPredicate(CriteriaBuilder cb, Path<?> path, FilterCriteria f) {
    Class<?> type = path.getJavaType();

    if (type == Integer.class) {
      return buildPredicate(cb, path.as(Integer.class), f, Integer::valueOf);
    }
    if (type == Long.class) {
      return buildPredicate(cb, path.as(Long.class), f, Long::valueOf);
    }
    if (type == BigDecimal.class) {
      return buildPredicate(cb, path.as(BigDecimal.class), f, BigDecimal::new);
    }
    if (type == LocalDate.class) {
      return buildPredicate(cb, path.as(LocalDate.class), f, LocalDate::parse);
    }
    if (type == String.class) {
      return buildPredicate(cb, path.as(String.class), f, String::valueOf);
    }

    throw new IllegalArgumentException("Type " + type.getName() + " not supported");
  }

  public static Sort buildSort(List<SortCriteria> sorts) {

    if (sorts == null || sorts.isEmpty()) {
      return Sort.unsorted();
    }

    List<Sort.Order> orders = new ArrayList<>();

    for (SortCriteria s : sorts) {

      Sort.Direction direction =
          s.getDirection() == SortDirection.DESC ? Sort.Direction.DESC : Sort.Direction.ASC;

      orders.add(new Sort.Order(direction, s.getField()));
    }

    return Sort.by(orders);
  }

  public static Specification<@NonNull CWAnalysis> queryIn(String fieldName, List<String> symbols) {
    return (root, query, cb) -> root.get(fieldName).in(symbols);
  }

  private static <T extends Comparable<? super T>> Predicate buildPredicate(
      CriteriaBuilder cb, Expression<T> exp, FilterCriteria f, Function<String, T> converter) {
    T value = converter.apply(f.getValue().toString());
    T valueTo = null;

    Operator op = f.getOperator();
    if (op == BETWEEN) {
      valueTo = converter.apply(f.getValueTo().toString());
    }

    return switch (op) {
      case EQ -> cb.equal(exp, value);
      case NE -> cb.notEqual(exp, value);
      case GT -> cb.greaterThan(exp, value);
      case GE -> cb.greaterThanOrEqualTo(exp, value);
      case LT -> cb.lessThan(exp, value);
      case LE -> cb.lessThanOrEqualTo(exp, value);
      case BETWEEN -> cb.between(exp, value, valueTo);
      case LIKE -> {
        Expression<String> stringExp = exp.as(String.class);
        yield cb.like(cb.lower(stringExp), "%" + f.getValue().toString().toLowerCase() + "%");
      }
      case STARTS_WITH -> {
        Expression<String> stringExp = exp.as(String.class);
        yield cb.like(cb.lower(stringExp), f.getValue().toString().toLowerCase() + "%");
      }
      case IN -> {
        if (!(f.getValue() instanceof Collection<?> col)) {
          throw new IllegalArgumentException("IN operator requires a Collection value");
        }

        CriteriaBuilder.In<T> in = cb.in(exp);
        for (Object v : col) {
          in.value((T) v);
        }
        yield in;
      }
      case NOT_IN -> {
        if (!(f.getValue() instanceof Collection<?> col)) {
          throw new IllegalArgumentException("NOT_IN operator requires a Collection value");
        }

        CriteriaBuilder.In<T> in = cb.in(exp);
        for (Object v : col) {
          in.value((T) v);
        }
        yield cb.not(in);
      }
    };
  }
}
