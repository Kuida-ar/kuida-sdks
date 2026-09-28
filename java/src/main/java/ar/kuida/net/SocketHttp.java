package ar.kuida.net;

import ar.kuida.KuidaResponse;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

/**
 * Cliente HTTP/1.1 mínimo sobre un socket, para los métodos que {@code HttpURLConnection} no
 * acepta. Una conexión por pedido ({@code Connection: close}); entiende {@code Content-Length},
 * {@code Transfer-Encoding: chunked} y cierre de conexión.
 */
final class SocketHttp {
  private SocketHttp() {}

  static KuidaResponse execute(HttpRequest request) throws IOException {
    URL url = new URL(request.getUrl());
    String scheme = url.getProtocol().toLowerCase(Locale.ROOT);
    boolean tls = "https".equals(scheme);
    if (!tls && !"http".equals(scheme)) throw new IOException("Esquema no soportado: " + scheme);
    String host = url.getHost();
    int port = url.getPort() == -1 ? url.getDefaultPort() : url.getPort();
    int timeout = request.getTimeoutMillis();

    Socket socket = new Socket();
    try {
      socket.connect(new InetSocketAddress(host, port), timeout);
      socket.setSoTimeout(timeout);
      if (tls) {
        SSLSocket ssl = (SSLSocket) ((SSLSocketFactory) SSLSocketFactory.getDefault()).createSocket(socket, host, port, true);
        SSLParameters params = ssl.getSSLParameters();
        params.setEndpointIdentificationAlgorithm("HTTPS");
        ssl.setSSLParameters(params);
        ssl.startHandshake();
        socket = ssl;
      }
      writeRequest(socket.getOutputStream(), request, url, host, port, tls);
      return readResponse(new BufferedInputStream(socket.getInputStream()));
    } finally {
      try {
        socket.close();
      } catch (IOException ignored) {
        // nada que hacer
      }
    }
  }

  private static void writeRequest(OutputStream out, HttpRequest request, URL url, String host, int port, boolean tls)
      throws IOException {
    String target = url.getFile().isEmpty() ? "/" : url.getFile();
    boolean defaultPort = (tls && port == 443) || (!tls && port == 80);
    StringBuilder head = new StringBuilder();
    head.append(request.getMethod()).append(' ').append(target).append(" HTTP/1.1\r\n");
    head.append("Host: ").append(host).append(defaultPort ? "" : ":" + port).append("\r\n");
    for (Map.Entry<String, String> h : request.getHeaders().entrySet()) {
      checkHeader(h.getKey());
      checkHeader(h.getValue());
      head.append(h.getKey()).append(": ").append(h.getValue()).append("\r\n");
    }
    byte[] body = request.getBody();
    head.append("Content-Length: ").append(body == null ? 0 : body.length).append("\r\n");
    head.append("Connection: close\r\n\r\n");
    out.write(head.toString().getBytes(UrlConnectionTransport.UTF_8));
    if (body != null) out.write(body);
    out.flush();
  }

  private static void checkHeader(String s) {
    if (s.indexOf('\r') >= 0 || s.indexOf('\n') >= 0) throw new IllegalArgumentException("Header con salto de línea");
  }

  private static KuidaResponse readResponse(InputStream in) throws IOException {
    String statusLine = readLine(in);
    String[] parts = statusLine.split(" ", 3);
    if (parts.length < 2 || !parts[0].startsWith("HTTP/")) throw new IOException("Respuesta HTTP inválida: " + statusLine);
    int status;
    try {
      status = Integer.parseInt(parts[1]);
    } catch (NumberFormatException e) {
      throw new IOException("Status HTTP inválido: " + statusLine);
    }
    Map<String, List<String>> headers = new LinkedHashMap<String, List<String>>();
    String line;
    while (!(line = readLine(in)).isEmpty()) {
      int colon = line.indexOf(':');
      if (colon <= 0) continue;
      String name = line.substring(0, colon).trim();
      String value = line.substring(colon + 1).trim();
      List<String> values = null;
      for (Map.Entry<String, List<String>> e : headers.entrySet()) {
        if (e.getKey().equalsIgnoreCase(name)) values = e.getValue();
      }
      if (values == null) {
        values = new ArrayList<String>();
        headers.put(name, values);
      }
      values.add(value);
    }
    String transferEncoding = first(headers, "Transfer-Encoding");
    String contentLength = first(headers, "Content-Length");
    byte[] body;
    if (status == 204 || status == 304 || (status >= 100 && status < 200)) {
      body = new byte[0];
    } else if (transferEncoding != null && transferEncoding.toLowerCase(Locale.ROOT).contains("chunked")) {
      body = readChunked(in);
    } else if (contentLength != null) {
      body = readFixed(in, Integer.parseInt(contentLength.trim()));
    } else {
      body = readToEnd(in);
    }
    return new KuidaResponse(status, headers, new String(body, UrlConnectionTransport.UTF_8));
  }

  private static String first(Map<String, List<String>> headers, String name) {
    for (Map.Entry<String, List<String>> e : headers.entrySet()) {
      if (e.getKey().equalsIgnoreCase(name) && !e.getValue().isEmpty()) return e.getValue().get(0);
    }
    return null;
  }

  private static String readLine(InputStream in) throws IOException {
    ByteArrayOutputStream buf = new ByteArrayOutputStream();
    int b;
    while ((b = in.read()) != -1) {
      if (b == '\n') break;
      if (b != '\r') buf.write(b);
    }
    if (b == -1 && buf.size() == 0) throw new EOFException("La conexión se cerró antes de la respuesta");
    return new String(buf.toByteArray(), UrlConnectionTransport.UTF_8);
  }

  private static byte[] readFixed(InputStream in, int length) throws IOException {
    byte[] out = new byte[length];
    int off = 0;
    while (off < length) {
      int n = in.read(out, off, length - off);
      if (n == -1) throw new EOFException("Cuerpo incompleto");
      off += n;
    }
    return out;
  }

  private static byte[] readChunked(InputStream in) throws IOException {
    ByteArrayOutputStream buf = new ByteArrayOutputStream();
    while (true) {
      String sizeLine = readLine(in);
      int semi = sizeLine.indexOf(';');
      int size = Integer.parseInt((semi >= 0 ? sizeLine.substring(0, semi) : sizeLine).trim(), 16);
      if (size == 0) {
        while (!readLine(in).isEmpty()) {
          // trailers
        }
        return buf.toByteArray();
      }
      buf.write(readFixed(in, size));
      readLine(in);
    }
  }

  private static byte[] readToEnd(InputStream in) throws IOException {
    ByteArrayOutputStream buf = new ByteArrayOutputStream();
    byte[] chunk = new byte[8192];
    int n;
    while ((n = in.read(chunk)) != -1) buf.write(chunk, 0, n);
    return buf.toByteArray();
  }
}
