package ar.kuida;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.ToNumberPolicy;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAccessor;
import java.util.Date;
import java.util.Map;

/**
 * Serialización JSON del SDK (Gson). Uso interno: puede cambiar sin aviso entre versiones menores.
 */
public final class KuidaJson {
  private KuidaJson() {}

  private static final TypeAdapter<OffsetDateTime> DATE_TIME = new TypeAdapter<OffsetDateTime>() {
    @Override
    public void write(JsonWriter out, OffsetDateTime value) throws IOException {
      if (value == null) out.nullValue();
      else out.value(value.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
    }

    @Override
    public OffsetDateTime read(JsonReader in) throws IOException {
      if (in.peek() == JsonToken.NULL) {
        in.nextNull();
        return null;
      }
      String s = in.nextString();
      try {
        return OffsetDateTime.parse(s);
      } catch (DateTimeParseException e) {
        return null; // el texto original sigue en getRawJson()
      }
    }
  };

  /** Guarda el JSON crudo en cada {@link KuidaObject}, incluidos los anidados. */
  private static final TypeAdapterFactory RAW_JSON = new TypeAdapterFactory() {
    @Override
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
      if (!KuidaObject.class.isAssignableFrom(type.getRawType())) return null;
      final TypeAdapter<T> delegate = gson.getDelegateAdapter(this, type);
      final TypeAdapter<JsonElement> elements = gson.getAdapter(JsonElement.class);
      return new TypeAdapter<T>() {
        @Override
        public void write(JsonWriter out, T value) throws IOException {
          JsonObject raw = value == null ? null : ((KuidaObject) value).getRawJson();
          if (raw != null) elements.write(out, raw);
          else delegate.write(out, value);
        }

        @Override
        public T read(JsonReader in) throws IOException {
          JsonElement tree = elements.read(in);
          if (tree == null || tree.isJsonNull()) return null;
          T value = delegate.fromJsonTree(tree);
          if (value != null && tree.isJsonObject()) ((KuidaObject) value).setRawJson(tree.getAsJsonObject());
          return value;
        }
      };
    }
  };

  private static final Gson GSON = new GsonBuilder()
      .registerTypeAdapter(OffsetDateTime.class, DATE_TIME.nullSafe())
      .registerTypeAdapterFactory(RAW_JSON)
      .setObjectToNumberStrategy(ToNumberPolicy.LONG_OR_DOUBLE)
      .disableHtmlEscaping()
      .create();

  /** La instancia de Gson configurada del SDK. */
  public static Gson gson() {
    return GSON;
  }

  /** Parsea un cuerpo JSON en el tipo pedido y le adjunta la respuesta HTTP. */
  public static <T> T parse(JsonElement tree, Type type, KuidaResponse response) {
    T value = GSON.fromJson(tree, type);
    if (value instanceof KuidaObject) {
      KuidaObject.attach((KuidaObject) value, tree.isJsonObject() ? tree.getAsJsonObject() : null, response);
    }
    return value;
  }

  /**
   * Convierte un valor de entrada (parámetros, referencias, mapas, listas, fechas) en JSON.
   * Las claves de los mapas viajan tal cual, sin conversión.
   */
  public static JsonElement encode(Object value) {
    if (value == null) return JsonNull.INSTANCE;
    if (value instanceof JsonElement) return (JsonElement) value;
    if (value instanceof JsonEncodable) return encode(((JsonEncodable) value).toJsonValue());
    if (value instanceof String) return new JsonPrimitive((String) value);
    if (value instanceof Boolean) return new JsonPrimitive((Boolean) value);
    if (value instanceof Number) return new JsonPrimitive((Number) value);
    if (value instanceof Character) return new JsonPrimitive(value.toString());
    if (value instanceof Map) {
      JsonObject obj = new JsonObject();
      for (Map.Entry<?, ?> e : ((Map<?, ?>) value).entrySet()) {
        if (e.getValue() == null) continue;
        obj.add(String.valueOf(e.getKey()), encode(e.getValue()));
      }
      return obj;
    }
    if (value instanceof Iterable) {
      JsonArray arr = new JsonArray();
      for (Object o : (Iterable<?>) value) arr.add(encode(o));
      return arr;
    }
    if (value instanceof Object[]) {
      JsonArray arr = new JsonArray();
      for (Object o : (Object[]) value) arr.add(encode(o));
      return arr;
    }
    String temporal = formatTemporal(value);
    if (temporal != null) return new JsonPrimitive(temporal);
    if (value instanceof Enum) return new JsonPrimitive(((Enum<?>) value).name());
    return GSON.toJsonTree(value);
  }

  /** Valor de un query param: booleanos {@code "true"}/{@code "false"}, fechas ISO 8601. */
  public static String queryValue(Object value) {
    if (value instanceof JsonEncodable) return queryValue(((JsonEncodable) value).toJsonValue());
    String temporal = formatTemporal(value);
    if (temporal != null) return temporal;
    return String.valueOf(value);
  }

  private static String formatTemporal(Object value) {
    if (value instanceof OffsetDateTime) return ((OffsetDateTime) value).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    if (value instanceof ZonedDateTime) return ((ZonedDateTime) value).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    if (value instanceof Instant) return value.toString();
    if (value instanceof LocalDate) return value.toString();
    if (value instanceof Date) return ((Date) value).toInstant().toString();
    if (value instanceof TemporalAccessor) return value.toString();
    return null;
  }
}
