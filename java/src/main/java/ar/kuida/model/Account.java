// Generado por scripts/generate.mjs a partir de openapi/kuida-v1.json. No editar a mano.
package ar.kuida.model;

import ar.kuida.KuidaObject;
import com.google.gson.annotations.SerializedName;
import java.util.List;

/** La organización dueña de la clave de API. */
public class Account extends KuidaObject {
  @SerializedName("id")
  private String id;

  @SerializedName("object")
  private String object;

  @SerializedName("name")
  private String name;

  @SerializedName("type")
  private String type;

  @SerializedName("timeZone")
  private String timeZone;

  @SerializedName("serviceLines")
  private List<String> serviceLines;

  @SerializedName("livemode")
  private Boolean livemode;

  @SerializedName("apiKey")
  private ApiKey apiKey;

  /** Constructor vacío: los objetos los arma el SDK a partir de las respuestas. */
  public Account() {}

  /** Id de la organización. */
  public String getId() {
    return id;
  }

  /**
   * Campo <code>object</code>.
   *
   * <p>Siempre <code>account</code>.
   */
  public String getObject() {
    return object;
  }

  /** Campo <code>name</code>. */
  public String getName() {
    return name;
  }

  /** Tipo de institución (clinic, pharmacy, lab, …). */
  public String getType() {
    return type;
  }

  /** Campo <code>timeZone</code>. */
  public String getTimeZone() {
    return timeZone;
  }

  /**
   * Líneas de servicio contratadas: seguimiento de pacientes y red de derivaciones.
   *
   * <p>Valores posibles: <code>follow_up</code>, <code>network</code>.
   */
  public List<String> getServiceLines() {
    return serviceLines;
  }

  /** <code>true</code> en producción, <code>false</code> en el entorno de pruebas. */
  public Boolean getLivemode() {
    return livemode;
  }

  /** Campo <code>apiKey</code>. */
  public ApiKey getApiKey() {
    return apiKey;
  }

  /** Objeto anidado <code>ApiKey</code>. */
  public static class ApiKey extends KuidaObject {
    @SerializedName("name")
    private String name;

    @SerializedName("prefix")
    private String prefix;

    @SerializedName("scopes")
    private List<String> scopes;

    /** Constructor vacío: los objetos los arma el SDK a partir de las respuestas. */
    public ApiKey() {}

    /** Campo <code>name</code>. */
    public String getName() {
      return name;
    }

    /** Campo <code>prefix</code>. */
    public String getPrefix() {
      return prefix;
    }

    /** Campo <code>scopes</code>. */
    public List<String> getScopes() {
      return scopes;
    }
  }
}
