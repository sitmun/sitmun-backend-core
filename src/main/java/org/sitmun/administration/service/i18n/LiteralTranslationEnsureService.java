package org.sitmun.administration.service.i18n;

import jakarta.persistence.EntityManager;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Session;
import org.sitmun.infrastructure.persistence.type.i18n.DatabaseDefaultLanguageResolver;
import org.sitmun.infrastructure.persistence.type.i18n.Language;
import org.sitmun.infrastructure.persistence.type.i18n.LiteralTranslation;
import org.sitmun.infrastructure.persistence.type.i18n.LiteralTranslationRepository;
import org.sitmun.infrastructure.persistence.type.i18n.LiteralTranslationValue;
import org.sitmun.infrastructure.persistence.type.i18n.LiteralTranslationValueRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Slf4j
public class LiteralTranslationEnsureService {

  private static final Pattern TAG_PATTERN = Pattern.compile("<t>(.*?)</t>", Pattern.DOTALL);

  private static final String INSERT_SAVEPOINT = "stm_ltr_insert";

  private final LiteralTranslationRepository literalTranslationRepository;
  private final LiteralTranslationValueRepository literalTranslationValueRepository;
  private final DatabaseDefaultLanguageResolver databaseDefaultLanguageResolver;
  private final EntityManager entityManager;

  public LiteralTranslationEnsureService(
      LiteralTranslationRepository literalTranslationRepository,
      LiteralTranslationValueRepository literalTranslationValueRepository,
      DatabaseDefaultLanguageResolver databaseDefaultLanguageResolver,
      EntityManager entityManager) {
    this.literalTranslationRepository = literalTranslationRepository;
    this.literalTranslationValueRepository = literalTranslationValueRepository;
    this.databaseDefaultLanguageResolver = databaseDefaultLanguageResolver;
    this.entityManager = entityManager;
  }

  public static Set<String> extractLiteralKeys(String html) {
    Set<String> keys = new LinkedHashSet<>();
    if (!StringUtils.hasText(html) || !html.contains("<t>")) {
      return keys;
    }
    Matcher matcher = TAG_PATTERN.matcher(html);
    while (matcher.find()) {
      String key = matcher.group(1);
      if (StringUtils.hasText(key)) {
        keys.add(key);
      }
    }
    return keys;
  }

  @Transactional
  public void ensureLiteralsFromTemplateHtml(String templateHtml) {
    Set<String> keys = extractLiteralKeys(templateHtml);
    if (keys.isEmpty()) {
      return;
    }
    Language sourceLanguage = databaseDefaultLanguageResolver.requireLanguage();
    for (String key : keys) {
      ensureLiteral(key, sourceLanguage);
    }
  }

  @Transactional
  public void ensureLiteral(String literal, Language sourceLanguage) {
    if (!StringUtils.hasText(literal) || sourceLanguage == null) {
      return;
    }
    Optional<LiteralTranslation> existing = literalTranslationRepository.findByLiteral(literal);
    if (existing.isPresent()) {
      syncSourceValueIfMissing(existing.get(), literal);
      return;
    }
    try {
      insertLiteral(literal, sourceLanguage);
    } catch (DataIntegrityViolationException ex) {
      log.debug("Literal already exists concurrently: {}", literal);
      literalTranslationRepository
          .findByLiteral(literal)
          .ifPresent(found -> syncSourceValueIfMissing(found, literal));
    }
  }

  private void insertLiteral(String literal, Language sourceLanguage) {
    Session session = entityManager.unwrap(Session.class);
    AtomicBoolean duplicate = new AtomicBoolean();
    session.doWork(
        connection -> {
          // A SQLException that leaves doWork marks this Hibernate transaction rollback-only.
          Savepoint savepoint = connection.setSavepoint(INSERT_SAVEPOINT);
          try {
            int literalId = nextSequenceValue(connection, "LTR_ID");
            try (PreparedStatement insert =
                connection.prepareStatement(
                    "INSERT INTO STM_LITERAL_TRANSLATION (LTR_ID, LTR_LITERAL, LTR_LANID) VALUES (?, ?, ?)")) {
              insert.setInt(1, literalId);
              insert.setString(2, literal);
              insert.setInt(3, sourceLanguage.getId());
              insert.executeUpdate();
            }
            int valueId = nextSequenceValue(connection, "LTV_ID");
            try (PreparedStatement insert =
                connection.prepareStatement(
                    "INSERT INTO STM_LITERAL_TRANSLATION_VALUE (LTV_ID, LTV_LTRID, LTV_LANID, LTV_VALUE) VALUES (?, ?, ?, ?)")) {
              insert.setInt(1, valueId);
              insert.setInt(2, literalId);
              insert.setInt(3, sourceLanguage.getId());
              insert.setString(4, literal);
              insert.executeUpdate();
            }
            connection.releaseSavepoint(savepoint);
          } catch (SQLException ex) {
            connection.rollback(savepoint);
            if (!isUniqueViolation(ex)) {
              throw ex;
            }
            duplicate.set(true);
          }
        });
    if (duplicate.get()) {
      throw new DataIntegrityViolationException("literal exists");
    }
  }

  private static int nextSequenceValue(Connection connection, String name) throws SQLException {
    while (true) {
      int current;
      try (PreparedStatement select =
          connection.prepareStatement(
              "SELECT SEQ_COUNT FROM STM_SEQUENCE WHERE SEQ_NAME = ? FOR UPDATE")) {
        select.setString(1, name);
        try (ResultSet rows = select.executeQuery()) {
          if (!rows.next()) {
            throw new SQLException("Missing sequence " + name);
          }
          current = rows.getInt(1);
        }
      }
      int next = current + 1;
      try (PreparedStatement update =
          connection.prepareStatement(
              "UPDATE STM_SEQUENCE SET SEQ_COUNT = ? WHERE SEQ_NAME = ? AND SEQ_COUNT = ?")) {
        update.setInt(1, next);
        update.setString(2, name);
        update.setInt(3, current);
        if (update.executeUpdate() == 1) {
          return next;
        }
      }
    }
  }

  private static boolean isUniqueViolation(SQLException failure) {
    SQLException current = failure;
    while (current != null) {
      if ("23505".equals(current.getSQLState())
          || (current.getErrorCode() == 1 && "23000".equals(current.getSQLState()))) {
        return true;
      }
      current = current.getNextException();
    }
    return false;
  }

  private void syncSourceValueIfMissing(LiteralTranslation literalTranslation, String literal) {
    Language source = literalTranslation.getSourceLanguage();
    if (source == null || !StringUtils.hasText(source.getShortname())) {
      return;
    }
    Optional<LiteralTranslationValue> existing =
        literalTranslationValueRepository.findByLiteralTranslationIdAndLanguageShortname(
            literalTranslation.getId(), source.getShortname());
    if (existing.isEmpty()) {
      upsertValue(literalTranslation, source, literal);
    }
  }

  private void upsertValue(LiteralTranslation literalTranslation, Language language, String value) {
    LiteralTranslationValue row =
        literalTranslationValueRepository
            .findByLiteralTranslationIdAndLanguageShortname(
                literalTranslation.getId(), language.getShortname())
            .orElseGet(LiteralTranslationValue::new);
    row.setLiteralTranslation(literalTranslation);
    row.setLanguage(language);
    row.setValue(value);
    literalTranslationValueRepository.save(row);
  }
}
