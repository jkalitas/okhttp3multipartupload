package com.codavel.test.okhttp3multipartupload;

import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import java.io.IOException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import okhttp3.CacheControl;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //running the async task
        new UploadTask().execute();
    }

    /**
     * Performing the upload on asynchronous task.
     */
    private class UploadTask extends AsyncTask<Void, Void, Void> {
        /**
         * Scheme used by requests
         */
        private static final String SCHEME = "https://";
        /**
         * URL for upload
         */
        private static final String URL_PATH = "speedtest1.meo.pt:8080/speedtest/upload.php";
        /**
         * The file size that will control the amount of random bytes to be sent.
         */
        private static final long fileSize = 505544;

        /**
         * In background the task will send an upload using OkHttp3 to a public server, expecting to
         * see a response body with the information of bytes sent (e.g. "size=506322")
         *
         * @param voids
         * @return
         */
        @Override
        protected Void doInBackground(Void... voids) {

            Log.e("UPLOAD_TASK", "Initiating on background");

            final MediaType MEDIA_TYPE_PNG = MediaType.parse("image/png");
            MultipartBody.Builder multipartBody = new MultipartBody.Builder().setType(MultipartBody.
                    FORM);

            byte[] data = new byte[64 * 1024];
            long bytestBody = 0;

            while (bytestBody < fileSize) {
                if (bytestBody + data.length > fileSize) {
                    long difference = fileSize - bytestBody;
                    data = new byte[(int) difference];
                    multipartBody.addPart(RequestBody.create(MEDIA_TYPE_PNG, data));
                    bytestBody += data.length;
                } else {
                    multipartBody.addPart(RequestBody.create(MEDIA_TYPE_PNG, data));
                    bytestBody += data.length;
                }
            }
            RequestBody requestBody = multipartBody.build();

            Request request = new Request.Builder()
                    .url(SCHEME + URL_PATH)
                    .cacheControl(new CacheControl.Builder().noCache().build())
                    .post(requestBody)
                    .build();

            OkHttpClient client = getUnsafeOkHttpClient();
            Response response = null;
            try {
                Log.e("UPLOAD_TASK", "Executing...");
                response = client.newCall(request).execute();

                if (!response.isSuccessful()) {
                    response.close();
                    throw new IOException("Unexpected code " + response);
                } else {
                    Log.e("UPLOAD_TASK", "doInBackground: RESPONSE \n" +
                            response.body().string());
                }

                Log.e("UPLOAD_TASK", "bytesBody=" + bytestBody);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (response != null) {
                    response.close();
                }
            }
            return null;
        }

        /**
         * In the example I use a public server which presents the following SSL error:
         * "NET::ERR_CERT_COMMON_NAME_INVALID". To overcome the issue we create an unsafe
         * OkHttpClient instance that basically approves all the domains name when it's call to
         * verify.
         *
         * @return an unsafe OkHttpClient instance
         */
        private OkHttpClient getUnsafeOkHttpClient() {
            try {
                // Create a trust manager that does not validate certificate chains
                final TrustManager[] trustAllCerts = new TrustManager[]{
                        new X509TrustManager() {
                            @Override
                            public void checkClientTrusted(java.security.cert.X509Certificate[]
                                                                   chain, String authType)
                                    throws CertificateException {
                            }

                            @Override
                            public void checkServerTrusted(java.security.cert.X509Certificate[]
                                                                   chain, String authType)
                                    throws CertificateException {
                            }

                            @Override
                            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                                return new X509Certificate[0];
                            }
                        }
                };

                // Install the all-trusting trust manager
                final SSLContext sslContext = SSLContext.getInstance("SSL");
                sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
                // Create an ssl socket factory with our all-trusting manager
                final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

                return new OkHttpClient.Builder()
                        .sslSocketFactory(sslSocketFactory, (X509TrustManager) trustAllCerts[0])
                        .hostnameVerifier(new HostnameVerifier() {
                            @Override
                            public boolean verify(String hostname, SSLSession session) {
                                return true;
                            }
                        }).build();

            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}
