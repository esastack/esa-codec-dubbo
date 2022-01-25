package io.esastack.codec.serialization.fastjson;

import io.esastack.codec.serialization.api.DataInputStream;
import io.esastack.codec.serialization.api.DataOutputStream;
import io.esastack.codec.serialization.api.SerializeConstants;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class FastJsonSerializationTest {
    @Test
    public void testFastJson() throws IOException, ClassNotFoundException {
        FastJsonSerialization serialization = new FastJsonSerialization();
        assertEquals(SerializeConstants.FASTJSON_SERIALIZATION_ID, serialization.getSeriTypeId());
        assertEquals("fastjson", serialization.getSeriName());
        assertEquals("x-application/fastjson", serialization.getContentType());

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        DataOutputStream serialize = serialization.serialize(byteArrayOutputStream);
        serialize.writeByte((byte) 0);
        serialize.writeInt(0);
        serialize.writeUTF("0");
        serialize.writeObject("0");
        serialize.writeObject("0");
        serialize.writeBytes(new byte[]{97, 98});
        serialize.flush();

        byte[] bytes = byteArrayOutputStream.toByteArray();
        DataInputStream deserialize = serialization.deserialize(new ByteArrayInputStream(bytes));
        assertEquals((byte) 0, deserialize.readByte());
        assertEquals(0, deserialize.readInt());
        assertEquals("0", deserialize.readUTF());
        assertEquals("0", deserialize.readObject(String.class));
        assertEquals("0", deserialize.readObject(String.class, String.class));
        assertEquals(2, deserialize.readBytes().length);

        serialize.close();
        deserialize.close();
    }
}
