package gamemap.icon;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.FocusEvent.Cause;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;

import javax.swing.JComponent;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

public class Utils {
	public static Field getField(String field, Object obj) throws ReflectiveOperationException {
		int dot = field.indexOf('.');
		if(dot != -1) field = field.substring(0, dot);
		return obj.getClass().getDeclaredField(field);
	}
	
	public static Object getObjectMember(String field, Object obj) {
		try {
			Field f = getField(field, obj);
			Object val = f.get(obj);
			int dot = field.indexOf('.');
			if(dot == -1) return val;
			return getObjectMember(field.substring(dot + 1), val);
		} catch(ReflectiveOperationException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public static void setObjectMember(String field, Object obj, Object value) {
		try {
			Field f = getField(field, obj);
			int dot = field.indexOf('.');
			if(dot == -1) {
				f.set(obj, value);
			} else {
				obj = f.get(obj);
				setObjectMember(field.substring(dot + 1), obj, value);
			}
		} catch(ReflectiveOperationException e) {
			e.printStackTrace();
		}
	}
	
	public static JComponent restrictHeight(JComponent comp) {
		comp.setMaximumSize(new Dimension(Integer.MAX_VALUE,
				comp.getPreferredSize().height));
		return comp;
	}
	
	public static String toTitleCase(String str) {
		return Character.toUpperCase(str.charAt(0)) + str.substring(1);
	}
	
	public static Color hexToColor(String hexString) {
		int val = Integer.parseInt(hexString.substring(1));
		return new Color(val, true);
	}
	
	public static String colorToHex(Color color) {
		return '#' + Integer.toString(color.getRGB() & 0xFFFFFF, 16).toUpperCase();
	}
	
	public static final TypeAdapter<File> FILE_ADAPTER = new TypeAdapter<File>() {
		@Override
		public File read(JsonReader in) throws IOException {
			if(in.peek() == JsonToken.NULL) {
				in.nextNull();
				return null;
			}
			String nextString = in.nextString();
			return "null".equals(nextString) ? null : new File(nextString);
		}

		@Override
		public void write(JsonWriter out, File value) throws IOException {
			out.value(value == null ? null : value.getAbsolutePath());
		}
	};
	
	public static Gson getGson() {
		return (new GsonBuilder()).registerTypeAdapter(File.class,
				Utils.FILE_ADAPTER).create();
	}
	
	public static boolean isCauseTraversal(Cause cause) {
		return cause.name().startsWith("TRAVERSAL");
	}
}