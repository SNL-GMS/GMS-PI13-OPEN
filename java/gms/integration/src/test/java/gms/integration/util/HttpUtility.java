package gms.integration.util;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.FileEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

public class HttpUtility {

  private static final int MAX_ERR_CHARS = 10000;

  public static Response postRequest(HttpUriRequest request) throws IOException {
    try (CloseableHttpClient client = HttpClients.createDefault();
        CloseableHttpResponse response = client.execute(request)) {

      return new Response(response.getStatusLine().getStatusCode(),
          response.getStatusLine().getReasonPhrase(),
          EntityUtils.toString(response.getEntity()));
    }
  }

  public static Response postRequest(String postRoute, String requestDirectory, String requestFile)
      throws IOException, URISyntaxException {
    String requestFilePath = String.format("gms/requests/%s/%s", requestDirectory, requestFile);

    URL fileResource = Thread.currentThread().getContextClassLoader()
        .getResource(requestFilePath);

    HttpPost request = new HttpPost(postRoute);
    request.setEntity(new FileEntity(new File(fileResource.toURI())));
    request.setHeader("Content-Type", "application/json");
    request.setHeader("Accept", "application/json");

    return postRequest(request);
  }

  /**
   * Posts JSON to an endpoint
   *
   * @param endPointURL the url to which to post, which must not be null.
   * @param json the json to post (not checked to be valid json).
   * @return a response object
   */
  public static Response postJSONToEndpoint(
      final String endPointURL, final String json) throws IOException {

    HttpPost post = new HttpPost(endPointURL);

    post.addHeader("content-type", "application/json");
    StringEntity entity = new StringEntity(json);
    post.setEntity(entity);

    Response response = postRequest(post);

    // Some error output to help with diagnostics.
    if (response.code / 100 != 2) {
      String toShow = response.entity;
      if (toShow != null && toShow.length() > MAX_ERR_CHARS) {
        toShow = toShow.substring(0, MAX_ERR_CHARS);
      }
      System.err.printf("-------- POST RESPONSE ---------\n%s\n------------------------\n",
          toShow);
    }

    return response;
  }

  public static class Response {

    public int code;
    public String reason;
    public String entity;

    public Response(int code, String reason, String entity) {
      this.code = code;
      this.reason = reason;
      this.entity = entity;
    }

    @Override
    public String toString() {
      return "Response{" +
          "code=" + code +
          ", reason='" + reason + '\'' +
          ", entity='" + entity + '\'' +
          '}';
    }
  }
}
