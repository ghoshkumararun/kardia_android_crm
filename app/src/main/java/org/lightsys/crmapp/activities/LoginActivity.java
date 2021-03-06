package org.lightsys.crmapp.activities;

import android.accounts.Account;
import android.accounts.AccountAuthenticatorActivity;
import android.accounts.AccountManager;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatCallback;
import android.support.v7.app.AppCompatDelegate;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.lightsys.crmapp.R;
import org.lightsys.crmapp.data.CRMContract;
import org.lightsys.crmapp.data.KardiaFetcher;
import org.lightsys.crmapp.models.Partner;
import org.lightsys.crmapp.models.Staff;

import java.io.IOException;
import java.util.List;


public class LoginActivity extends AccountAuthenticatorActivity implements AppCompatCallback {

    private AccountManager mAccountManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("Login Activity", "Created");

        mAccountManager = AccountManager.get(this);

        AppCompatDelegate delegate = AppCompatDelegate.create(this, this);

        delegate.onCreate(savedInstanceState);
        delegate.setContentView(R.layout.activity_login);

        Toolbar toolbar = (Toolbar) findViewById(R.id.loginToolbar);
        delegate.setSupportActionBar(toolbar);

        Button button = (Button) findViewById(R.id.loginSubmit);
        button.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                addAccount(v);
            }
        });

        EditText serverAddress = (EditText) findViewById(R.id.loginServer);
        serverAddress.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_GO) {
                    addAccount(v);
                    return true;
                }

                return false;
            }
        });
    }

    public void addAccount(View v) {
        EditText accountName = (EditText) findViewById(R.id.loginUsername);
        EditText accountPassword = (EditText) findViewById(R.id.loginPassword);
        EditText serverAddress = (EditText) findViewById(R.id.loginServer);
        EditText portNumber = (EditText) findViewById(R.id.loginPort);
        Spinner protocol = (Spinner) findViewById(R.id.protocolSpinner);

        String addAccountName = accountName.getText().toString();
        String addAccountPassword = accountPassword.getText().toString();
        String addServerAddress = serverAddress.getText().toString();
        String addPortNumber = portNumber.getText().toString();
        String addProtocol;

        //Set protocol with spinner
        //Other options can easily be added by adding items in strings.xml
        switch (protocol.getSelectedItemPosition()){
            case 0:
                addProtocol = "http";
                break;
            case 1:
                addProtocol = "https";
                break;
            default:
                addProtocol = "http";
                break;
        }



        //If any field is left blank, display error message
        if (addAccountName.equals("")) {
            ((TextView) findViewById(R.id.loginUsernameError)).setText("Enter a username.");
            return;
        }
        else {
            ((TextView) findViewById(R.id.loginUsernameError)).setText("");
        }
        if (addAccountPassword.equals("")) {
            ((TextView) findViewById(R.id.loginPasswordError)).setText("Enter a password.");
            return;
        } else {
            ((TextView) findViewById(R.id.loginPasswordError)).setText("");
        }
        if (addServerAddress.equals("")) {
            ((TextView) findViewById(R.id.loginServerError)).setText("Enter a server address.");
            return;
        }
        else {
            ((TextView) findViewById(R.id.loginServerError)).setText("");
        }
        if (addPortNumber.equals("")) {
            ((TextView) findViewById(R.id.loginPortError)).setText("Enter a port number.");
            return;
        }
        else {
            ((TextView) findViewById(R.id.loginPortError)).setText("");
        }


        //Concatenate server info into full address
        String addFullServerAddress = addProtocol + "://" + addServerAddress + ":" + addPortNumber;


        Account newAccount = new Account(addAccountName, CRMContract.accountType);
        Bundle bundle = new Bundle();
        bundle.putString(AccountManager.KEY_ACCOUNT_NAME, addAccountName);
        bundle.putString(AccountManager.KEY_ACCOUNT_TYPE, CRMContract.accountType);
        boolean success = mAccountManager.addAccountExplicitly(newAccount, addAccountPassword, bundle);
        if (success)
        {
            mAccountManager.setUserData(newAccount, "server", addFullServerAddress);
            new GetPartnerIdTask().execute(newAccount);
        } else {
            Log.d("LoginActivity", "Adding Acount Failed");
        }

    }

    private void checkAccount(Account account) {
        if (mAccountManager.getUserData(account, "partnerId") != null) {
            ContentResolver.setSyncAutomatically(account, CRMContract.providerAuthority, true);

            Bundle bundle = new Bundle();
            bundle.putString(AccountManager.KEY_ACCOUNT_NAME, account.name);
            bundle.putString(AccountManager.KEY_ACCOUNT_TYPE, CRMContract.accountType);
            setAccountAuthenticatorResult(bundle);

            new GetCollaborateesTask().execute(account);
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1)
            {
                mAccountManager.removeAccount(account, this, null, null);
            }
            else
            {
                mAccountManager.removeAccount(account, null, null);
            }
            View loginLayout = findViewById(R.id.loginLayout);
            Snackbar.make(loginLayout, "Connection to server failed", Snackbar.LENGTH_LONG).show();
        }
    }

    private void mainActivity() {
        Intent main = new Intent(LoginActivity.this, MainActivity.class);
        startActivity(main);
        finish();
    }

    @Override
    public void onSupportActionModeStarted(ActionMode mode) {

    }

    @Override
    public void onSupportActionModeFinished(ActionMode mode) {

    }

    @Nullable
    @Override
    public ActionMode onWindowStartingSupportActionMode(ActionMode.Callback callback) {
        return null;
    }

    private class GetPartnerIdTask extends AsyncTask<Account, Void, Account> {
        @Override
        protected Account doInBackground(Account... accounts) {
            KardiaFetcher fetcher = new KardiaFetcher(LoginActivity.this);
            List<Staff> staff = fetcher.getStaff(accounts[0]);
            for(Staff staffMember : staff) {
                ContentValues values = new ContentValues();
                values.put(CRMContract.StaffTable.PARTNER_ID, staffMember.getPartnerId());
                values.put(CRMContract.StaffTable.KARDIA_LOGIN, staffMember.getKardiaLogin());
                getContentResolver().insert(CRMContract.StaffTable.CONTENT_URI, values);
            }
            Cursor cursor = getContentResolver().query(
                    CRMContract.StaffTable.CONTENT_URI,
                    new String[] { CRMContract.StaffTable.PARTNER_ID },
                    CRMContract.StaffTable.KARDIA_LOGIN + " = ?",
                    new String[] { accounts[0].name },
                    null
            );

            if(cursor.moveToFirst()) {
                mAccountManager.setUserData(accounts[0], "partnerId", cursor.getString(0));
            }
            cursor.close();
            return accounts[0];
        }

        @Override
        protected void onPostExecute(Account account) {
            checkAccount(account);
        }
    }

    private class GetCollaborateesTask extends AsyncTask<Account, Void, Void> {
        Exception error;
        @Override
        protected Void doInBackground(Account... accounts) {
            KardiaFetcher fetcher = new KardiaFetcher(LoginActivity.this);

            try
            {
                List<Partner> collaboratees = fetcher.getCollaboratees(accounts[0]);

                for (Partner collaboratee : collaboratees) {
                    ContentValues values = new ContentValues();
                    values.put(CRMContract.CollaborateeTable.COLLABORATER_ID, mAccountManager.getUserData(accounts[0], "partnerId"));
                    values.put(CRMContract.CollaborateeTable.PARTNER_ID, Integer.parseInt(collaboratee.getPartnerId()));
                    values.put(CRMContract.CollaborateeTable.PARTNER_NAME, collaboratee.getPartnerName());
                    values.put(CRMContract.CollaborateeTable.PROFILE_PICTURE, collaboratee.getProfilePictureFilename());

                    getContentResolver().insert(CRMContract.CollaborateeTable.CONTENT_URI, values);

                    ProfileActivity.saveImageFromUrl(
                            mAccountManager.getUserData(accounts[0], "server"),
                            getApplicationContext(),
                            collaboratee.getProfilePictureFilename(),
                            collaboratee.getPartnerId()
                    );
                }
            } catch (IOException e)
            {
                e.printStackTrace();
                error = e;
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void nothing) {
            if (error == null)
                mainActivity();
            else
            {
                Toast.makeText(getApplicationContext(), "Network Issues: Server rejected request.", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
