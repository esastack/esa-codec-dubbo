package io.esastack.codec.serialization.fastjson;

import io.esastack.codec.serialization.api.SerializeConstants;
import io.esastack.codec.serialization.constant.TestConstant;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static io.esastack.codec.serialization.constant.TestConstant.*;
import static org.junit.Assert.assertEquals;

public class FastJsonDataInputStreamTest {
    @Test
    public void test() throws IOException, ClassNotFoundException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        FastJsonSerialization fastJsonSerialization = new FastJsonSerialization();

        assertEquals(SerializeConstants.FASTJSON_SERIALIZATION_ID, fastJsonSerialization.getSeriTypeId());
        assertEquals("x-application/fastjson", fastJsonSerialization.getContentType());
        assertEquals("fastjson", fastJsonSerialization.getSeriName());

        FastJsonDataOutputStream serialize =
                (FastJsonDataOutputStream) fastJsonSerialization.serialize(byteArrayOutputStream);
        serialize.writeByte(WRITE_BYTE);
        serialize.writeBytes(WRITE_BYTES);
        serialize.writeInt(WRITE_INT);
        serialize.writeObject(WRITE_OBJECT);
        serialize.writeUTF(WRITE_UTF);
        serialize.flush();
        byte[] sourceBytes = byteArrayOutputStream.toByteArray();
        serialize.close();
        FastJsonDataInputStream deserialize =
                (FastJsonDataInputStream) fastJsonSerialization.deserialize(new ByteArrayInputStream(sourceBytes));
        assertEquals(WRITE_BYTE, deserialize.readByte());
        assertEquals(WRITE_BYTES[0], deserialize.readBytes()[0]);
        assertEquals(WRITE_INT, deserialize.readInt());
        assertEquals(WRITE_OBJECT, deserialize.readObject(TestConstant.User.class));
        assertEquals(WRITE_UTF, deserialize.readUTF());
        deserialize.close();
    }
}
