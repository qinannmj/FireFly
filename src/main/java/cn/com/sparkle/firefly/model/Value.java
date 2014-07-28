package cn.com.sparkle.firefly.model;

import java.util.LinkedList;

import cn.com.sparkle.firefly.addprocess.AddRequestPackage;

public class Value {
	private ValueType valueType;
	private byte[][] value;

	private LinkedList<AddRequestPackage> addPackage = null; //just for transport message by pipe in jvm

	public byte[][] getValue() {
		return value;
	}

	public Value(ValueType valueType, byte[][] value) {
		super();
		this.valueType = valueType;
		this.value = value;
	}

	public static enum ValueType {
		ADMIN(0), COMM(1);
		private int value;

		private ValueType(int value) {
			this.value = value;
		}

		public int getValue() {
			return this.value;
		}

		public static ValueType getValueType(int type) {
			switch (type) {
			case 0:
				return ADMIN;
			case 1:
				return COMM;
			default:
				throw new RuntimeException("unsupported type:" + type);
			}
		}
	}

	public ValueType getValueType() {
		return valueType;
	}

	public LinkedList<AddRequestPackage> getAddPackage() {
		return addPackage;
	}

	public void setAddPackage(LinkedList<AddRequestPackage> addPackage) {
		this.addPackage = addPackage;
	}

}
