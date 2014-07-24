package cn.com.sparkle.paxos.stablestorage.util;

import com.google.protobuf.ByteString;

import cn.com.sparkle.paxos.model.Value;
import cn.com.sparkle.paxos.model.Value.ValueType;
import cn.com.sparkle.paxos.stablestorage.model.StoreModel;

public class ValueTranslator {
	public static StoreModel.Value.Builder toStoreModelValue(Value value) {
		if (value == null) {
			return null;
		}
		StoreModel.Value.Builder b = StoreModel.Value.newBuilder();
		b.setType(value.getValueType().getValue());
		if (value.getValue() != null) {
			for (byte[] bytes : value.getValue()) {
				b.addValues(ByteString.copyFrom(bytes));
			}
		}
		return b;
	}

	public static Value toValue(StoreModel.ValueOrBuilder svalue) {
		Value value = null;
		if (svalue != null) {
			byte[][] bytes = new byte[svalue.getValuesCount()][];
			for (int i = 0; i < bytes.length; ++i) {
				bytes[i] = svalue.getValues(i).toByteArray();
			}
			value = new Value(ValueType.getValueType(svalue.getType()), bytes);
		}
		return value;
	}
}
