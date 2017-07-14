package thrift;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.MissingNode;
import com.fasterxml.jackson.databind.node.NullNode;

import thrift.def.TDataEncodingType;
import utils.ApiUtils;
import utils.AppConstants;

/**
 * Thrift API utility class.
 * 
 * @author Thanh Nguyen <btnguyen2k@gmail.com>
 * @since template-v0.1.4
 */
public class ThriftApiUtils {

    /**
     * Encode data from JSON to byte array.
     * 
     * @param dataType
     * @param jsonNode
     * @return
     */
    public static byte[] encodeFromJson(TDataEncodingType dataType, JsonNode jsonNode) {
        byte[] data = jsonNode == null || jsonNode instanceof NullNode
                || jsonNode instanceof MissingNode ? null
                        : jsonNode.toString().getBytes(AppConstants.UTF8);
        if (data == null) {
            return null;
        }
        if (dataType == null) {
            dataType = TDataEncodingType.JSON_STRING;
        }
        try {
            switch (dataType) {
            case JSON_STRING:
                return data;
            case JSON_GZIP:
                return ApiUtils.toGzip(data);
            default:
                throw new IllegalArgumentException("Unsupported data encoding type: " + dataType);
            }
        } catch (Exception e) {
            throw e instanceof RuntimeException ? (RuntimeException) e : new RuntimeException(e);
        }
    }

    /**
     * Decode data from a byte array to json.
     * 
     * @param dataType
     * @param data
     * @return
     */
    public static JsonNode decodeToJson(TDataEncodingType dataType, byte[] data) {
        if (data == null) {
            return null;
        }
        if (dataType == null) {
            dataType = TDataEncodingType.JSON_STRING;
        }
        try {
            switch (dataType) {
            case JSON_STRING:
                return ApiUtils.fromJsonString(data);
            case JSON_GZIP:
                return ApiUtils.fromJsonGzip(data);
            default:
                throw new IllegalArgumentException("Unsupported data encoding type: " + dataType);
            }
        } catch (Exception e) {
            throw e instanceof RuntimeException ? (RuntimeException) e : new RuntimeException(e);
        }
    }

}
