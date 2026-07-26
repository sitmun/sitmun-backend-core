package org.sitmun.infrastructure.persistence.type.i18n;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.StringExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Optional;
import org.sitmun.administration.controller.dto.LiteralTranslationListItemDto;
import org.sitmun.administration.service.i18n.LiteralTranslationFilterModel;
import org.sitmun.administration.service.i18n.LiteralTranslationFilterModel.ColumnFilter;
import org.sitmun.administration.service.i18n.LiteralTranslationFilterModel.Condition;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
public class LiteralTranslationQuerydslRepositoryImpl
    implements LiteralTranslationQuerydslRepository {

  private final JPAQueryFactory queryFactory;

  public LiteralTranslationQuerydslRepositoryImpl(EntityManager entityManager) {
    this.queryFactory = new JPAQueryFactory(entityManager);
  }

  @Override
  public Page<LiteralTranslationListItemDto> findPageByLanguage(
      String language, LiteralTranslationFilterModel filter, String searchText, Pageable pageable) {
    QueryContext context = new QueryContext(language);
    BooleanBuilder where = buildWhereClause(filter, context);
    appendSearchText(where, searchText, context);

    List<LiteralTranslationListItemDto> content =
        baseContentQuery(context)
            .where(where)
            .offset(pageable.getOffset())
            .limit(pageable.getPageSize())
            .fetch();

    Long total = baseCountQuery(context).where(where).fetchOne();
    return new PageImpl<>(content, pageable, total == null ? 0L : total);
  }

  private BooleanBuilder buildWhereClause(
      LiteralTranslationFilterModel filter, QueryContext context) {
    BooleanBuilder where = new BooleanBuilder();
    appendStringColumnFilter(
        where, context.literalTranslation.literal, filter.columnFilter("literal"));
    appendStringColumnFilter(where, context.translatedValue, filter.columnFilter("translation"));
    return where;
  }

  private JPAQuery<LiteralTranslationListItemDto> baseContentQuery(QueryContext context) {
    return queryFactory
        .select(
            Projections.constructor(
                LiteralTranslationListItemDto.class,
                context.literalTranslation.id,
                context.literalTranslation.literal,
                context.translatedValue,
                context.literalTranslation.sourceLanguage.shortname,
                context.complete))
        .from(context.literalTranslation)
        .leftJoin(context.literalValue)
        .on(
            context.literalValue.literalTranslation.eq(context.literalTranslation),
            context.literalValue.language.shortname.eq(context.language));
  }

  private JPAQuery<Long> baseCountQuery(QueryContext context) {
    return queryFactory
        .select(context.literalTranslation.count())
        .from(context.literalTranslation)
        .leftJoin(context.literalValue)
        .on(
            context.literalValue.literalTranslation.eq(context.literalTranslation),
            context.literalValue.language.shortname.eq(context.language));
  }

  private void appendStringColumnFilter(
      BooleanBuilder where, StringExpression expression, ColumnFilter columnFilter) {
    if (columnFilter == null || columnFilter.conditions().isEmpty()) {
      return;
    }

    BooleanBuilder group = new BooleanBuilder();
    boolean useOr = "OR".equalsIgnoreCase(columnFilter.operator());

    for (Condition condition : columnFilter.conditions()) {
      Optional<BooleanExpression> predicate = stringPredicate(expression, condition);
      if (predicate.isEmpty()) {
        continue;
      }
      if (useOr) {
        group.or(predicate.get());
      } else {
        group.and(predicate.get());
      }
    }

    where.and(group);
  }

  private void appendSearchText(BooleanBuilder where, String searchText, QueryContext context) {
    if (searchText == null || searchText.isBlank()) {
      return;
    }
    String trimmed = searchText.trim();
    where.and(
        context
            .literalTranslation
            .literal
            .containsIgnoreCase(trimmed)
            .or(context.translatedValue.containsIgnoreCase(trimmed)));
  }

  private Optional<BooleanExpression> stringPredicate(
      StringExpression expression, Condition condition) {
    if (condition == null || condition.operator() == null || condition.operator().isBlank()) {
      return Optional.empty();
    }

    String operator = condition.operator().toLowerCase();

    return Optional.of(
        switch (operator) {
          case "equals" -> expression.equalsIgnoreCase(requireValue(condition));
          case "contains" -> expression.containsIgnoreCase(requireValue(condition));
          case "notcontains" -> expression.containsIgnoreCase(requireValue(condition)).not();
          case "startswith" -> expression.startsWithIgnoreCase(requireValue(condition));
          case "endswith" -> expression.endsWithIgnoreCase(requireValue(condition));
          case "blank" -> expression.isNull().or(expression.trim().eq(""));
          case "notblank" -> expression.isNotNull().and(expression.trim().ne(""));
          default ->
              throw new IllegalArgumentException("Unsupported operator: " + condition.operator());
        });
  }

  private String requireValue(Condition condition) {
    if (condition.value() == null || condition.value().isBlank()) {
      throw new IllegalArgumentException(
          "Operator '" + condition.operator() + "' requires a filter value");
    }
    return condition.value();
  }

  private static final class QueryContext {
    private final String language;
    private final QLiteralTranslation literalTranslation = QLiteralTranslation.literalTranslation;
    private final QLiteralTranslationValue literalValue =
        QLiteralTranslationValue.literalTranslationValue;
    private final QLiteralTranslationValue literalValueForCount =
        new QLiteralTranslationValue("literalValueForCount");
    private final QLanguage languageEntity = QLanguage.language;
    private final StringExpression translatedValue = literalValue.value;
    private final BooleanExpression complete =
        JPAExpressions.select(languageEntity.count())
            .from(languageEntity)
            .eq(
                JPAExpressions.select(literalValueForCount.count())
                    .from(literalValueForCount)
                    .where(literalValueForCount.literalTranslation.eq(literalTranslation)));

    private QueryContext(String language) {
      this.language = language;
    }
  }
}
