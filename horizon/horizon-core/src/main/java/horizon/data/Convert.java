/* Copyright (c) 2020 Emjay Khan. All rights reserved. */

package horizon.data;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;

import horizon.base.Assert;

/**Utility for data conversion.
 */
public class Convert {
	private Convert() {}

	/**Converts s to klass's object.
	 * @param klass a class
	 * @param s a string
	 * @return the klass's object
	 */
	public static final Object toObject(Class<?> klass, String s) {
		if (Assert.isEmpty(s)) return null;
		if (String.class.equals(Assert.notEmpty(klass, "klass"))) return s;

		s = s.trim();
		if (isNumber(klass)) {
			s = s.replace(",", "");
			if (BigDecimal.class.equals(klass)) return new BigDecimal(s);
			else if (Double.class.equals(klass)) return Double.valueOf(s);
			else if (Float.class.equals(klass)) return Float.valueOf(s);
			else if (BigInteger.class.equals(klass)) return new BigInteger(s);
			else if (Integer.class.equals(klass)) return Integer.valueOf(s);
			else if (Long.class.equals(klass)) return Long.valueOf(s);
			else if (Short.class.equals(klass)) return Short.valueOf(s);
			else if (Byte.class.equals(klass)) return Byte.valueOf(s);
		} else {
			if (Boolean.class.equals(klass)) return Boolean.valueOf(s);
			else if (Character.class.equals(klass)) return Character.valueOf(s.charAt(0));
/*
			if (Clob.class.isAssignableFrom(klass)) return s;
			else if (Date.class.equals(klass)) return Date.valueOf(s);
			else if (Time.class.equals(klass)) return Time.valueOf(s);
			else if (Timestamp.class.equals(klass)) return Timestamp.valueOf(s);
*/
		}
		throw inconvertible(s, klass);
	}

	/**Returns whether klass is a Number class.
	 * @param klass a class
	 * @return
	 * <ul><li>true if klass is a Number class</li>
	 * 	   <li>false otherwise</li>
	 * </ul>
	 */
	public static final boolean isNumber(Class<?> klass) {
		return Number.class.isAssignableFrom(klass);
	}

	/**Returns a String converted from obj.
	 * @param obj an Object
	 * @return String converted from obj
	 */
	public static final String toString(Object obj) {
		if (Assert.isEmpty(obj))
			return "";
		if (obj instanceof String)
			return String.class.cast(obj);
		else
			return obj.toString();
	}

	/**Returns a Number converted from obj.
	 * @param obj an Object
	 * @return
	 * <ul><li>Number converted from obj</li>
	 * 	   <li>Integer(0) if obj is empty</li>
	 * </ul>
	 */
	public static final Number toNumber(Object obj) {
		if (Assert.isEmpty(obj))
			return Integer.valueOf(0);
		if (obj instanceof Number)
			return Number.class.cast(obj);
		if (obj instanceof String) {
			String s = (String)obj;
			return Double.valueOf(s.replace(",", "").trim());
		}
		throw inconvertible(obj, Number.class);
	}

	/**Returns a short value converted from obj.
	 * Convenience method for {@link #toNumber(Object)}.shortValue();
	 * @param obj an Object
	 * @return
	 * <ul><li>a short value converted from obj</li>
	 * 	   <li>0 if obj is empty</li>
	 * </ul>
	 */
	public static final short toShort(Object obj) {
		return toNumber(obj).shortValue();
	}

	/**Returns an int value converted from obj.
	 * Convenience method for {@link #toNumber(Object)}.intValue();
	 * @param obj an Object
	 * @return
	 * <ul><li>an int value converted from obj</li>
	 * 	   <li>0 if obj is empty</li>
	 * </ul>
	 */
	public static final int toInt(Object obj) {
		return toNumber(obj).intValue();
	}

	/**Returns a long value converted from obj.
	 * Convenience method for {@link #toNumber(Object)}.longValue();
	 * @param obj an Object
	 * @return
	 * <ul><li>a long value converted from obj</li>
	 * 	   <li>0 if obj is empty</li>
	 * </ul>
	 */
	public static final long toLong(Object obj) {
		return toNumber(obj).longValue();
	}

	/**Returns a double value converted from obj.
	 * Convenience method for {@link #toNumber(Object)}.doubleValue();
	 * @param obj an Object
	 * @return
	 * <ul><li>a double value converted from obj</li>
	 * 	   <li>0 if obj is empty</li>
	 * </ul>
	 */
	public static final double toDouble(Object obj) {
		return toNumber(obj).doubleValue();
	}

	/**Returns a float value converted from obj.
	 * Convenience method for {@link #toNumber(Object)}.floatValue();
	 * @param obj an Object
	 * @return
	 * <ul><li>a float value converted from obj</li>
	 * 	   <li>0 if obj is empty</li>
	 * </ul>
	 */
	public static final float toFloat(Object obj) {
		return toNumber(obj).floatValue();
	}

