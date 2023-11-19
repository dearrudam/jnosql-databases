/*
 *  Copyright (c) 2022 Contributors to the Eclipse Foundation
 *   All rights reserved. This program and the accompanying materials
 *   are made available under the terms of the Eclipse Public License v1.0
 *   and Apache License v2.0 which accompanies this distribution.
 *   The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 *   and the Apache License v2.0 is available at http://www.opensource.org/licenses/apache2.0.php.
 *
 *   You may elect to redistribute this code under either of these licenses.
 *
 *   Contributors:
 *
 *   Otavio Santana
 */

package org.eclipse.jnosql.databases.arangodb.communication;

import com.arangodb.ArangoDB;
import org.assertj.core.api.SoftAssertions;
import org.eclipse.jnosql.communication.TypeReference;
import org.eclipse.jnosql.communication.document.Document;
import org.eclipse.jnosql.communication.document.DocumentDeleteQuery;
import org.eclipse.jnosql.communication.document.DocumentEntity;
import org.eclipse.jnosql.communication.document.DocumentQuery;
import org.eclipse.jnosql.communication.document.Documents;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.jnosql.communication.document.DocumentDeleteQuery.delete;
import static org.eclipse.jnosql.communication.document.DocumentQuery.select;
import static org.eclipse.jnosql.communication.driver.IntegrationTest.MATCHES;
import static org.eclipse.jnosql.communication.driver.IntegrationTest.NAMED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@EnabledIfSystemProperty(named = NAMED, matches = MATCHES)
public class ArangoDBDocumentManagerTest {

    public static final String COLLECTION_NAME = "person";
    private static final String DATABASE = "database";
    private ArangoDBDocumentManager entityManager;
    private Random random;
    private final String KEY_NAME = "_key";

    @BeforeEach
    void setUp() {
        random = new Random();
        entityManager = DocumentDatabase.INSTANCE.get().apply(DATABASE);
        entityManager.delete(DocumentDeleteQuery.delete().from(COLLECTION_NAME).build());

    }

    @AfterEach
    void after() {
        entityManager.delete(DocumentDeleteQuery.delete().from(COLLECTION_NAME).build());
    }

    @Test
    void shouldSave() {
        DocumentEntity entity = getEntity();

        DocumentEntity documentEntity = entityManager.insert(entity);
        assertTrue(documentEntity.documents().stream().map(Document::name).anyMatch(s -> s.equals(KEY_NAME)));
    }

    @Test
    void shouldUpdateSave() {
        DocumentEntity entity = getEntity();
        entityManager.insert(entity);
        Document newField = Documents.of("newField", "10");
        entity.add(newField);
        DocumentEntity updated = entityManager.update(entity);
        assertEquals(newField, updated.find("newField").get());
    }

    @Test
    void shouldRemoveEntity() {
        DocumentEntity documentEntity = entityManager.insert(getEntity());
        Document id = documentEntity.find("_key").get();
        DocumentQuery select = select().from(COLLECTION_NAME).where(id.name()).eq(id.get()).build();
        DocumentDeleteQuery deleteQuery = delete().from(COLLECTION_NAME).where(id.name()).eq(id.get()).build();
        entityManager.delete(deleteQuery);
        assertThat(entityManager.select(select)).hasSize(0);
    }

    @Test
    void shouldRemoveEntity2() {
        DocumentEntity documentEntity = entityManager.insert(getEntity());
        Document id = documentEntity.find("name").get();
        DocumentQuery select = select().from(COLLECTION_NAME).where(id.name()).eq(id.get()).build();
        DocumentDeleteQuery deleteQuery = delete().from(COLLECTION_NAME).where(id.name()).eq(id.get()).build();
        entityManager.delete(deleteQuery);
        assertThat(entityManager.select(select)).hasSize(0);
    }


