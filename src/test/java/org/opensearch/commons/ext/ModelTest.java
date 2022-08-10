/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.commons.ext;

import org.junit.jupiter.api.Test;
import org.junit.platform.commons.logging.Logger;
import org.junit.platform.commons.logging.LoggerFactory;
import org.opensearch.common.io.stream.BytesStreamInput;
import org.opensearch.common.io.stream.BytesStreamOutput;
import org.opensearch.common.unit.TimeValue;
import org.opensearch.commons.exp.model.util.ModelSerializer;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.opensearch.commons.exp.model.util.ModelSerializer.checkType;
import static org.opensearch.commons.exp.model.util.ModelSerializer.getListGeneric;
import static org.opensearch.commons.ext.ModelTestRegistry.TEST_MODELS;

public final class ModelTest {

    private static final Logger LOG = LoggerFactory.getLogger(ModelTest.class);

    private static final int NULL_FIELD_PROBABILITY = 20;
    private static final List<String> ALPHABET = List.of("a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m", "n", "o", "p", "q", "r", "s", "t", "u", "v", "w", "x", "y", "z", "_", "-");

    @Test
    public void testModels() throws Exception {
        final Random random = new Random();
        final long seed = random.nextInt();
        LOG.warn(() -> String.format(Locale.getDefault(), "The seed for the following tests is [%s]", seed));
        modelBuilder(seed);
    }

    public void modelBuilder(long seed) throws Exception {
        final Random random = new Random(seed);
        for (Class<?> modelClass : TEST_MODELS) {
            LOG.info(() -> String.format(Locale.getDefault(), "Testing %s", modelClass.getName()));
            // TODO make model instance population parameterized
            final Object modelA = createModel(new Random(seed+1), modelClass);
            final Object modelB = createModel(new Random(seed+1), modelClass);
            final Object modelC = createModel(random, modelClass);
            final Object modelD = createModel(random, modelClass);
            LOG.info(() -> String.format(Locale.getDefault(), "\tModel instances: \n\tmodelA: %s\n\tmodelB: %s\n\tmodelC: %s\n\tmodelD: %s", modelA, modelB, modelC, modelD));
            assertEquals(modelA, modelB);
            assertNotEquals(modelA, modelC);
            assertNotEquals(modelA, modelD);
            assertNotEquals(modelB, modelC);
            assertNotEquals(modelB, modelD);
            assertNotEquals(modelC, modelD);
            assertEquals(modelA.hashCode(), modelB.hashCode());
            assertEquals(3, new HashSet<>(List.of(modelA, modelB, modelC, modelD)).size());

            LOG.info(() -> String.format(Locale.getDefault(), "\tVerifying stream input/output serialization consistency for %s", modelClass));
            // TODO: test against all stream output/input types
            final BytesStreamOutput output = new BytesStreamOutput();
            ModelSerializer.write(output, modelA);
            final BytesStreamInput input = new BytesStreamInput(output.bytes().toBytesRef().bytes);
            final Object modelE = ModelSerializer.read(input, modelClass);
            assertEquals(modelA, modelE);
            for (final Field field : ModelSerializer.getSortedFields(modelClass)) {
                assertEquals(field.get(modelA), field.get(modelB));
            }

            // TODO: test XContent parser/builder
        }
    }


    public static <T> T createModel(final Random random, final Class<T> modelClass) {
        try {
            final T model = modelClass.getConstructor().newInstance();
            for (final Field field : ModelSerializer.getSortedFields(modelClass)) {
                final boolean isNull = random.nextInt(100) < NULL_FIELD_PROBABILITY;
                if (!isNull) {
                    if (checkType(field, Boolean.class))
                        field.set(model, random.nextBoolean());
                    else if (checkType(field, String.class))
                        field.set(model, nextString(random));
                    else if (checkType(field, long.class))
                        field.set(model, random.nextLong());
                    else if (checkType(field, Integer.class))
                        field.set(model, random.nextInt());
                    else if (checkType(field, Instant.class))
                        field.set(model, Instant.ofEpochSecond(Math.abs(random.nextInt())));
                    else if (checkType(field, TimeValue.class))
                        field.set(model, new TimeValue(Math.abs(random.nextLong()), TimeUnit.values()[random.nextInt(TimeUnit.values().length)]));
                    else if (checkType(field, List.class, String.class))
                        field.set(model, nextListString(random));
                    else if (checkType(field, List.class)) {
                        field.set(model, nextList(random, getListGeneric(field)));
                    }
                }
            }
            return model;
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static String nextString(final Random random) {
        final int length = random.nextInt(10) + 1;
        String string = "";
        for (int i = 0; i < length; i++) {
            string = string + ALPHABET.get(random.nextInt(ALPHABET.size()));
        }
        return string;
    }

    public static List<String> nextListString(final Random random) {
        final int length = random.nextInt(10) + 1;
        final List<String> list = new ArrayList<>();
        for (int i = 0; i < length; i++) {
            list.add(nextString(random));
        }
        return list;
    }

    public static <T> List<T> nextList(final Random random, final Class<T> type) {
        final int length = random.nextInt(10) + 1;
        final List<T> list = new ArrayList<>();
        for (int i = 0; i < length; i++) {
            list.add(createModel(random, type));
        }
        return list;
    }


}
