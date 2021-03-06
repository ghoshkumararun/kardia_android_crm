package org.lightsys.crmapp.fragments;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.support.v7.widget.AppCompatButton;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import org.json.JSONException;
import org.json.JSONObject;
import org.lightsys.crmapp.R;
import org.lightsys.crmapp.activities.EditProfileActivity;
import org.lightsys.crmapp.activities.ProfileActivity;
import org.lightsys.crmapp.data.KardiaFetcher;
import org.lightsys.crmapp.data.PatchJson;
import org.lightsys.crmapp.data.PostJson;
import org.lightsys.crmapp.data.PostProfilePicture;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;


/**
 * Created by cubemaster on 3/11/16.
 * Edited by Ca2br and Judah on 7/19/16
 *
 * Allows a user to edit/create a profile.
 *
 */


//TODO: Go through all the create methods and delete hardcoded stuff
public class ProfileInputFragment extends Fragment implements AdapterView.OnItemSelectedListener {
    private static final String LOG_TAG = ProfileInputFragment.class.getName();
    private static final String DATA_ARRAY_KEY = "DATA_ARRAY_KEY";
    private static final int[] VIEW_IDS = new int[]{
            R.id.profile_input_name_first,
            R.id.profile_input_name_last,
            R.id.profile_input_country_code_text,
            R.id.profile_input_email,
            R.id.profile_input_street_address,
            R.id.profile_input_city,
            R.id.profile_input_state,
            R.id.profile_input_zip};

    // Values for authenticating the network.
    private AccountManager mAccountManager;
    private Account mAccount;

    // Values that store profile information.
    private String mName;
    private String mSurname;
    private String mGivenName;
    private String mPartnerId;
    private String mPhone;
    private String mCell;
    private String mCountryCode;
    private String mAreaCode;
    private String mPhoneNumber;
    private String mEmail;
    private String mAddress;
    private String mCity;
    private String mState;
    private String mPostalCode;

    TextView firstName;
    TextView lastName;
    TextView phone; //displays the main part of the phone number
    TextView countryCode; //displays country code
    TextView areaCode; //displays area code. phone is split like this to match kardia tables.
    TextView email;
    TextView address;
    TextView city;
    TextView state;
    TextView postalCode;
    ImageView photo;

    private String selectedPhone = "mobile";

    //These store the url id for phone, email, address, and partner.
    //They are used for posting specific json objects.
    private String mPhoneJsonId;
    private String mCellJsonId;
    private String mEmailJsonId;
    private String mAddressJsonId;
    private String mPartnerJsonId;
    private String mTypeJsonId;

    Spinner phoneType; // Switches between home and cell.
    Spinner collabType; //Chooses user role.
    private int collabTypeNumber; //Used to turn role into a number.
    boolean initializedView = false;

    private String[] mEditTextData;

    private String nextPartnerKey;
    boolean mNewProfile = true;
    JSONObject jsonDate;

