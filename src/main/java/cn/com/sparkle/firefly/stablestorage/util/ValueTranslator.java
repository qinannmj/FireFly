package cn.com.sparkle.firefly.stablestorage.util;

import cn.com.sparkle.firefly.model.Value;
import cn.com.sparkle.firefly.model.Value.ValueType;
import cn.com.sparkle.firefly.stablestorage.model.StoreModel;

import com.google.protobuf.ByteString;

public class ValueTranslator {
	public static StoreModel.Value.Builder toStoreModelValue(Value value) {
		if (value == null) {
			return null;
		}
		StoreModel.Value.Builder b = StoreModel.Value.newBuilder();
		b.setType(value.getValueType().getValue());
		b.setValues(ByteString.copyFrom(value.getValuebytes(), 0, value.length()));
		return b;
	}

	public static Value toValue(StoreModel.ValueOrBuilder svalue) {
		Value value = null;
		if (svalue != null) {
			byte[] bytes = svalue.getValues().toByteArray();
			value = new Value(ValueType.getValueType(svalue.getType()), bytes, bytes.length);
		}
		return value;
	}
}
