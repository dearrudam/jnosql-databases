/*
 *  Copyright (c) 2023 Contributors to the Eclipse Foundation
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
 *   Maximillian Arruda
 */
package org.eclipse.jnosql.databases.mongodb.integration;

import jakarta.inject.Inject;
import org.eclipse.jnosql.databases.mongodb.communication.MongoDBDocumentConfigurations;
import org.eclipse.jnosql.mapping.Convert;
import org.eclipse.jnosql.mapping.core.Converters;
import org.eclipse.jnosql.mapping.Database;
import org.eclipse.jnosql.mapping.DatabaseType;
import org.eclipse.jnosql.mapping.config.MappingConfigurations;
import org.eclipse.jnosql.mapping.document.DocumentEntityConverter;
import org.eclipse.jnosql.mapping.document.spi.DocumentExtension;
import org.eclipse.jnosql.mapping.reflection.Reflections;
import org.eclipse.jnosql.mapping.spi.EntityMetadataExtension;
import org.jboss.weld.junit5.auto.AddExtensions;
import org.jboss.weld.junit5.auto.AddPackages;
import org.jboss.weld.junit5.auto.EnableAutoWeld;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.jnosql.communication.driver.IntegrationTest.MATCHES;
import static org.eclipse.jnosql.communication.driver.IntegrationTest.NAMED;
import static org.eclipse.jnosql.databases.mongodb.communication.DocumentDatabase.INSTANCE;

@EnableAutoWeld
@AddPackages(value = {Convert.class, DocumentEntityConverter.class})
@AddPackages(Book.class)
@AddPackages(Reflections.class)
@AddPackages(Converters.class)
@AddExtensions({EntityMetadataExtension.class,
        DocumentExtension.class})
@EnabledIfSystemProperty(named = NAMED, matches = MATCHES)
class RepositoryIntegrationTest {

    static {
        INSTANCE.get("library");
        System.setProperty(MongoDBDocumentConfigurations.HOST.get() + ".1", INSTANCE.host());
        System.setProperty(MappingConfigurations.DOCUMENT_DATABASE.get(), "library");
    }

    @Inject
    @Database(DatabaseType.DOCUMENT)
    BookRepository repository;

    @Test
    void shouldSave() {
        Book book = new Book(randomUUID().toString(), "Effective Java", 1);
        assertThat(repository.save(book))
                .isNotNull()
                .isEqualTo(book);

        Book updated = new Book(book.id(), book.title() + " updated", 2);

        assertThat(repository.save(updated))
                .isNotNull()
                .isNotEqualTo(book);

        assertThat(repository.findById(book.id()))
                .isNotNull().get().isEqualTo(updated);

    }

    @Test
    void shouldDelete() {
        Book book = new Book(randomUUID().toString(), "Effective Java", 1);
        assertThat(repository.save(book))
                .isNotNull()
                .isEqualTo(book);

        repository.delete(book);
        assertThat(repository.findById(book.id()))
                .isNotNull().isEmpty();
    }

    @Test
    void shouldDeleteById() {
        Book book = new Book(randomUUID().toString(), "Effective Java", 1);
        assertThat(repository.save(book))
                .isNotNull()
                .isEqualTo(book);

        repository.deleteById(book.id());
        assertThat(repository.findById(book.id()))
                .isNotNull().isEmpty();
    }

    @Test
    void shouldDeleteAll() {
        for (int index = 0; index < 20; index++) {
            Book book = new Book(randomUUID().toString(), "Effective Java", 1);
            assertThat(repository.save(book))
                    .isNotNull()
                    .isEqualTo(book);
        }

        repository.deleteAll();
        assertThat(repository.findAll()).isEmpty();
    }

}
