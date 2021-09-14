package io.esastack.codec.serialization.json;

import io.esastack.codec.serialization.api.SerializeConstants;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static io.esastack.codec.serialization.constant.TestConstant.*;
import static org.junit.Assert.assertEquals;

public class JsonSerializationTest {
    @Test
    public void test() throws IOException, ClassNotFoundException, NoSuchFieldException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        JsonSerialization jsonSerialization = new JsonSerialization();

        assertEquals(SerializeConstants.JSON_SERIALIZATION_ID, jsonSerialization.getSeriTypeId());
        assertEquals("x-application/json", jsonSerialization.getContentType());
        assertEquals("json", jsonSerialization.getSeriName());

        JsonDataOutputStream serialize =
                (JsonDataOutputStream) jsonSerialization.serialize(byteArrayOutputStream);
        serialize.writeByte(WRITE_BYTE);
        serialize.writeBytes(WRITE_BYTES);
        serialize.writeInt(WRITE_INT);
        serialize.writeObject(WRITE_OBJECT);
        serialize.writeObject(WRITE_OBJECT);
        serialize.writeObject(WRITE_OBJECT);
        serialize.writeUTF(WRITE_UTF);
        serialize.flush();
        byte[] sourceBytes = byteArrayOutputStream.toByteArray();
        serialize.close();
        JsonDataInputStream deserialize =
                (JsonDataInputStream) jsonSerialization.deserialize(new ByteArrayInputStream(sourceBytes));
        assertEquals(WRITE_BYTE, deserialize.readByte());
        assertEquals(WRITE_BYTES[0], deserialize.readBytes()[0]);
        assertEquals(WRITE_INT, deserialize.readInt());
        assertEquals(WRITE_OBJECT, deserialize.readObject(User.class));
        assertEquals(WRITE_OBJECT, deserialize.readObject(User.class, null));
        assertEquals(WRITE_OBJECT, deserialize.readObject(String.class, SubUser.class.getGenericSuperclass()));
        assertEquals(WRITE_UTF, deserialize.readUTF());
        deserialize.close();
    }
}
