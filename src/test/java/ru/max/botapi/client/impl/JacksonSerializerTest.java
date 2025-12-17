package ru.max.botapi.client.impl;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import ru.max.botapi.UnitTest;
import ru.max.botapi.exceptions.SerializationException;
import ru.max.botapi.model.MessageBody;
import ru.max.botapi.model.MessageCreatedUpdate;
import ru.max.botapi.model.Update;
import ru.max.botapi.model.UpdateList;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;


@Category(UnitTest.class)
public class JacksonSerializerTest {
    private JacksonSerializer serializer = new JacksonSerializer();

    @Test(expected = SerializationException.class)
    public void shouldThrowSerializationException() throws Exception {
        serializer.serialize(new NotSerializableClass());
    }

    @Test(expected = SerializationException.class)
    public void shouldThrowSerializationException2() throws Exception {
        serializer.serializeToString(new NotSerializableClass());
    }

    @Test(expected = SerializationException.class)
    public void shouldThrowSerializationExceptionOnDeserialization() throws Exception {
        serializer.deserialize("{", MessageBody.class);
    }

    @Test(expected = SerializationException.class)
    public void shouldThrowSerializationExceptionOnDeserialization2() throws Exception {
        serializer.deserialize(new byte[]{1, 2, 3}, MessageBody.class);
    }

    @Test(expected = SerializationException.class)
    public void shouldThrowSerializationExceptionOnDeserialization3() throws Exception {
        serializer.deserialize(new ByteArrayInputStream(new byte[]{1, 2, 3}), MessageBody.class);
    }

    @Test
    public void shouldReturnNullOnEmptyInput() throws Exception {
        assertThat(serializer.deserialize((byte[]) null, Object.class), is(nullValue()));
        assertThat(serializer.deserialize((String) null, Object.class), is(nullValue()));
        assertThat(serializer.deserialize((InputStream) null, Object.class), is(nullValue()));
        assertThat(serializer.deserialize("", Object.class), is(nullValue()));
    }

    @Test
    public void shouldReturnNullOnSerialize() throws Exception {
        assertThat(serializer.serialize(null), is(nullValue()));
        assertThat(serializer.serializeToString(null), is(nullValue()));
    }

    @Test
    public void testSerializeToString() throws Exception {
        MessageBody object = new MessageBody("mid", 1L, "text", Collections.emptyList());
        String serialized = serializer.serializeToString(object);
        MessageBody deserialized = serializer.deserialize(serialized, MessageBody.class);
        assertThat(deserialized, is(object));
    }

    @Test
    public void testSerialize() throws Exception {
        MessageBody object = new MessageBody("mid", 1L, "text", Collections.emptyList());
        byte[] serialized = serializer.serialize(object);
        MessageBody deserialized = serializer.deserialize(serialized, MessageBody.class);
        assertThat(deserialized, is(object));
    }

    @Test
    public void testDeserializeStream() throws Exception {
        MessageBody object = new MessageBody("mid", 1L, "text", Collections.emptyList());
        byte[] serialized = serializer.serialize(object);
        MessageBody deserialized = serializer.deserialize(new ByteArrayInputStream(serialized), MessageBody.class);
        assertThat(deserialized, is(object));
    }

    @Test
    public void testUpdates() throws SerializationException {
        String json = "{\"updates\":[{\"message\":{\"recipient\":{\"chat_id\":8155625,\"chat_type\":\"dialog\",\"user_id\":92478166},\"timestamp\":1763496854749,\"sender\":{\"user_id\":100625215,\"first_name\":\".\",\"last_name\":\".\",\"is_bot\":false,\"last_activity_time\":1763495780000,\"name\":\". .\"},\"message\":{\"mid\":\"mid.00000000007c71e9019a989a84dd5e09\",\"seq\":115572529872854537,\"text\":\"5\"}},\"timestamp\":1763496854749,\"user_locale\":\"ru\",\"update_type\":\"message_created\"}],\"marker\":2546143}";
        UpdateList deserialize = serializer.deserialize(json, UpdateList.class);
        assertThat(deserialize.getUpdates(), hasSize(1));

        MessageCreatedUpdate messageCreatedUpdate = ((MessageCreatedUpdate) deserialize.getUpdates().get(0));
        assertThat(messageCreatedUpdate.getMessage().getBody(), notNullValue());
    }

    /**
     * Тест проверки десериализации сообщения от webhook
     * @throws SerializationException
     */
    @Test
    public void messageCreatedWebhookTest() throws SerializationException {
        String json = "{\"message\":{\"recipient\":{\"chat_id\":-69729329042751,\"chat_type\":\"chat\"},\"timestamp\":1765973375344,\"body\":{\"mid\":\"mid.ffffc094e01caac1019b2c3739701ac6\",\"seq\":115734831126551238,\"text\":\"3\"},\"sender\":{\"user_id\":100625215,\"first_name\":\"Череповецкий\",\"last_name\":\"Государственный Университет\",\"is_bot\":false,\"last_activity_time\":1765972868000,\"name\":\"Череповецкий Государственный Университет\"}},\"timestamp\":1765973375344,\"update_type\":\"message_created\"}";

        Update message = serializer.deserialize(json.getBytes(StandardCharsets.UTF_8), Update.class);
        assertTrue(message instanceof MessageCreatedUpdate);

        assertNotNull(((MessageCreatedUpdate) message).getMessage().getBody());
    }

    //Disabled
    /**
     * Тест проверки десериализации сообщения от longPolling
     * @throws SerializationException
     *//*
    @Test
    public void messageCreatedPollingTest() throws SerializationException {
        String json = "{\"updates\":[{\"message\":{\"recipient\":{\"chat_id\":-69729329042751,\"chat_type\":\"chat\"},\"timestamp\":1765982632541,\"sender\":{\"user_id\":100625215,\"first_name\":\"Череповецкий\",\"last_name\":\"Государственный Университет\",\"is_bot\":false,\"last_activity_time\":1765982588000,\"name\":\"Череповецкий Государственный Университет\"},\"message\":{\"mid\":\"mid.ffffc094e01caac1019b2cc47a5d206a\",\"seq\":115735437806215274,\"text\":\"36\"}},\"timestamp\":1765982632541,\"update_type\":\"message_created\"}],\"marker\":10319035}";

        Update message = serializer.deserialize(json.getBytes(StandardCharsets.UTF_8), Update.class);
        assertTrue(message instanceof MessageCreatedUpdate);

        assertNotNull(((MessageCreatedUpdate) message).getMessage().getBody());
    }*/

    private static class NotSerializableClass {
        private final NotSerializableClass self = this;

        @Override
        public String toString() {
            return self.getClass().getName();
        }
    }
}