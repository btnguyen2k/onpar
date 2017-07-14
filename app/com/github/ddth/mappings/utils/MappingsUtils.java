package com.github.ddth.mappings.utils;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

/**
 * Utility class.
 *
 * @author Thanh Nguyen <btnguyen2k@gmail.com>
 * @since 0.1.0
 */
public class MappingsUtils {
    public final static Charset UTF8 = Charset.forName("utf-8");

    public static enum CacheInvalidationType {
        CREATE(0), UPDATE(1), DELETE(2);

        private final int value;

        CacheInvalidationType(int value) {
            this.value = value;
        }
    }

    public static enum DaoActionStatus {
        ERROR(0), SUCCESSFUL(1), DUPLICATED(2), NOT_FOUND(3);

        private final int value;

        DaoActionStatus(int value) {
            this.value = value;
        }
    }

    public static class DaoResult {
        public final DaoActionStatus status;
        public final Object output;

        public DaoResult(DaoActionStatus status) {
            this.status = status;
            this.output = null;
        }

        public DaoResult(DaoActionStatus status, Object output) {
            this.status = status;
            this.output = output;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            ToStringBuilder tsb = new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE);
            tsb.append("status", status).append("output", output);
            return tsb.toString();
        }
    }

    /**
     * Simple encoding: separator.
     */
    public final static char SE_SEPARATOR = 0x01;

    /**
     * Simple encoding: encode a list of strings.
     *
     * @param inputs
     * @return
     */
    public static String seEncodeAsString(String... inputs) {
        return inputs != null ? StringUtils.join(inputs, SE_SEPARATOR) : "";
    }

    /**
     * Simple encoding: encode a list of strings.
     *
     * @param inputs
     * @return
     */
    public static byte[] seEncodeAsBytes(String... inputs) {
        return seEncodeAsString(inputs).getBytes(UTF8);
    }

    /**
     * Simple encoding: encode a list of strings.
     *
     * @param inputs
     * @return
     */
    public static ByteBuffer seEncodeAsByteBuffer(String... inputs) {
        return ByteBuffer.wrap(seEncodeAsBytes(inputs));
    }

    /**
     * Simple encoding: decode a string that was encoded by {@link #seEncodeAsString(String...)}.
     *
     * @param input
     * @return
     */
    public static String[] seDecode(String input) {
        return input != null ? StringUtils.split(input, SE_SEPARATOR)
                : ArrayUtils.EMPTY_STRING_ARRAY;
    }

    /**
     * Simple encoding: decode a byte array that was encoded by
     * {@link #seEncodeAsBytes(String...)}.
     *
     * @param input
     * @return
     */
    public static String[] seDecode(byte[] input) {
        return seDecode(input != null ? new String(input, UTF8) : null);
    }

    /**
     * Simple encoding: decode a byte array that was encoded
     * by {@link #seEncodeAsByteBuffer(String...)}.
     *
     * @param input
     * @return
     */
    public static String[] seDecode(ByteBuffer input) {
        return seDecode(input != null ? input.array() : null);
    }

}
