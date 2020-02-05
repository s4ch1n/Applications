package com.test;

import com.test.utils.ByteSequence;
import com.test.utils.BytesArray;
import com.test.utils.DelegatedProtocol;
import org.apache.commons.httpclient.*;
import org.apache.commons.httpclient.methods.ByteArrayRequestEntity;
import org.apache.commons.httpclient.methods.EntityEnclosingMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.params.HttpClientParams;
import org.apache.commons.httpclient.params.HttpConnectionManagerParams;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.apache.commons.httpclient.protocol.Protocol;
import org.apache.commons.httpclient.protocol.SSLProtocolSocketFactory;
import org.apache.commons.httpclient.protocol.SecureProtocolSocketFactory;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContexts;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.cert.CertificateException;
import java.util.zip.GZIPOutputStream;

public class HttpClient3Check {

    protected HttpConnection conn;

    private static String clntType = KeyStore.getDefaultType();
    private static String clntPath = "file:///Users/s4ch1n/GitHub/tmp/hdfsclienttest/src/main/resources/esaccess.jks";
    private static String clntPass = "changeit";

    public static SecureProtocolSocketFactory createSslCustomContext() throws KeyStoreException, IOException, NoSuchAlgorithmException,
            CertificateException, KeyManagementException, UnrecoverableKeyException {

        KeyStore cks = KeyStore.getInstance(clntType);

        cks.load(new FileInputStream(clntPath), clntPass.toCharArray());

        KeyManagerFactory kmFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        //char[] pass = (StringUtils.hasText(keyStorePass) ? keyStorePass.trim().toCharArray() : null);
        kmFactory.init(cks, null);

        SSLContext ctx = SSLContext.getInstance("TLS");
        ctx.init(kmFactory.getKeyManagers(), null, null);

        SecureProtocolSocketFactory sslFactory = new SSLSocketFactory(clntPath,clntPass);
        return sslFactory;

    }

    public static HttpClientParams buildHttpClientParams() {
        HttpClientParams clientParams = new HttpClientParams();
        clientParams.setConnectionManagerTimeout(0);
        //clientParams.setSoTimeout((int) settings.getHttpTimeout());

        clientParams.setCredentialCharset(Charset.forName("UTF-8").name());
        clientParams.setContentCharset(Charset.forName("UTF-8").name());

        return clientParams;
    }

    public static void printResponse(HttpMethod response) throws IOException {

        //HttpEntity httpEntity = response.getEntity();
        InputStream is1 = response.getResponseBodyAsStream();
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

    public static byte[] compressDataByteArray(ByteSequence ba) throws URISyntaxException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try {
            OutputStream deflater = new GZIPOutputStream(buffer);
            deflater.write(ba.toString().getBytes());
            deflater.close();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        return buffer.toByteArray();
    }


    public static void main(String[] args) throws IOException, URISyntaxException {

        HttpMethod http = null;
        http = new PostMethod();

        String httpURI = "https://XXXXX-Host:4443/indexname/_search";
        //String httpURI = "http://XXXXX-Host:4080/indexname/_search";

        http.setURI(new URI(httpURI, false));

        System.out.println("Host " + http.getURI().getHost() + " - Port : " + http.getURI().getPort());

        HostConfiguration hostConfig = new HostConfiguration();

        //HttpClient client = new HttpClient(buildHttpClientParams(), new SocketTrackingConnectionManager());
        SimpleHttpConnectionManager httpConnectionManager = new SimpleHttpConnectionManager();

        HttpClient client = new HttpClient(buildHttpClientParams(), httpConnectionManager);
        client.setHostConfiguration(hostConfig);

        String schema = "https" ;
        SecureProtocolSocketFactory protoSocketFactory = new SSLSocketFactory(clntPath,clntPass);
        Protocol authhttps = Protocol.getProtocol(schema);
        Protocol proxiedHttp = new DelegatedProtocol(protoSocketFactory, authhttps, schema, 443);
        Protocol.registerProtocol(schema,proxiedHttp);

        client.getHostConfiguration().setHost(http.getURI().getHost(), http.getURI().getPort(), authhttps);
        HttpConnectionManagerParams connectionParams = client.getHttpConnectionManager().getParams();

        //HttpMethodParams params = new HttpMethodParams();
        connectionParams.setTcpNoDelay(true);

        //http.setRequestHeader("Content-Encoding","gzip");
        http.setRequestHeader("Content-Type","application/json");
        http.setRequestHeader("Content-Encoding","gzip");

        String body = "{\"size\" : 0, \"aggregations\" : { \"uniq_type\" : { \"terms\" : { \"field\" : \"region\", \"size\": 60 } } } }";
        ByteSequence document = new BytesArray(body);

        System.out.println("Print BytesArray : " + document + " : Length : " + document.length()) ;

        //byte []  body = "{\"size\" : 0, \"aggregations\" : { \"uniq_type\" : { \"terms\" : { \"field\" : \"region\", \"size\": 60 } } } }";

        EntityEnclosingMethod entityMethod = (EntityEnclosingMethod) http;
        entityMethod.setRequestEntity(new ByteArrayRequestEntity(compressDataByteArray(document)));
        entityMethod.setContentChunked(false);

        //body.getBytes();

        client.executeMethod(http) ;
        System.out.println("Result Methods : ");
        printResponse(http);

        System.out.println("Here .. ");
    }
}
