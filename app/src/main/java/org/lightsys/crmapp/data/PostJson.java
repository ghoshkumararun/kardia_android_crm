package org.lightsys.crmapp.data;

import android.accounts.AccountManager;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONObject;
import org.lightsys.crmapp.activities.LoginActivity;
import org.lightsys.crmapp.activities.MainActivity;

import android.accounts.Account;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.HttpURLConnection;
import java.net.PasswordAuthentication;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

/**
 * Created by Judah Sistrunk on 7/7/2016.
 *
 * This class takes a json object, a baseUrl, and an account, and posts the json object to the server
 *
 * Edited by Tim Parr on 6/22/2017
 */
public class PostJson extends AsyncTask<String, Void, String> {

    private static final String TAG = "POST Json";
    private Account account;
    private AccountManager mAccountManager;
    private String baseUrl = "";
    private String backupUrl = "";
    private JSONObject jsonObject;
    private Context context;
    private boolean success = false;
    private static CookieManager cookieManager = new CookieManager();
    private boolean finalTask;

    public PostJson(Context context, String baseUrl, JSONObject jsonPost, Account userAccount, boolean finalAsyncTask){
        this.baseUrl = baseUrl;
        backupUrl = baseUrl;
        jsonObject = jsonPost;
        account = userAccount;
        this.context = context;
        mAccountManager = AccountManager.get(context);
        CookieHandler.setDefault(cookieManager);
        finalTask = finalAsyncTask;
    }

    @Override
    protected String doInBackground(String... params) {

        java.net.Authenticator.setDefault(new java.net.Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(account.name, mAccountManager.getPassword(account).toCharArray());
            }
        });

        InputStream inputStream;
        String result;
        try {

            //baseUrl used to retrieve the access token
            URL getUrl = new URL(mAccountManager.getUserData(account, "server") + "/?cx__mode=appinit&cx__groupname=Kardia&cx__appname=Donor");

            HttpURLConnection connection;
            connection = (HttpURLConnection) getUrl.openConnection();
            connection.setReadTimeout(10000);
            connection.setConnectTimeout(10000);

            inputStream = connection.getInputStream();

            //get access token
            if (inputStream != null) {
                result = convertInputStreamToString(inputStream);
                JSONObject token = new JSONObject(result);

                baseUrl += "&cx__akey=" + token.getString("akey");

                //post json object
                performPostCall(baseUrl, jsonObject);
            }
        } catch (Exception e) {
            e.printStackTrace();}

        return null;
    }

    private String convertInputStreamToString(InputStream in) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        String line, result = "";

        while ((line = reader.readLine()) != null) {
            result += line;
        }
        in.close();
        return result;
    }

    /*
        function that posts a json object to the server
    */
    private String performPostCall(String requestURL, JSONObject jsonObject) {

        URL url;
        String response = "";
        try {
            url = new URL(requestURL);

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setReadTimeout(15000);//15 second time out
            conn.setConnectTimeout(15000);
            conn.setRequestMethod("POST");
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json");

            //get json object ready to send
            String str = jsonObject.toString();
            byte[] outputBytes = str.getBytes("UTF-8");
            OutputStream os = conn.getOutputStream();
            os.write(outputBytes);//send json object

            int responseCode = conn.getResponseCode();

            Log.e(TAG, "responseCode : " + responseCode);

            //if the things were sent properly, get the response code
            if (responseCode == HttpsURLConnection.HTTP_CREATED) {
                Log.e(TAG, "HTTP_OK");
                response = convertInputStreamToString(conn.getInputStream());
                success = true;
            } else
            {
                Log.e(TAG, "False - HTTP_OK");//send failed :(
                String s = convertInputStreamToString(conn.getErrorStream());
                response = "";
                success = false;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return response;
    }

    @Override
    protected void onPostExecute(String params) {

        if (success) {
            if (finalTask)
            {
                Toast.makeText(context, "Data posted successfully!", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(context, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
            }
        }
        else {
            Toast.makeText(context, "Network Issues: Your data is waiting to be sent", Toast.LENGTH_SHORT).show();
        }
    }
}
