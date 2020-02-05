
package com.testclient;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.methods.*;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.conn.ssl.SSLContexts;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;

import javax.net.ssl.SSLContext;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.List;
import java.lang.String;
import java.util.zip.GZIPOutputStream;

public class HttpClient4Check {

    private static String clntType = KeyStore.getDefaultType();
    private static String clntPath = "src/main/resources/esaccess.jks";
    private static String clntPass = "changeit";

    public static SSLConnectionSocketFactory createSslCustomContext() throws KeyStoreException, IOException, NoSuchAlgorithmException,
            CertificateException, KeyManagementException, UnrecoverableKeyException {

        KeyStore cks = KeyStore.getInstance(clntType);
        cks.load(new FileInputStream(clntPath), clntPass.toCharArray());

        SSLContext sslContext = SSLContexts.custom()
                .useTLS()
                .loadKeyMaterial(cks, clntPass.toCharArray()) // load client certificate
                .build();

        return new SSLConnectionSocketFactory(sslContext);
    }

    public static URI buildURI() throws URISyntaxException {
        String httpURI = "https://XXX:4443/indexname/_search";
        URIBuilder builder = new URIBuilder(httpURI);
        System.out.println("Query String : " + builder.build().toString());
        List<NameValuePair> nvp = new ArrayList<>();
        //nvp.add(new BasicNameValuePair("key1", "value1"));
        builder.addParameters(nvp);
        return builder.build();
    }

    public static byte[] compressData(String str) throws URISyntaxException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try {
            OutputStream deflater = new GZIPOutputStream(buffer);
            deflater.write(str.getBytes(StandardCharsets.UTF_8));
            deflater.close();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        return buffer.toByteArray();
    }

    public static HttpEntity buildHttpRequestEntity() throws URISyntaxException {
        ByteSequence bs = null ;
        String body = "{\"size\" : 0, \"aggregations\" : { \"uniq_type\" : { \"terms\" : { \"field\" : \"region\", \"size\": 60 } } } }";
        //InputStream is = new ByteArrayInputStream(body.getBytes());
        InputStream is = new ByteArrayInputStream(compressData(body));
        BasicHttpEntity entity = new BasicHttpEntity();
        entity.setContent(is);
        return entity ;
    }


    public static void printResponse(HttpResponse response) throws IOException {

        HttpEntity httpEntity = response.getEntity();
        InputStream is1 = httpEntity.getContent();

        BufferedReader reader = new BufferedReader(new InputStreamReader(is1, StandardCharsets.ISO_8859_1), 8);
        StringBuilder sb = new StringBuilder();

        String line = null ;
        while ((line = reader.readLine()) != null)
        {
            sb.append(line + "\n");
        }

        System.out.println("Result JSON : " + sb.toString());
        int statusCode = response.getStatusLine().getStatusCode();
        System.out.println("Response : " + response.getStatusLine().toString());
    }

    public static void main(String[] args) throws URISyntaxException, InterruptedException, IOException, UnrecoverableKeyException, CertificateException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {

        CloseableHttpClient clientSecure = HttpClients.custom().setSSLSocketFactory(createSslCustomContext()).build();
        //CloseableHttpClient client1 = HttpClients.createDefault();
        // HttpRequestBase http = null;  // For GET Method

        HttpEntityEnclosingRequestBase httpPost = null ;
        httpPost = new HttpPost(buildURI());

        httpPost.setEntity(buildHttpRequestEntity());
        httpPost.addHeader("Content-Type", "application/json");

        // Specifying content encoding
        httpPost.addHeader("Content-Encoding", "gzip");
        httpPost.addHeader("Accept-C", "gzip");

        HttpResponse response =  clientSecure.execute(httpPost);

        printResponse(response);

        //Thread.sleep(2000);

        HttpRequestBase httpRequestBase = httpPost ;
        httpRequestBase.releaseConnection();

        httpPost.releaseConnection();

    }
}