    @Test
    void shouldFindDocument() {
        DocumentEntity entity = entityManager.insert(getEntity());
        Document id = entity.find(KEY_NAME).get();
        DocumentQuery query = select().from(COLLECTION_NAME).where(id.name()).eq(id.get()).build();
        List<DocumentEntity> entities = entityManager.select(query).toList();
        assertFalse(entities.isEmpty());
        DocumentEntity documentEntity = entities.get(0);
        assertEquals(entity.find(KEY_NAME).get().value().get(String.class), documentEntity.find(KEY_NAME).get()
                .value().get(String.class));
        assertEquals(entity.find("name").get(), documentEntity.find("name").get());
        assertEquals(entity.find("city").get(), documentEntity.find("city").get());
    }


    @Test
    void shouldSaveSubDocument() {
        DocumentEntity entity = getEntity();
        entity.add(Document.of("phones", Document.of("mobile", "1231231")));
        DocumentEntity entitySaved = entityManager.insert(entity);
        Document id = entitySaved.find(KEY_NAME).get();
        DocumentQuery query = select().from(COLLECTION_NAME).where(id.name()).eq(id.get()).build();
        DocumentEntity entityFound = entityManager.select(query).collect(Collectors.toList()).get(0);
        Document subDocument = entityFound.find("phones").get();
        List<Document> documents = subDocument.get(new TypeReference<>() {
        });
        assertThat(documents).contains(Document.of("mobile", "1231231"));
    }

    @Test
    void shouldSaveSubDocument2() {
        DocumentEntity entity = getEntity();
        entity.add(Document.of("phones", Arrays.asList(Document.of("mobile", "1231231"), Document.of("mobile2", "1231231"))));
        DocumentEntity entitySaved = entityManager.insert(entity);
        Document id = entitySaved.find(KEY_NAME).get();
        DocumentQuery query = select().from(COLLECTION_NAME).where(id.name()).eq(id.get()).build();
        DocumentEntity entityFound = entityManager.select(query).collect(Collectors.toList()).get(0);
        Document subDocument = entityFound.find("phones").get();
        List<Document> documents = subDocument.get(new TypeReference<>() {
        });
        assertThat(documents).contains(Document.of("mobile", "1231231"),
                Document.of("mobile2", "1231231"));
    }


    @Test
    void shouldConvertFromListSubdocumentList() {
        DocumentEntity entity = createDocumentList();
        entityManager.insert(entity);

    }

    @Test
    void shouldRetrieveListSubdocumentList() {
        DocumentEntity entity = entityManager.insert(createDocumentList());
        Document key = entity.find(KEY_NAME).get();
        DocumentQuery query = select().from("AppointmentBook").where(key.name()).eq(key.get()).build();

        DocumentEntity documentEntity = entityManager.singleResult(query).get();
        assertNotNull(documentEntity);

        List<List<Document>> contacts = (List<List<Document>>) documentEntity.find("contacts").get().get();

        assertEquals(3, contacts.size());
        assertTrue(contacts.stream().allMatch(d -> d.size() == 3));
    }

    @Test
    void shouldRunAQL() {
        DocumentEntity entity = getEntity();
        DocumentEntity entitySaved = entityManager.insert(entity);

        String aql = "FOR a IN person FILTER a.name == @name RETURN a";
        List<DocumentEntity> entities = entityManager.aql(aql,
                singletonMap("name", "Poliana")).collect(Collectors.toList());
        assertNotNull(entities);
    }


    @Test
    void shouldCount() {
        DocumentEntity entity = getEntity();
        entityManager.insert(entity);

        assertTrue(entityManager.count(COLLECTION_NAME) > 0);
    }

    @Test
    void shouldReadFromDifferentBaseDocumentUsingInstance() {
        entityManager.insert(getEntity());
        ArangoDB arangoDB = DefaultArangoDBDocumentManager.class.cast(entityManager).getArangoDB();
        arangoDB.db(DATABASE).collection(COLLECTION_NAME).insertDocument(new Person());
        DocumentQuery select = select().from(COLLECTION_NAME).build();
        List<DocumentEntity> entities = entityManager.select(select).collect(Collectors.toList());
        assertFalse(entities.isEmpty());
    }