    Uri outputFileUri;
    Uri selectedImageUri;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_profile_input, container, false);

        // Gets user account.
        mAccountManager = AccountManager.get(getContext());
        final Account[] accounts = mAccountManager.getAccounts();
        if(accounts.length > 0) {
            mAccount = accounts[0];
        }

        // Gets profile information from previous activity.
        Bundle arguments = getArguments();
        if (arguments != null) {
            mName = arguments.getString(ProfileActivity.NAME_KEY);
            mSurname = arguments.getString(EditProfileActivity.SURNAME_KEY);
            mGivenName = arguments.getString(EditProfileActivity.GIVEN_NAMES_KEY);
            mPartnerId = arguments.getString(ProfileActivity.PARTNER_ID_KEY);
            mPhone = arguments.getString(EditProfileActivity.PHONE_KEY);
            mCell = arguments.getString(EditProfileActivity.CELL_KEY);
            mEmail = arguments.getString(EditProfileActivity.EMAIL_KEY);
            mAddress = arguments.getString(EditProfileActivity.ADDRESS_KEY);
            mCity = arguments.getString(EditProfileActivity.CITY_KEY);
            mState = arguments.getString(EditProfileActivity.STATE_KEY);
            mPostalCode = arguments.getString(EditProfileActivity.POSTALCODE_KEY);
            mPhoneJsonId = arguments.getString(EditProfileActivity.PHONE_JSON_ID_KEY);
            mCellJsonId = arguments.getString(EditProfileActivity.CELL_JSON_ID_KEY);
            mEmailJsonId = arguments.getString(EditProfileActivity.EMAIL_JSON_ID_KEY);
            mAddressJsonId = arguments.getString(EditProfileActivity.ADDRESS_JSON_ID_KEY);
            mPartnerJsonId = arguments.getString(EditProfileActivity.PARTNER_JSON_ID_KEY);
            mTypeJsonId = arguments.getString(EditProfileActivity.TYPE_JSON_ID_KEY);
            mNewProfile = false;
        }
        else {
            mNewProfile = true;
        }

        if(savedInstanceState != null) {
            mEditTextData = savedInstanceState.getStringArray(DATA_ARRAY_KEY);
        }
        else {
            mEditTextData = new String[VIEW_IDS.length];
        }

        System.out.println(mNewProfile ? "Creating a new profile!" : "Editing an existing profile!");

        photo = (ImageView) rootView.findViewById(R.id.profile_input_photo);

        //sets up profile picture
        Picasso.with(getActivity())
                .load(R.drawable.persona)
                .into(photo);

        // Sets up views.
        final ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getActivity(),
                R.array.name_spinner, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item);

        firstName = (TextView)rootView.findViewById(R.id.profile_input_name_first);
        lastName = (TextView)rootView.findViewById(R.id.profile_input_name_last);
        phone = (TextView)rootView.findViewById(R.id.profile_input_phone_text);
        countryCode = (TextView) rootView.findViewById(R.id.profile_input_country_code_text);
        areaCode = (TextView)rootView.findViewById(R.id.profile_input_area_code_text);
        email = (TextView)rootView.findViewById(R.id.profile_input_email);
        address = (TextView)rootView.findViewById(R.id.profile_input_street_address);
        city = (TextView)rootView.findViewById(R.id.profile_input_city);
        state = (TextView)rootView.findViewById(R.id.profile_input_state);
        postalCode = (TextView)rootView.findViewById(R.id.profile_input_zip);
        phoneType = (Spinner)rootView.findViewById(R.id.profile_input_phone_spinner);
        collabType = (Spinner)rootView.findViewById(R.id.profile_input_role);

        //Used to switch between home and mobile.
        phoneType.setOnItemSelectedListener(this);

        //Set values to views
        firstName.setText(mGivenName);
        lastName.setText(mSurname);
        email.setText(mEmail);
        address.setText(mAddress);
        city.setText(mCity);
        state.setText(mState);
        postalCode.setText(mPostalCode);

        AppCompatButton photoButton = (AppCompatButton) rootView.findViewById(R.id.profile_input_photo_button);
        photoButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                // Determine Uri of camera image to save.
                final File root = new File(Environment.getExternalStorageDirectory() + File.separator + "MyDir" + File.separator);
                root.mkdirs();
                final String fname = "img_" + System.currentTimeMillis() + ".jpg";
                final File sdImageMainDirectory = new File(root, fname);
                outputFileUri = Uri.fromFile(sdImageMainDirectory);

                // Camera.
                final List<Intent> cameraIntents = new ArrayList<>();
                final Intent captureIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                final PackageManager packageManager = getContext().getPackageManager();
                final List<ResolveInfo> listCam = packageManager.queryIntentActivities(captureIntent, 0);
                for (ResolveInfo res : listCam)
                {
                    final String packageName = res.activityInfo.packageName;
                    final Intent intent = new Intent(captureIntent);
                    intent.setComponent(new ComponentName(packageName, res.activityInfo.name));
                    intent.setPackage(packageName);
                    intent.putExtra(MediaStore.EXTRA_OUTPUT, outputFileUri);
                    cameraIntents.add(intent);
                }

                // Filesystem.
                final Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                galleryIntent.setType("image/*");
                galleryIntent.setAction(Intent.ACTION_GET_CONTENT);

                // Chooser of filesystem options.
                final Intent chooserIntent = Intent.createChooser(galleryIntent, "Select Source");

                // Add the camera options.
                chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, cameraIntents.toArray(new Parcelable[cameraIntents.size()]));

                startActivityForResult(chooserIntent, 0);
            }
        });

        Button submit = (Button) rootView.findViewById(R.id.submit);
        submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    setCurrentDate();

                    AsyncTask<String, Void, String> uploadJson1;
                    AsyncTask<String, Void, String> uploadJson2;
                    AsyncTask<String, Void, String> uploadJson3;
                    AsyncTask<String, Void, String> uploadJson4;
                    AsyncTask<String, Void, String> uploadJson5;
                    AsyncTask<String, Void, String> uploadJson6;
                    PostProfilePicture postProfilePicture;

                    if (mNewProfile)
                    {
                        nextPartnerKey = new GetPartnerKey().execute().get();
                        System.out.println("Retrieved New Partner Key: " + nextPartnerKey);

                        //urls for Posting to kardia
                        String partnerUrl = mAccountManager.getUserData(mAccount, "server") + "/apps/kardia/api/partner/Partners?cx__mode=rest&cx__res_format=attrs&cx__res_attrs=basic&cx__res_type=collection";
                        String addressUrl = mAccountManager.getUserData(mAccount, "server") + "/apps/kardia/api/partner/Partners/" + nextPartnerKey + "/Addresses?cx__mode=rest&cx__res_type=collection&cx__res_format=attrs&cx__res_attrs=basic";
                        String phoneUrl = selectedPhone.equals("home")
                                ? mAccountManager.getUserData(mAccount, "server") + "/apps/kardia/api/partner/Partners/" + nextPartnerKey + "/ContactInfo?cx__mode=rest&cx__res_type=collection&cx__res_format=attrs&cx__res_attrs=basic"
                                : null;
                        String cellUrl = selectedPhone.equals("mobile")
                                ? mAccountManager.getUserData(mAccount, "server") + "/apps/kardia/api/partner/Partners/" + nextPartnerKey + "/ContactInfo?cx__mode=rest&cx__res_format=attrs&cx__res_attrs=basic&cx__res_type=collection"
                                : null;
                        
                        String emailUrl = mAccountManager.getUserData(mAccount, "server") + "/apps/kardia/api/partner/Partners/" + nextPartnerKey + "/ContactInfo?cx__mode=rest&cx__res_type=collection&cx__res_format=attrs&cx__res_attrs=basic";
                        String photoUrl = mAccountManager.getUserData(mAccount, "server") + "/apps/kardia/files?";
                        String typeUrl = mAccountManager.getUserData(mAccount, "server") + "/apps/kardia/api/crm/Partners/" + mAccountManager.getUserData(mAccount, "partnerId") + "/Collaboratees?cx__mode=rest&cx__res_type=collection&cx__res_format=attrs&cx__res_attrs=basic";

                        //set up POST json objects for patching
                        uploadJson1 = new PostJson(getContext(), partnerUrl, createPartnerJson(), mAccount, false);
                        uploadJson2 = new PostJson(getContext(), addressUrl, createAddressJson(), mAccount, false);
                        uploadJson3 = new PostJson(getContext(), phoneUrl, createPhoneJson(), mAccount, false);
                        uploadJson4 = new PostJson(getContext(), cellUrl, createCellJson(), mAccount, false);
                        String realPathFromURI = getRealPathFromURI(selectedImageUri, getContext());
                        postProfilePicture = new PostProfilePicture(getContext(), photoUrl, new File(realPathFromURI), mAccount, nextPartnerKey);
                        uploadJson5 = new PostJson(getContext(), emailUrl, createEmailJson(), mAccount, false);
                        uploadJson6 = new PostJson(getContext(), typeUrl, createTypeJson(), mAccount, true);
                    }
                    else
                    {
                        //urls for patching to kardia
                        String partnerUrl = mAccountManager.getUserData(mAccount, "server") + mPartnerJsonId + "&cx__res_type=element";
                        String addressUrl = mAccountManager.getUserData(mAccount, "server") + mAddressJsonId + "&cx__res_type=element";
                        String phoneUrl = mAccountManager.getUserData(mAccount, "server") + mPhoneJsonId + "&cx__res_type=element";
                        String cellUrl = mAccountManager.getUserData(mAccount, "server") + mCellJsonId + "&cx__res_type=element";
                        String emailUrl = mAccountManager.getUserData(mAccount, "server") + mEmailJsonId + "&cx__res_type=element";
                        String typeUrl = mAccountManager.getUserData(mAccount, "server") + mTypeJsonId + "&cx__res_type=element";

                        //set up patch json objects for patching
                        uploadJson1 = new PatchJson(getContext(), partnerUrl, createPartnerJson(), mAccount);
                        uploadJson2 = new PatchJson(getContext(), addressUrl, createAddressJson(), mAccount);
                        uploadJson3 = new PatchJson(getContext(), phoneUrl, createPhoneJson(), mAccount);
                        uploadJson4 = new PatchJson(getContext(), cellUrl, createCellJson(), mAccount);
                        uploadJson5 = new PatchJson(getContext(), emailUrl, createEmailJson(), mAccount);
                        uploadJson6 = new PatchJson(getContext(), typeUrl, createTypeJson(), mAccount);
                        postProfilePicture = null;
                    }

                    uploadJson1.execute();
                    uploadJson2.execute();
                    if(selectedPhone.equals("home")) {//if home phone is selected, patch home
                        System.out.println("POST Phone info");
                        uploadJson3.execute();
                    }
                    else if(selectedPhone.equals("mobile")) {//if mobile phone is selected, patch mobile
                        System.out.println("POST Mobile info");
                        uploadJson4.execute();
                    }
                    if (postProfilePicture != null)
                        System.out.println("Posting Profile Picture");
                        postProfilePicture.execute();
                    System.out.println("POST Email info");
                    uploadJson5.execute();
                    uploadJson6.execute();
                }
                catch(Exception e) {
                    e.printStackTrace();
                }
            }
        });

        return rootView;
    }

    private void setCurrentDate()
    {
        //Get current date
        java.util.Date date = new Date();
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);

        try
        {
            //Create Date Json object of current datetime
            jsonDate = new JSONObject();
            jsonDate.put("month", cal.get(Calendar.MONTH));
            jsonDate.put("year", cal.get(Calendar.YEAR));
            jsonDate.put("day", cal.get(Calendar.DAY_OF_MONTH));
            jsonDate.put("minute", cal.get(Calendar.MINUTE));
            jsonDate.put("second", cal.get(Calendar.SECOND));
            jsonDate.put("hour", cal.get(Calendar.HOUR));
        }
        catch (JSONException ex)
        {
            ex.printStackTrace();
        }
    }

    private JSONObject createEmailJson()
    {
        JSONObject emailJson = new JSONObject();

        try {
            if (mNewProfile)
            {
                emailJson.put("p_partner_key", nextPartnerKey);
                emailJson.put("s_created_by", mAccount.name);
                emailJson.put("s_modified_by", mAccount.name);
                emailJson.put("s_date_created", jsonDate);
                emailJson.put("s_date_modified", jsonDate);
                emailJson.put("p_contact_data", email.getText().toString());
                emailJson.put("p_contact_type", "E");
                emailJson.put("p_record_status_code", "A");
            }
            else
            {
                emailJson.put("contact_data", email.getText().toString());
            }
        }
        catch (JSONException ex) {
            ex.printStackTrace();
        }

        return emailJson;
    }

    private JSONObject createCellJson()
    {
        JSONObject cellJson = new JSONObject();

        try {
            String countryCodeTemp = countryCode.getText().toString();
            String actualCountryCode = countryCodeTemp.equals("") ? "1" : countryCodeTemp.replace("+", "");

            if (mNewProfile)
            {
                cellJson.put("p_partner_key", nextPartnerKey);
                cellJson.put("s_created_by", mAccount.name);
                cellJson.put("s_modified_by", mAccount.name);
                cellJson.put("s_date_created", jsonDate);
                cellJson.put("s_date_modified", jsonDate);
                cellJson.put("p_contact_data", phone.getText().toString());
                cellJson.put("p_phone_country", actualCountryCode);
                cellJson.put("p_phone_area_city", areaCode.getText().toString());
                cellJson.put("p_contact_type", "C");
                cellJson.put("p_record_status_code", "A");
            }
            else
            {
                cellJson.put("contact_data", phone.getText().toString());
                cellJson.put("phone_country", actualCountryCode);
                cellJson.put("phone_area_city", areaCode.getText().toString());
            }
        }
        catch (JSONException ex) {
            ex.printStackTrace();
        }

        return cellJson;
    }

    private JSONObject createPhoneJson()
    {
        JSONObject phoneJson = new JSONObject();

        try {

            String countryCodeTemp = countryCode.getText().toString();
            String actualCountryCode = countryCodeTemp.equals("") ? "1" : countryCodeTemp.replace("+", "");

            if(mNewProfile)
            {
                phoneJson.put("p_partner_key", nextPartnerKey);
                phoneJson.put("s_created_by", mAccount.name);
                phoneJson.put("s_modified_by", mAccount.name);
                phoneJson.put("s_date_created", jsonDate);
                phoneJson.put("s_date_modified", jsonDate);
                phoneJson.put("p_contact_data", phone.getText().toString());
                phoneJson.put("p_phone_country", actualCountryCode);
                phoneJson.put("p_phone_area_city", areaCode.getText().toString());
                phoneJson.put("p_contact_type", "P");
                phoneJson.put("p_record_status_code", "A");
            }
            else
            {
                phoneJson.put("contact_data", phone.getText().toString());
                phoneJson.put("phone_country", actualCountryCode);
                phoneJson.put("phone_area_city", areaCode.getText().toString());
            }

        }
        catch (JSONException ex) {
            ex.printStackTrace();
        }

        return phoneJson;
    }

    private JSONObject createAddressJson()
    {
        JSONObject addressJson = new JSONObject();

        try {
            if (mNewProfile)
            {
                addressJson.put("p_partner_key", nextPartnerKey);
                addressJson.put("s_created_by", mAccount.name);
                addressJson.put("s_modified_by", mAccount.name);
                addressJson.put("s_date_created", jsonDate);
                addressJson.put("s_date_modified", jsonDate);
                addressJson.put("p_location_id", 0);
                addressJson.put("p_revision_id", 0);
                addressJson.put("p_record_status_code", "A");
                addressJson.put("p_address_1", address.getText().toString());
                addressJson.put("p_city", city.getText().toString());
                addressJson.put("p_state_province", state.getText().toString());
                addressJson.put("p_postal_code", postalCode.getText());
            }
            else
            {
                addressJson.put("location_id", 1);
                addressJson.put("address_1", address.getText().toString());
                addressJson.put("city", city.getText().toString());
                addressJson.put("state_province", state.getText().toString());
                addressJson.put("postal_code", postalCode.getText());
            }
        }
        catch (JSONException ex) {
            ex.printStackTrace();
        }

        return addressJson;
    }

    private JSONObject createPhotoJson()
    {
        JSONObject photoJson = new JSONObject();

        try
        {
            if (mNewProfile)
            {
                photoJson.put("name", "ProfilePicture");
            } else
            {

            }
        }catch (JSONException ex)
        {
            ex.printStackTrace();
        }

        return photoJson;

    }

    private JSONObject createPartnerJson()
    {
        JSONObject partnerJson = new JSONObject();

        try
        {
            if (mNewProfile)
            {
                partnerJson.put("p_partner_key", nextPartnerKey);
                partnerJson.put("s_created_by", mAccount.name);
                partnerJson.put("s_modified_by", mAccount.name);
                partnerJson.put("p_creating_office", mAccountManager.getUserData(mAccount, "partnerId"));
                partnerJson.put("p_status_code", "A");
                partnerJson.put("p_partner_class", "123");
                partnerJson.put("p_record_status_code", "A");
                partnerJson.put("p_surname", lastName.getText().toString());
                partnerJson.put("p_given_name", firstName.getText().toString());
                partnerJson.put("s_date_created", jsonDate);
                partnerJson.put("s_date_modified", jsonDate);

            } else
            {
                partnerJson.put("surname", lastName.getText().toString());
                partnerJson.put("given_names", firstName.getText().toString());
            }
        }
        catch (JSONException ex)
        {
            ex.printStackTrace();
        }

        return partnerJson;
    }

    private JSONObject createTypeJson()
    {

        //Convert collaborator type to a number
        if(collabType.getSelectedItem().equals("None")){
            collabTypeNumber = 0;
        }else if(collabType.getSelectedItem().equals("Mobilizer")){
            collabTypeNumber = 1;
        }else if(collabType.getSelectedItem().equals("NonMobilizer")){
            collabTypeNumber = 2;
        }

        JSONObject typeJson = new JSONObject();

        try
        {
            if (mNewProfile)
            {
                typeJson.put("e_collaborator", mAccountManager.getUserData(mAccount, "partnerId"));
                typeJson.put("p_partner_key", nextPartnerKey);
                typeJson.put("e_collab_type_id", collabTypeNumber);
                typeJson.put("e_is_automatic", 0);
                typeJson.put("e_collaborator_status", "A");
                typeJson.put("s_created_by", "dgarcia");
                typeJson.put("s_modified_by", "dgarcia");
                typeJson.put("s_date_created", jsonDate);
                typeJson.put("s_date_modified", jsonDate);
                typeJson.put( "collaborator_id", mAccountManager.getUserData(mAccount, "partnerId"));
                typeJson.put( "collaborator_type_id", collabTypeNumber);
                typeJson.put( "collaborator_status", "A");
                typeJson.put( "partner_id", nextPartnerKey);
                typeJson.put( "role_id", collabTypeNumber);
                typeJson.put( "role_name", collabType.getSelectedItem());



            } else
            {
                typeJson.put("collab_type_id", collabTypeNumber);
            }
        }
        catch (JSONException ex)
        {
            ex.printStackTrace();
        }
        return typeJson;
    }

    /**
     * Saves info.
     */
    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putStringArray(DATA_ARRAY_KEY, mEditTextData);
    }

    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l)
    {
        if (!initializedView)
        {
            initializedView = true;
            return;
        }

        String typeOfPhone = (String)phoneType.getSelectedItem();
        String[] phoneBits = null;//split up phone into its parts

        selectedPhone = typeOfPhone.toLowerCase();

        if (typeOfPhone.equals("Home"))
        {
            if (mPhone != null)
                phoneBits = mPhone.split(" ");
        }
        else if (typeOfPhone.equals("Mobile"))
        {
            if (mCell != null)
                phoneBits = mCell.split(" ");//split phone into its parts
        }

        if (phoneBits == null || phoneBits.length != 3)
            return;

        mCountryCode = phoneBits[0].replaceAll("[^0-9.]", "");//get country code and remove non numbers
        mAreaCode = phoneBits[1].replaceAll("[^0-9.]", "");//get area code and remove non numbers
        mPhoneNumber = phoneBits[2].replaceAll("[^0-9.]", "");//get phone number and remove non numbers

        //set phone number values to views
        phone.setText(mPhoneNumber);
        countryCode.setText(mCountryCode);
        areaCode.setText(mAreaCode);
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView)
    {

    }

    private class GetPartnerKey extends AsyncTask<Void, Void, String>
    {
        @Override
        protected String doInBackground(Void... voids)
        {
            KardiaFetcher fetcher = new KardiaFetcher(getContext());

            return fetcher.getNextPartnerKey(mAccount);
        }

        @Override
        protected void onPostExecute(String s)
        {
            super.onPostExecute(s);
            nextPartnerKey = s;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == -1)
        {
            if (requestCode == 0)
            {
                final boolean isCamera;
                if (data == null)
                {
                    isCamera = true;
                } else
                {
                    final String action = data.getAction();
                    isCamera = action != null && action.equals(MediaStore.ACTION_IMAGE_CAPTURE);
                }

                if (isCamera)
                {
                    selectedImageUri = outputFileUri;
                } else
                {
                    selectedImageUri = data.getData();
                }

                photo.setImageURI(selectedImageUri);
                Log.d("NewProfileActivity", "isCamera: " + isCamera + " selectedImageUri: " + selectedImageUri);
            }
        }
    }

    public String getRealPathFromURI(Uri contentUri, Context context)
    {
        String[] proj = { MediaStore.Images.Media.DATA };
        Cursor cursor = context.getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, proj, null, null, null); //Since manageQuery is deprecated
        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
        return cursor.getString(column_index);
    }
}