	/**Returns a byte value converted from obj.
	 * Convenience method for {@link #toNumber(Object)}.byteValue();
	 * @param obj an Object
	 * @return
	 * <ul><li>a byte value converted from obj</li>
	 * 	   <li>0 if obj is empty</li>
	 * </ul>
	 */
	public static final byte toByte(Object obj) {
		return toNumber(obj).byteValue();
	}

	/**Returns a boolean value converted from obj.
	 * @param obj an Object
	 * @return
	 * <ul><li>boolean value converted from obj</li>
	 * 	   <li>false if obj is empty</li>
	 * </ul>
	 */
	public static final boolean toBoolean(Object obj) {
		if (Assert.isEmpty(obj))
			return false;
		Boolean bool = obj instanceof Boolean ? Boolean.class.cast(obj) : Boolean.valueOf(obj.toString());
		return bool.booleanValue();
	}

	private static final HashMap<Class<?>, Object> ifnull = new HashMap<>();
	static {
		ifnull.put(char.class, Character.valueOf(' '));
		ifnull.put(byte.class, Byte.valueOf("0"));
		ifnull.put(short.class, Short.valueOf("0"));
		ifnull.put(int.class, Integer.valueOf(0));
		ifnull.put(long.class, Long.valueOf(0));
		ifnull.put(float.class, Float.valueOf(0));
		ifnull.put(double.class, Double.valueOf(0));
		ifnull.put(boolean.class, Boolean.FALSE);
	}
	/**Returns the primitive default value of the klass.
	 * @param klass wrapper class of a primitive type
	 * @return
	 * <ul><li>primitive default value of the klass</li>
	 * 	   <li>null if the klass is not a wrapper of a primitive type</li>
	 * </ul>
	 */
	public static final Object primitiveDefault(Class<?> klass) {
		return klass != null && klass.isPrimitive() ? ifnull.get(klass) : null;
	}

	/**Returns the string whose \r, \n, \t, and double quotation characters are escaped.
	 * @param s a String
	 * @return string whose \r, \n, \t, and double quotation characters are escaped
	 */
	public static final String rntq(String s) {
		return Assert.isEmpty(s) ? "" :
			s.replace("\\","\\\\")
			 .replace("\r","\\r")
			 .replace("\n","\\n")
			 .replace("\t","\\t")
			 .replace("\"","\\\"");
	}

	private static RuntimeException inconvertible(Object obj, Class<?> klass) {
		return new RuntimeException("Inconvertible: " + obj + " to " + klass);
	}

	/**Interface to {@link Convert}.
	 * <p>All methods of this interface have default implementation, which delegate operations to corresponding methods of the Convert class.<br />
	 * The intention is to help use the Convert operations easily by implementing this interface.
	 * </p>
	 */
	public static interface Support {
		/**See {@link Convert#toObject(Class, String)}.*/
		default Object toObject(Class<?> klass, String s) {
			return Convert.toObject(klass, s);
		}

		/**See {@link Convert#isNumber(Class)}.*/
		default boolean isNumber(Class<?> klass) {
			return Convert.isNumber(klass);
		}

		/**See {@link Convert#toString(Object)}.*/
		default String toString(Object obj) {
			return Convert.toString(obj);
		}

		/**See {@link Convert#toNumber(Object)}.*/
		default Number toNumber(Object obj) {
			return Convert.toNumber(obj);
		}

		/**See {@link Convert#toShort(Object)}.*/
		default short toShort(Object obj) {
			return Convert.toShort(obj);
		}

		/**See {@link Convert#toInt(Object)}.*/
		default int toInt(Object obj) {
			return Convert.toInt(obj);
		}

		/**See {@link Convert#toLong(Object)}.*/
		default long toLong(Object obj) {
			return Convert.toLong(obj);
		}

		/**See {@link Convert#toDouble(Object)}.*/
		default double toDouble(Object obj) {
			return Convert.toDouble(obj);
		}

		/**See {@link Convert#toFloat(Object)}.*/
		default float toFloat(Object obj) {
			return Convert.toFloat(obj);
		}

		/**See {@link Convert#toByte(Object)}.*/
		default byte toByte(Object obj) {
			return Convert.toByte(obj);
		}

		/**See {@link Convert#toBoolean(Object)}.*/
		default boolean toBoolean(Object obj) {
			return Convert.toBoolean(obj);
		}

		/**See {@link Convert#primitiveDefault(Class)}.*/
		default Object primitiveDefault(Class<?> klass) {
			return Convert.primitiveDefault(klass);
		}

		/**See {@link Convert#rntq(String)}.*/
		default String rntq(String s) {
			return Convert.rntq(s);
		}
	}
}