    @Test
    void shouldReadFromDifferentBaseDocumentUsingMap() {
        entityManager.insert(getEntity());
        ArangoDB arangoDB = DefaultArangoDBDocumentManager.class.cast(entityManager).getArangoDB();
        Map<String, Object> map = new HashMap<>();
        map.put("name", "Poliana");
        map.put("city", "Salvador");
        arangoDB.db(DATABASE).collection(COLLECTION_NAME).insertDocument(map);
        DocumentQuery select = select().from(COLLECTION_NAME).build();
        List<DocumentEntity> entities = entityManager.select(select).collect(Collectors.toList());
        assertFalse(entities.isEmpty());
    }

    @Test
    void shouldExecuteAQLWithTypeParams() {
        entityManager.insert(getEntity());
        String aql = "FOR a IN person FILTER a.name == @name RETURN a.name";
        List<String> entities = entityManager.aql(aql,
                singletonMap("name", "Poliana"), String.class).collect(Collectors.toList());

        assertFalse(entities.isEmpty());
    }

    @Test
    void shouldExecuteAQLWithType() {
        entityManager.insert(getEntity());
        String aql = "FOR a IN person RETURN a.name";
        List<String> entities = entityManager.aql(aql, String.class).collect(Collectors.toList());
        assertFalse(entities.isEmpty());
    }

    @Test
    void shouldInsertNull() {
        DocumentEntity entity = DocumentEntity.of(COLLECTION_NAME);
        entity.add(Document.of(KEY_NAME, String.valueOf(random.nextLong())));
        entity.add(Document.of("name", null));
        DocumentEntity documentEntity = entityManager.insert(entity);
        Optional<Document> name = documentEntity.find("name");
        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(name).isPresent();
            soft.assertThat(name).get().extracting(Document::name).isEqualTo("name");
            soft.assertThat(name).get().extracting(Document::get).isNull();
        });
    }

    @Test
    void shouldUpdateNull(){
        var entity = entityManager.insert(getEntity());
        entity.add(Document.of("name", null));
        var documentEntity = entityManager.update(entity);
        Optional<Document> name = documentEntity.find("name");
        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(name).isPresent();
            soft.assertThat(name).get().extracting(Document::name).isEqualTo("name");
            soft.assertThat(name).get().extracting(Document::get).isNull();
        });
    }

    @Test
    void shouldDeleteAll() {
        for (int index = 0; index < 20; index++) {
            DocumentEntity entity = getEntity();
            entityManager.insert(entity);
        }
        DocumentDeleteQuery deleteQuery = delete().from(COLLECTION_NAME).build();
        entityManager.delete(deleteQuery);
        DocumentQuery select = select().from(COLLECTION_NAME).build();
        List<DocumentEntity> entities = entityManager.select(select).toList();
        assertThat(entities).isEmpty();
    }

    private DocumentEntity getEntity() {
        DocumentEntity entity = DocumentEntity.of(COLLECTION_NAME);
        Map<String, Object> map = new HashMap<>();
        map.put("name", "Poliana");
        map.put("city", "Salvador");
        entity.add(Document.of(KEY_NAME, String.valueOf(random.nextLong())));
        List<Document> documents = Documents.of(map);
        documents.forEach(entity::add);
        return entity;
    }

    private DocumentEntity createDocumentList() {
        DocumentEntity entity = DocumentEntity.of("AppointmentBook");
        entity.add(Document.of("_id", "ids"));
        List<List<Document>> documents = new ArrayList<>();

        documents.add(asList(Document.of("name", "Ada"), Document.of("type", ContactType.EMAIL),
                Document.of("information", "ada@lovelace.com")));

        documents.add(asList(Document.of("name", "Ada"), Document.of("type", ContactType.MOBILE),
                Document.of("information", "11 1231231 123")));

        documents.add(asList(Document.of("name", "Ada"), Document.of("type", ContactType.PHONE),
                Document.of("information", "phone")));

        entity.add(Document.of("contacts", documents));
        return entity;
    }

}